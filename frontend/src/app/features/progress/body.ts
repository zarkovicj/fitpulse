import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, effect, inject, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';

import { errorMessage } from '../../core/api/api-error';
import { ProgressApi } from '../../core/progress/progress-api';
import { BodyGoal, WeightEntry } from '../../core/progress/progress.model';
import { CurrentUser } from '../../core/user/current-user';
import { ChartPoint, LineChart } from '../../shared/chart/line-chart';
import { ConfirmDialog } from '../../shared/dialog/confirm-dialog';
import { Icon } from '../../shared/icon/icon';
import { dayToTime, formatDay, formatNumber, localDate } from '../../shared/text/format';

type Range = 3 | 6 | 12;

@Component({
  selector: 'app-body',
  imports: [ReactiveFormsModule, LineChart, Icon, ConfirmDialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './body.html',
})
export class Body {
  private readonly api = inject(ProgressApi);
  private readonly currentUser = inject(CurrentUser);
  private readonly fb = inject(NonNullableFormBuilder);

  protected readonly today = localDate();
  protected readonly ranges: { value: Range; label: string }[] = [
    { value: 3, label: '3 meseca' },
    { value: 6, label: '6 meseci' },
    { value: 12, label: 'Godina' },
  ];
  protected readonly range = signal<Range>(3);

  protected readonly weights = httpResource<WeightEntry[]>(() => {
    const from = new Date();
    from.setMonth(from.getMonth() - this.range());
    return { url: '/api/progress/weight', params: { from: localDate(from), to: this.today } };
  });
  protected readonly goal = httpResource<BodyGoal>(() => '/api/progress/goal');

  private readonly entries = computed(() => (this.weights.hasValue() ? (this.weights.value() ?? []) : []));
  protected readonly recent = computed(() => [...this.entries()].reverse().slice(0, 10));
  protected readonly goalValue = computed(() => (this.goal.hasValue() ? this.goal.value() : null));

  protected readonly current = computed(() => this.currentUser.user()?.weight ?? null);
  protected readonly toGoal = computed(() => {
    const current = this.current();
    const target = this.goalValue()?.weight;
    if (current === null || target == null) return null;
    const diff = Math.round((current - target) * 10) / 10;
    if (diff === 0) return 'Na cilju si.';
    return diff > 0 ? `Još ${formatNumber(diff)} kg do cilja.` : `Još ${formatNumber(-diff)} kg do cilja.`;
  });

  protected readonly chartPoints = computed<ChartPoint[]>(() =>
    this.entries().map((e) => ({ x: dayToTime(e.date), y: e.weight })),
  );
  protected readonly reference = computed(() => {
    const target = this.goalValue()?.weight;
    return target ? { value: target, label: `Cilj ${formatNumber(target)} kg` } : null;
  });
  protected readonly formatWeight = (v: number) => `${formatNumber(v)} kg`;
  protected readonly formatNumber = formatNumber;
  protected readonly formatDay = formatDay;

  // ---- unos merenja ----
  protected readonly weightForm = this.fb.group({
    date: [this.today, Validators.required],
    weight: this.fb.control<number | null>(null, [Validators.required, Validators.min(20), Validators.max(500)]),
  });
  protected readonly savingWeight = signal(false);
  protected readonly weightError = signal<string | null>(null);

  // ---- cilj ----
  protected readonly goalForm = this.fb.group({
    weight: this.fb.control<number | null>(null, [Validators.min(20), Validators.max(500)]),
    bodyFatPercent: this.fb.control<number | null>(null, [Validators.min(1), Validators.max(70)]),
  });
  protected readonly savingGoal = signal(false);
  protected readonly goalMessage = signal<{ ok: boolean; text: string } | null>(null);

  protected readonly deleting = signal<WeightEntry | null>(null);

  constructor() {
    // forma za cilj se popuni kad stigne sa servera
    effect(() => {
      const goal = this.goalValue();
      if (goal && this.goalForm.pristine) this.goalForm.reset({ weight: goal.weight, bodyFatPercent: goal.bodyFatPercent });
    });
  }

  protected saveWeight(): void {
    if (this.weightForm.invalid) {
      this.weightForm.markAllAsTouched();
      return;
    }
    const { date, weight } = this.weightForm.getRawValue();
    if (date > this.today) {
      this.weightError.set('Merenje ne može biti u budućnosti.');
      return;
    }

    this.savingWeight.set(true);
    this.weightError.set(null);
    this.api.logWeight(date, weight!).subscribe({
      next: () => {
        this.savingWeight.set(false);
        this.weightForm.reset({ date: this.today, weight: null });
        this.afterWeightChange();
      },
      error: (err) => {
        this.savingWeight.set(false);
        this.weightError.set(errorMessage(err));
      },
    });
  }

  protected confirmDelete(): void {
    const entry = this.deleting();
    if (!entry) return;
    this.api.deleteWeight(entry.date).subscribe({
      next: () => {
        this.deleting.set(null);
        this.afterWeightChange();
      },
      error: (err) => {
        this.deleting.set(null);
        this.weightError.set(errorMessage(err));
      },
    });
  }

  protected saveGoal(): void {
    if (this.goalForm.invalid) {
      this.goalForm.markAllAsTouched();
      return;
    }
    const { weight, bodyFatPercent } = this.goalForm.getRawValue();

    this.savingGoal.set(true);
    this.goalMessage.set(null);
    this.api.updateGoal({ weight: weight || null, bodyFatPercent: bodyFatPercent || null }).subscribe({
      next: (goal) => {
        this.savingGoal.set(false);
        this.goal.set(goal);
        this.goalForm.reset({ weight: goal.weight, bodyFatPercent: goal.bodyFatPercent });
        this.goalMessage.set({ ok: true, text: 'Cilj je sačuvan.' });
      },
      error: (err) => {
        this.savingGoal.set(false);
        this.goalMessage.set({ ok: false, text: errorMessage(err) });
      },
    });
  }

  // trenutna masa u profilu prati poslednje merenje, pa se i on osvežava
  private afterWeightChange(): void {
    this.weights.reload();
    this.currentUser.reload();
  }
}
