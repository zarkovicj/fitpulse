import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-progress-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="max-w-4xl">
      <h1 class="font-display text-4xl font-bold">Napredak</h1>
      <nav class="mt-4 flex gap-1 border-b border-line" aria-label="Napredak">
        <a routerLink="/progress" [routerLinkActiveOptions]="{ exact: true }" routerLinkActive="tab-on" class="tab">Snaga</a>
        <a routerLink="/progress/body" routerLinkActive="tab-on" class="tab">Telo</a>
      </nav>
      <div class="mt-6">
        <router-outlet />
      </div>
    </div>
  `,
  styles: `
    @reference '../../../styles.css';
    .tab {
      @apply -mb-px border-b-2 border-transparent px-4 py-2.5 font-semibold text-slate hover:text-steel;
    }
    .tab.tab-on {
      @apply border-plate-blue text-steel;
    }
  `,
})
export class ProgressLayout {}
