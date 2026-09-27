import { NgTemplateOutlet } from '@angular/common';
import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';

import { errorMessage } from '../../core/api/api-error';
import { Ownership } from '../../core/auth/ownership';
import { MUSCLE_GROUPS } from '../../core/exercise/exercise.model';
import { Template } from '../../core/template/template.model';
import { Icon } from '../../shared/icon/icon';
import { plural } from '../../shared/text/plural';

@Component({
  selector: 'app-templates',
  imports: [RouterLink, NgTemplateOutlet, Icon],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './templates.html',
})
export class Templates {
  protected readonly ownership = inject(Ownership);

  protected readonly templates = httpResource<Template[]>(() => '/api/templates');

  private readonly all = computed(() => (this.templates.hasValue() ? this.templates.value() : []));
  protected readonly mine = computed(() => this.all().filter((t) => !t.system && this.ownership.isMine(t.createdBy)));
  protected readonly system = computed(() => this.all().filter((t) => t.system));

  protected readonly errorText = computed(() =>
    this.templates.error() ? errorMessage(this.templates.error(), 'Šabloni nisu učitani.') : null,
  );

  /** Mišićne grupe koje šablon pokriva, redom kao u biblioteci. */
  protected groupsOf(template: Template): string {
    const present = new Set(template.exercises.map((e) => e.muscleGroup));
    return MUSCLE_GROUPS.filter((g) => present.has(g.value))
      .map((g) => g.label)
      .join(', ');
  }

  protected exerciseCount(template: Template): string {
    const count = template.exercises.length;
    return `${count} ${plural(count, 'vežba', 'vežbe', 'vežbi')}`;
  }

  protected setCount(template: Template): string {
    const count = template.exercises.reduce((sum, e) => sum + e.setCount, 0);
    return `${count} ${plural(count, 'serija', 'serije', 'serija')}`;
  }
}
