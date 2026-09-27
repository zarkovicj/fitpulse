import {
  ChangeDetectionStrategy,
  Component,
  computed,
  inject,
  input,
  linkedSignal,
  output,
  signal,
} from '@angular/core';
import { NgTemplateOutlet } from '@angular/common';
import { Router } from '@angular/router';
import { Observable, finalize } from 'rxjs';

import { ExerciseInfo } from '../../core/exercise/exercise-info';
import { errorMessage } from '../../core/api/api-error';
import { Ownership } from '../../core/auth/ownership';
import { Exercise, muscleGroupLabel } from '../../core/exercise/exercise.model';
import { Clock } from '../../core/time/clock';
import { ActiveWorkout } from '../../core/workout/active-workout';
import { RestTimer } from '../../core/workout/rest-timer';
import { WorkoutApi } from '../../core/workout/workout-api';
import { Workout, WorkoutExercise, WorkoutSet, workoutTitle } from '../../core/workout/workout.model';
import { ConfirmDialog } from '../../shared/dialog/confirm-dialog';
import { Icon } from '../../shared/icon/icon';
import { formatDuration, formatRest } from '../../shared/text/format';
import { ExercisePicker } from '../exercises/exercise-picker';
import { FinishDialog } from './finish-dialog';

@Component({
  selector: 'app-live-workout',
  imports: [NgTemplateOutlet, Icon, ExercisePicker, ConfirmDialog, FinishDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './live-workout.html',
})
export class LiveWorkout {
  private readonly api = inject(WorkoutApi);
  protected readonly info = inject(ExerciseInfo);
  private readonly active = inject(ActiveWorkout);
  private readonly router = inject(Router);
  private readonly clock = inject(Clock);
  private readonly ownership = inject(Ownership);

  readonly initial = input.required<Workout>();
  readonly ended = output<Workout>();

  // lokalna kopija koja se menja odmah, a server se ažurira u pozadini
  protected readonly workout = linkedSignal(() => this.initial());

  protected readonly title = computed(() => workoutTitle(this.workout()));
  protected readonly elapsed = computed(() =>
    formatDuration(this.clock.now() - Date.parse(this.workout().startedAt)),
  );

  // ---- odmor između serija ----
  protected readonly rest = inject(RestTimer);
  protected readonly restLeft = computed(() => {
    const remaining = this.rest.remaining();
    return remaining === null ? null : formatDuration(remaining * 1000);
  });
  protected readonly restPresets = [30, 60, 90, 120, 180];
  protected readonly formatRest = formatRest;
  /** Serija čiji se odmor trenutno menja. */
  protected readonly editingRest = signal<number | null>(null);
  protected readonly notificationsAsked = signal(false);
  protected readonly canAskNotifications = computed(
    () => !this.notificationsAsked() && this.rest.notificationPermission() === 'default',
  );

  protected readonly progress = computed(() => {
    const sets = this.workout().exercises.flatMap((e) => e.sets);
    return { done: sets.filter((s) => s.completed).length, total: sets.length };
  });

  private readonly pending = signal(0);
  protected readonly saving = computed(() => this.pending() > 0);
  protected readonly error = signal<string | null>(null);

  protected readonly pickerOpen = signal(false);
  protected readonly removingExercise = signal<WorkoutExercise | null>(null);
  protected readonly finishing = signal(false);
  protected readonly cancelling = signal(false);
  protected readonly busy = signal(false);

  protected readonly groupLabel = muscleGroupLabel;
  protected readonly addedIds = computed(() => this.workout().exercises.map((e) => e.exerciseId));
  protected readonly allowedExercise = (e: Exercise) => e.system || this.ownership.isMine(e.createdBy);

  protected doneCount(exercise: WorkoutExercise): number {
    return exercise.sets.filter((s) => s.completed).length;
  }

  // ---- serije ----

  protected toggle(exercise: WorkoutExercise, set: WorkoutSet): void {
    const completed = !set.completed;
    if (!completed) {
      // poništena serija gasi i svoj odmor
      if (this.rest.setId() === set.id) this.rest.stop();
      this.saveSet(exercise, { ...set, completed }, set);
      return;
    }
    // urađena serija pokreće odbrojavanje, a odmor se čuva uz seriju za sledeći trening;
    // otvoren izbor odmora neke druge serije se zatvara, jer kontrole sada stoje uz odbrojavanje
    this.editingRest.set(null);
    const seconds = this.restOf(exercise, set);
    this.rest.start(set.id, seconds, exercise.exerciseName);
    this.saveSet(exercise, { ...set, completed, restAfter: seconds }, set);
  }

  /**
   * Odmor posle serije: sačuvan uz nju (backend ga prenosi iz prošlog treninga),
   * inače kao prethodna serija, inače poslednji izbor za ovu vežbu.
   */
  protected restOf(exercise: WorkoutExercise, set: WorkoutSet): number {
    if (set.restAfter) return set.restAfter;
    const before = exercise.sets.filter((s) => s.position < set.position && s.restAfter).at(-1)?.restAfter;
    return this.rest.restFor(exercise.exerciseId, before ?? null);
  }

  /** Nova vrednost važi za tu seriju i za sledeće neurađene serije koje su imale isti odmor. */
  protected setRest(exercise: WorkoutExercise, set: WorkoutSet, seconds: number): void {
    const value = Math.min(600, Math.max(15, seconds));
    const old = this.restOf(exercise, set);
    if (value === old) return;

    const following = exercise.sets.filter(
      (s) => s.position > set.position && !s.completed && this.restOf(exercise, s) === old,
    );
    for (const target of [set, ...following]) {
      this.saveSet(exercise, { ...target, restAfter: value }, target);
      this.rest.retime(target.id, value);
    }
    this.rest.setRestFor(exercise.exerciseId, value);
  }

  protected async askNotifications(): Promise<void> {
    this.notificationsAsked.set(true);
    await this.rest.enableNotifications();
  }



  // neispravan unos se samo vrati na staru vrednost, bez poruke
  protected changeReps(exercise: WorkoutExercise, set: WorkoutSet, field: HTMLInputElement): void {
    const raw = field.value.trim();
    const reps = Number(raw);
    if (raw === '' || !Number.isInteger(reps) || reps < 0 || reps > 1000) {
      field.value = String(set.reps);
      return;
    }
    if (reps === set.reps) return;
    this.cascade(exercise, set, (s) => s.reps === set.reps, (s) => ({ ...s, reps: reps }));
  }

  protected changeKg(exercise: WorkoutExercise, set: WorkoutSet, field: HTMLInputElement): void {
    const raw = field.value.trim().replace(',', '.');
    const kg = raw === '' ? null : Number(raw);
    if (kg !== null && (Number.isNaN(kg) || kg < 0 || kg > 1000)) {
      field.value = set.weight?.toString() ?? '';
      return;
    }
    const normalized = kg === 0 ? null : kg;
    if (normalized === set.weight) return;
    this.cascade(exercise, set, (s) => s.weight === set.weight, (s) => ({ ...s, weight: normalized }));
  }

  // izmena se prenosi i na sledeće neurađene serije koje su imale istu staru vrednost,
  // da se ista kilaža ne kuca za svaku seriju posebno
  private cascade(
    exercise: WorkoutExercise,
    changed: WorkoutSet,
    hadSameValue: (s: WorkoutSet) => boolean,
    apply: (s: WorkoutSet) => WorkoutSet,
  ): void {
    const following = exercise.sets.filter((s) => s.position > changed.position && !s.completed && hadSameValue(s));
    for (const set of [changed, ...following]) {
      this.saveSet(exercise, apply(set), set);
    }
  }

  protected addSet(exercise: WorkoutExercise): void {
    this.track(this.api.addSet(this.workout().id, exercise.id)).subscribe({
      next: (set) => this.updateExercise(exercise.id, (e) => ({ ...e, sets: [...e.sets, set] })),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  protected removeSet(exercise: WorkoutExercise, set: WorkoutSet): void {
    if (this.rest.setId() === set.id) this.rest.stop();
    const index = exercise.sets.findIndex((s) => s.id === set.id);
    this.updateExercise(exercise.id, (e) => ({
      ...e,
      sets: e.sets.filter((s) => s.id !== set.id).map((s, i) => ({ ...s, position: i + 1 })),
    }));
    this.track(this.api.removeSet(this.workout().id, exercise.id, set.id)).subscribe({
      error: (err) => {
        this.error.set(errorMessage(err));
        // vraća se samo obrisana serija, a izmene ostalih napravljene u međuvremenu ostaju
        this.updateExercise(exercise.id, (e) => ({
          ...e,
          sets: insertAt(e.sets, index, set).map((s, i) => ({ ...s, position: i + 1 })),
        }));
      },
    });
  }

  // Po seriji ide najviše jedan zahtev; izmene napravljene u međuvremenu šalju se posle njega
  // (dovoljna je poslednja, jer nosi celo stanje serije). Tako zahtevi ne mogu da stignu obrnutim redom.
  private readonly savingSets = new Set<number>();
  private readonly queuedSets = new Map<number, { exerciseId: number; set: WorkoutSet }>();
  /** Poslednje stanje serije koje je server potvrdio, za vraćanje posle greške. */
  private readonly savedSets = new Map<number, WorkoutSet>();

  private saveSet(exercise: WorkoutExercise, next: WorkoutSet, previous: WorkoutSet): void {
    if (!this.savedSets.has(next.id)) this.savedSets.set(next.id, previous);
    this.replaceSet(exercise.id, next);
    if (this.savingSets.has(next.id)) {
      this.queuedSets.set(next.id, { exerciseId: exercise.id, set: next });
      return;
    }
    this.sendSet(exercise.id, next);
  }

  private sendSet(exerciseId: number, set: WorkoutSet): void {
    this.savingSets.add(set.id);
    this.track(
      this.api.updateSet(this.workout().id, exerciseId, set.id, {
        reps: set.reps,
        weight: set.weight,
        completed: set.completed,
        restAfter: set.restAfter,
      }),
    )
      .pipe(
        finalize(() => {
          this.savingSets.delete(set.id);
          const queued = this.queuedSets.get(set.id);
          if (queued) {
            this.queuedSets.delete(set.id);
            this.sendSet(queued.exerciseId, queued.set);
          }
        }),
      )
      .subscribe({
        next: () => this.savedSets.set(set.id, set),
        error: (err) => {
          this.error.set(errorMessage(err));
          // novija izmena koja čeka ionako šalje celo stanje, pa se prikaz ne vraća
          if (this.queuedSets.has(set.id)) return;
          const saved = this.savedSets.get(set.id);
          if (saved) this.revertSet(exerciseId, saved);
        },
      });
  }

  // vraćaju se samo vrednosti; redni broj je možda promenjen brisanjem druge serije
  private revertSet(exerciseId: number, saved: WorkoutSet): void {
    this.updateExercise(exerciseId, (e) => ({
      ...e,
      sets: e.sets.map((s) =>
        s.id === saved.id
          ? { ...s, reps: saved.reps, weight: saved.weight, completed: saved.completed, restAfter: saved.restAfter }
          : s,
      ),
    }));
  }

  // ---- vežbe ----

  protected addExercise(exercise: Exercise): void {
    this.track(this.api.addExercise(this.workout().id, exercise.id)).subscribe({
      next: (added) => this.workout.update((w) => ({ ...w, exercises: [...w.exercises, added] })),
      error: (err) => this.error.set(errorMessage(err)),
    });
  }

  protected confirmRemoveExercise(): void {
    const exercise = this.removingExercise();
    if (!exercise) return;
    this.removingExercise.set(null);
    const index = this.workout().exercises.findIndex((e) => e.id === exercise.id);
    this.workout.update((w) => ({ ...w, exercises: w.exercises.filter((e) => e.id !== exercise.id) }));
    this.track(this.api.removeExercise(this.workout().id, exercise.id)).subscribe({
      error: (err) => {
        this.error.set(errorMessage(err));
        // vraća se samo ta vežba; ostatak treninga zadržava izmene napravljene u međuvremenu
        this.workout.update((w) => ({ ...w, exercises: insertAt(w.exercises, index, exercise) }));
      },
    });
  }

  // ---- kraj treninga ----

  protected finish(updateTemplate: boolean): void {
    this.busy.set(true);
    this.api.finish(this.workout().id, updateTemplate).subscribe({
      next: (workout) => {
        this.active.set(null);
        this.rest.stop();
        this.finishing.set(false);
        this.ended.emit(workout);
      },
      error: (err) => {
        this.busy.set(false);
        this.finishing.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  protected cancel(): void {
    this.busy.set(true);
    this.api.cancel(this.workout().id).subscribe({
      next: () => {
        this.active.set(null);
        this.rest.stop();
        this.router.navigate(['/workouts']);
      },
      error: (err) => {
        this.busy.set(false);
        this.cancelling.set(false);
        this.error.set(errorMessage(err));
      },
    });
  }

  // ---- pomoćne ----

  private track<T>(request: Observable<T>): Observable<T> {
    this.pending.update((n) => n + 1);
    this.error.set(null);
    return request.pipe(finalize(() => this.pending.update((n) => n - 1)));
  }

  private updateExercise(exerciseId: number, change: (e: WorkoutExercise) => WorkoutExercise): void {
    this.workout.update((w) => ({
      ...w,
      exercises: w.exercises.map((e) => (e.id === exerciseId ? change(e) : e)),
    }));
  }

  private replaceSet(exerciseId: number, set: WorkoutSet): void {
    this.updateExercise(exerciseId, (e) => ({
      ...e,
      sets: e.sets.map((s) => (s.id === set.id ? set : s)),
    }));
  }
}

function insertAt<T>(list: T[], index: number, item: T): T[] {
  const copy = [...list];
  copy.splice(Math.min(Math.max(index, 0), copy.length), 0, item);
  return copy;
}
