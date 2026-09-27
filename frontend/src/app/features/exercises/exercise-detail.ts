import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, linkedSignal } from '@angular/core';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { RouterLink } from '@angular/router';

import { errorMessage } from '../../core/api/api-error';
import { ExerciseInfo } from '../../core/exercise/exercise-info';
import { Exercise, muscleGroupLabel } from '../../core/exercise/exercise.model';
import { ExerciseProgressPoint, PersonalRecord, RECORD_LABELS, RecordType } from '../../core/progress/progress.model';
import { ChartPoint, LineChart } from '../../shared/chart/line-chart';
import { Dialog } from '../../shared/dialog/dialog';
import { Icon } from '../../shared/icon/icon';
import { dayToTime, formatDay, formatKg, formatNumber, localDate } from '../../shared/text/format';
import { youtubeEmbedUrl, youtubeId } from '../../shared/text/video';

type Tab = 'about' | 'history' | 'records';

const RECORD_ORDER: RecordType[] = ['MAX_WEIGHT', 'ESTIMATED_1RM', 'MAX_REPS'];

/** Detalji vežbe u modalu: opis i video, istorija i lični rekordi (kao u aplikaciji Strong). */
@Component({
  selector: 'app-exercise-detail',
  imports: [RouterLink, Dialog, Icon, LineChart],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './exercise-detail.html',
  styles: `
    @reference '../../../styles.css';
    .detail-tab {
      @apply -mb-px border-b-2 border-transparent px-3 py-2 font-semibold text-slate hover:text-steel;
    }
    .detail-tab[aria-selected='true'] {
      @apply border-plate-blue text-steel;
    }
  `,
})
export class ExerciseDetail {
  private readonly sanitizer = inject(DomSanitizer);
  protected readonly info = inject(ExerciseInfo);

  private readonly url = (path: string) => {
    const id = this.info.exerciseId();
    return id === null ? undefined : `${path}${id}`;
  };

  protected readonly exercise = httpResource<Exercise>(() => this.url('/api/exercises/'));
  protected readonly history = httpResource<ExerciseProgressPoint[]>(() => this.url('/api/progress/exercises/'));
  protected readonly records = httpResource<PersonalRecord[]>(() => this.url('/api/progress/records?exerciseId='));

  // svako otvaranje kreće od prve kartice
  protected readonly tab = linkedSignal<number | null, Tab>({ source: this.info.exerciseId, computation: () => 'about' });

  protected readonly value = computed(() => (this.exercise.hasValue() ? this.exercise.value() : null));
  protected readonly loadError = computed(() =>
    this.exercise.error() ? errorMessage(this.exercise.error(), 'Vežba nije učitana.') : null,
  );

  // iframe dobija samo adresu sastavljenu od proverenog YouTube ID-a
  protected readonly embed = computed<SafeResourceUrl | null>(() => {
    const id = youtubeId(this.value()?.videoUrl);
    return id ? this.sanitizer.bypassSecurityTrustResourceUrl(youtubeEmbedUrl(id)) : null;
  });

  protected readonly sessions = computed(() =>
    [...(this.history.hasValue() ? (this.history.value() ?? []) : [])].reverse(),
  );
  protected readonly chartPoints = computed<ChartPoint[]>(() => {
    // jedna tačka po danu, najbolji 1RM tog dana
    const best = new Map<string, number>();
    for (const p of this.history.hasValue() ? (this.history.value() ?? []) : []) {
      if (p.best1rm) best.set(p.date, Math.max(best.get(p.date) ?? 0, p.best1rm));
    }
    return [...best].map(([date, y]) => ({ x: dayToTime(date), y }));
  });

  protected readonly recordList = computed(() => {
    const all = this.records.hasValue() ? (this.records.value() ?? []) : [];
    return RECORD_ORDER.map((type) => all.find((r) => r.type === type)).filter((r) => r !== undefined);
  });

  protected readonly groupLabel = muscleGroupLabel;
  protected readonly recordLabels = RECORD_LABELS;
  protected readonly formatKg = formatKg;
  protected readonly formatDay = formatDay;
  protected readonly formatWeight = (v: number) => `${formatNumber(v)} kg`;

  protected recordValue(record: PersonalRecord): { main: string; detail: string | null } {
    switch (record.type) {
      case 'MAX_WEIGHT':
        return { main: formatKg(record.weight) ?? '–', detail: `× ${record.reps}` };
      case 'ESTIMATED_1RM':
        return { main: formatKg(record.estimated1rm) ?? '–', detail: `iz ${formatKg(record.weight)} × ${record.reps}` };
      case 'MAX_REPS':
        return { main: `${record.reps}`, detail: record.weight ? `sa ${formatKg(record.weight)}` : 'bez tega' };
    }
  }

  protected recordDate(record: PersonalRecord): string {
    return formatDay(localDate(record.achievedAt));
  }

  protected volume(value: number): string {
    return value > 0 ? `${formatNumber(value)} kg` : '–';
  }
}
