import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, computed, inject, input, output, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable, map, of, switchMap } from 'rxjs';

import { errorMessage } from '../../core/api/api-error';
import { ExerciseApi } from '../../core/exercise/exercise-api';
import { Exercise, MUSCLE_GROUPS, MuscleGroup } from '../../core/exercise/exercise.model';
import { Icon } from '../../shared/icon/icon';
import { ACCEPTED_IMAGE_TYPES, ImageResizer } from '../../shared/image/image-resizer';

/** Šta se radi sa slikom pri čuvanju: ništa, nova slika ili uklanjanje postojeće. */
type ImageChange = { kind: 'keep' } | { kind: 'new'; blob: Blob; preview: string } | { kind: 'remove' };

@Component({
  selector: 'app-exercise-form',
  imports: [ReactiveFormsModule, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './exercise-form.html',
})
export class ExerciseForm implements OnInit {
  private readonly api = inject(ExerciseApi);
  private readonly resizer = inject(ImageResizer);

  readonly exercise = input<Exercise | null>(null);
  readonly defaultGroup = input<MuscleGroup | null>(null);
  /** Admin ekran: vežba koju pravi admin je uvek gotova (sistemska); utiče samo na naslov. */
  readonly systemOnly = input(false);

  readonly saved = output<Exercise>();
  readonly cancelled = output<void>();

  protected readonly groups = MUSCLE_GROUPS;
  protected readonly acceptedTypes = ACCEPTED_IMAGE_TYPES.join(',');
  protected readonly submitting = signal(false);
  protected readonly preparingImage = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly imageError = signal<string | null>(null);

  // vežba napravljena pre nego što je upload slike pukao; sledeći pokušaj je menja, ne pravi novu
  private readonly created = signal<Exercise | null>(null);
  protected readonly imageChange = signal<ImageChange>({ kind: 'keep' });

  /** Slika koja se trenutno prikazuje u formi. */
  protected readonly imagePreview = computed(() => {
    const change = this.imageChange();
    if (change.kind === 'new') return change.preview;
    if (change.kind === 'remove') return null;
    return (this.created() ?? this.exercise())?.imageUrl ?? null;
  });

  protected readonly form = inject(NonNullableFormBuilder).group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
    muscleGroup: ['' as MuscleGroup | '', Validators.required],
    description: [''],
    videoUrl: ['', [Validators.pattern(/^https?:\/\/\S+$/), Validators.maxLength(500)]],
  });

  constructor() {
    inject(DestroyRef).onDestroy(() => this.releasePreview());
  }

  ngOnInit(): void {
    const exercise = this.exercise();
    if (exercise) {
      this.form.patchValue({
        name: exercise.name,
        muscleGroup: exercise.muscleGroup,
        description: exercise.description ?? '',
        videoUrl: exercise.videoUrl ?? '',
      });
    } else if (this.defaultGroup()) {
      this.form.controls.muscleGroup.setValue(this.defaultGroup()!);
    }
  }

  protected invalid(name: 'name' | 'muscleGroup' | 'videoUrl'): boolean {
    const control = this.form.controls[name];
    return control.invalid && control.touched;
  }

  protected async pickImage(input: HTMLInputElement): Promise<void> {
    const file = input.files?.[0];
    input.value = ''; // da isti fajl može ponovo da se izabere
    if (!file) return;

    this.imageError.set(null);
    this.preparingImage.set(true);
    try {
      const blob = await this.resizer.resize(file);
      this.releasePreview();
      this.imageChange.set({ kind: 'new', blob, preview: URL.createObjectURL(blob) });
    } catch (err) {
      this.imageError.set(err instanceof Error ? err.message : 'Slika ne može da se obradi.');
    } finally {
      this.preparingImage.set(false);
    }
  }

  protected removeImage(): void {
    this.releasePreview();
    this.imageChange.set({ kind: 'remove' });
  }

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const value = this.form.getRawValue();
    const request = {
      name: value.name.trim(),
      muscleGroup: value.muscleGroup as MuscleGroup,
      description: value.description.trim() || null,
      videoUrl: value.videoUrl.trim() || null,
    };
    const existing = this.created() ?? this.exercise();

    this.submitting.set(true);
    this.error.set(null);
    (existing ? this.api.update(existing.id, request) : this.api.create(request))
      .pipe(
        // podaci vežbe idu kao JSON, a slika posebnim multipart zahtevom
        switchMap((exercise) => {
          this.created.set(exercise);
          return this.applyImage(exercise);
        }),
      )
      .subscribe({
        next: (exercise) => this.saved.emit(exercise),
        error: (err) => {
          this.error.set(
            this.created() && !this.exercise()
              ? `Vežba je sačuvana, ali slika nije: ${errorMessage(err)}`
              : errorMessage(err),
          );
          this.submitting.set(false);
        },
      });
  }

  private applyImage(exercise: Exercise): Observable<Exercise> {
    const change = this.imageChange();
    if (change.kind === 'new') {
      return this.api.uploadImage(exercise.id, change.blob);
    }
    if (change.kind === 'remove' && exercise.imageUrl) {
      return this.api.deleteImage(exercise.id).pipe(map(() => ({ ...exercise, imageUrl: null })));
    }
    return of(exercise);
  }

  private releasePreview(): void {
    const change = this.imageChange();
    if (change.kind === 'new') URL.revokeObjectURL(change.preview);
  }
}
