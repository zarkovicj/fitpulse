import { ChangeDetectionStrategy, Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { FormArray, FormControl, FormGroup, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';

import { ExerciseInfo } from '../../core/exercise/exercise-info';
import { errorMessage } from '../../core/api/api-error';
import { AuthService } from '../../core/auth/auth';
import { Ownership } from '../../core/auth/ownership';
import { Exercise, MuscleGroup, muscleGroupLabel } from '../../core/exercise/exercise.model';
import { TemplateApi } from '../../core/template/template-api';
import { Template, TemplateRequest } from '../../core/template/template.model';
import { HasUnsavedChanges } from '../../shared/guards/unsaved-changes';
import { Icon } from '../../shared/icon/icon';
import { ExercisePicker } from '../exercises/exercise-picker';

type ItemForm = FormGroup<{
  exerciseId: FormControl<number>;
  exerciseName: FormControl<string>;
  muscleGroup: FormControl<MuscleGroup>;
  setCount: FormControl<number | null>;
  reps: FormControl<number | null>;
  weight: FormControl<number | null>;
}>;

@Component({
  selector: 'app-template-editor',
  imports: [ReactiveFormsModule, RouterLink, Icon, ExercisePicker],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './template-editor.html',
})
export class TemplateEditor implements OnInit, HasUnsavedChanges {
  private readonly api = inject(TemplateApi);
  protected readonly info = inject(ExerciseInfo);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);
  private readonly fb = inject(NonNullableFormBuilder);
  protected readonly ownership = inject(Ownership);

  /** Prazno na /templates/new. */
  readonly id = input<string>();

  protected readonly existing = signal<Template | null>(null);
  protected readonly loading = signal(false);
  protected readonly saving = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly pickerOpen = signal(false);
  private saved = false;

  protected readonly groupLabel = muscleGroupLabel;

  // admin dolazi iz administracije, pa se tamo i vraća
  protected readonly back = computed(() => {
    const existing = this.existing();
    if (existing) return { link: ['/templates', existing.id], params: null, label: existing.name };
    return this.auth.isAdmin()
      ? { link: ['/admin'], params: { tab: 'templates' }, label: 'Administracija' }
      : { link: ['/templates'], params: null, label: 'Šabloni' };
  });

  protected readonly form = this.fb.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: [''],
    exercises: this.fb.array<ItemForm>([], Validators.required),
  });

  private readonly formValue = toSignal(this.form.valueChanges, { initialValue: this.form.getRawValue() });
  protected readonly addedIds = computed(() => (this.formValue().exercises ?? []).map((e) => e.exerciseId!));

  // vežba u šablonu mora biti sistemska ili vlasnika šablona (isto pravilo kao na backend-u)
  private readonly templateOwner = computed(() => {
    const existing = this.existing();
    if (existing) return existing.createdBy;
    // šablon koji pravi admin je gotov (sistemski), pa sme da sadrži samo sistemske vežbe
    return this.auth.isAdmin() ? null : this.auth.userId();
  });
  protected readonly allowedExercise = computed(() => {
    const owner = this.templateOwner();
    return (e: Exercise) => e.system || (owner !== null && e.createdBy === owner);
  });

  get items(): FormArray<ItemForm> {
    return this.form.controls.exercises;
  }

  ngOnInit(): void {
    const id = this.id();
    if (!id) return;

    this.loading.set(true);
    this.api.get(+id).subscribe({
      next: (template) => {
        if (!this.ownership.canModify(template.createdBy)) {
          this.router.navigate(['/templates', template.id]);
          return;
        }
        this.existing.set(template);
        this.form.patchValue({ name: template.name, description: template.description ?? '' });
        template.exercises.forEach((e) =>
          this.items.push(this.item(e.exerciseId, e.exerciseName, e.muscleGroup, e.setCount, e.reps, e.weight)),
        );
        this.form.markAsPristine();
        this.loading.set(false);
      },
      error: (err) => {
        this.loading.set(false);
        this.error.set(errorMessage(err, 'Šablon nije učitan.'));
      },
    });
  }

  protected add(exercise: Exercise): void {
    this.items.push(this.item(exercise.id, exercise.name, exercise.muscleGroup, 3, 10, null));
    this.items.markAsDirty();
  }

  protected remove(index: number): void {
    this.items.removeAt(index);
    this.items.markAsDirty();
  }

  protected move(index: number, offset: -1 | 1): void {
    const target = index + offset;
    if (target < 0 || target >= this.items.length) return;
    const control = this.items.at(index);
    this.items.removeAt(index, { emitEvent: false });
    this.items.insert(target, control);
    this.items.markAsDirty();
  }

  protected invalid(control: FormControl<unknown>): boolean {
    return control.invalid && control.touched;
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.error.set(this.items.length === 0 ? 'Dodaj bar jednu vežbu.' : 'Proveri označena polja.');
      return;
    }

    const value = this.form.getRawValue();
    const request: TemplateRequest = {
      name: value.name.trim(),
      description: value.description.trim() || null,
      exercises: value.exercises.map((e) => ({
        exerciseId: e.exerciseId,
        setCount: e.setCount!,
        reps: e.reps!,
        weight: e.weight || null,
      })),
    };
    const existing = this.existing();

    this.saving.set(true);
    this.error.set(null);
    (existing ? this.api.update(existing.id, request) : this.api.create(request)).subscribe({
      next: (template) => {
        this.saved = true;
        this.router.navigate(['/templates', template.id]);
      },
      error: (err) => {
        this.saving.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  hasUnsavedChanges(): boolean {
    return this.form.dirty && !this.saved;
  }

  private item(
    exerciseId: number,
    exerciseName: string,
    muscleGroup: MuscleGroup,
    setCount: number,
    reps: number,
    weight: number | null,
  ): ItemForm {
    return this.fb.group({
      exerciseId: [exerciseId],
      exerciseName: [exerciseName],
      muscleGroup: [muscleGroup],
      setCount: this.fb.control<number | null>(setCount, [Validators.required, Validators.min(1), Validators.max(20)]),
      reps: this.fb.control<number | null>(reps, [
        Validators.required,
        Validators.min(1),
        Validators.max(100),
      ]),
      weight: this.fb.control<number | null>(weight, [Validators.min(0), Validators.max(1000)]),
    });
  }
}
