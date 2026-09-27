import { ChangeDetectionStrategy, Component, input } from '@angular/core';

export type IconName =
  | 'home'
  | 'dumbbell'
  | 'clipboard'
  | 'history'
  | 'chart'
  | 'user'
  | 'shield'
  | 'logout'
  | 'plus'
  | 'close'
  | 'search'
  | 'pencil'
  | 'trash'
  | 'arrow-up'
  | 'arrow-down'
  | 'arrow-left'
  | 'check'
  | 'copy'
  | 'play'
  | 'timer'
  | 'bell';

@Component({
  selector: 'app-icon',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'inline-block shrink-0', 'aria-hidden': 'true' },
  template: `
    <svg
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      stroke-width="1.8"
      stroke-linecap="round"
      stroke-linejoin="round"
      [attr.width]="size()"
      [attr.height]="size()"
    >
      @switch (name()) {
        @case ('home') {
          <path d="M3 10.5 12 3l9 7.5" />
          <path d="M5 9.5V21h14V9.5" />
          <path d="M10 21v-6h4v6" />
        }
        @case ('dumbbell') {
          <path d="M6.5 6.5v11M17.5 6.5v11" />
          <path d="M3.5 9v6M20.5 9v6" />
          <path d="M6.5 12h11" />
        }
        @case ('clipboard') {
          <rect x="5" y="4" width="14" height="17" rx="2" />
          <path d="M9 4V3h6v1" />
          <path d="M9 10h6M9 14h6M9 18h3" />
        }
        @case ('history') {
          <path d="M3 12a9 9 0 1 0 3-6.7" />
          <path d="M3 4v4h4" />
          <path d="M12 7v5l3 2" />
        }
        @case ('chart') {
          <path d="M4 20V4" />
          <path d="M4 20h16" />
          <path d="m7 15 4-4 3 3 5-6" />
        }
        @case ('user') {
          <circle cx="12" cy="8" r="4" />
          <path d="M4 21c0-4 3.6-7 8-7s8 3 8 7" />
        }
        @case ('shield') {
          <path d="M12 3 4.5 6v6c0 4.5 3.2 7.8 7.5 9 4.3-1.2 7.5-4.5 7.5-9V6z" />
          <path d="m9 12 2 2 4-4" />
        }
        @case ('logout') {
          <path d="M15 4h3a2 2 0 0 1 2 2v12a2 2 0 0 1-2 2h-3" />
          <path d="M10 16l-4-4 4-4" />
          <path d="M6 12h10" />
        }
        @case ('plus') {
          <path d="M12 5v14M5 12h14" />
        }
        @case ('close') {
          <path d="M6 6l12 12M18 6 6 18" />
        }
        @case ('search') {
          <circle cx="11" cy="11" r="6.5" />
          <path d="m20 20-4.2-4.2" />
        }
        @case ('pencil') {
          <path d="M4 20h4L19 9a2.8 2.8 0 0 0-4-4L4 16z" />
          <path d="m13.5 6.5 4 4" />
        }
        @case ('arrow-up') {
          <path d="M12 19V5M6 11l6-6 6 6" />
        }
        @case ('arrow-down') {
          <path d="M12 5v14M6 13l6 6 6-6" />
        }
        @case ('arrow-left') {
          <path d="M19 12H5M11 6l-6 6 6 6" />
        }
        @case ('check') {
          <path d="m5 12.5 4.5 4.5L19 7.5" />
        }
        @case ('copy') {
          <rect x="8" y="8" width="12" height="12" rx="2" />
          <path d="M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2" />
        }
        @case ('timer') {
          <circle cx="12" cy="13.5" r="7.5" />
          <path d="M12 9.5v4l2.5 1.5" />
          <path d="M10 2.5h4" />
        }
        @case ('bell') {
          <path d="M6 16V11a6 6 0 0 1 12 0v5l1.5 2h-15z" />
          <path d="M10 20.5a2 2 0 0 0 4 0" />
        }
        @case ('play') {
          <path d="M7 4.5v15l12.5-7.5z" />
        }
        @case ('trash') {
          <path d="M4 7h16" />
          <path d="M9 7V4h6v3" />
          <path d="M6 7l1 13h10l1-13" />
          <path d="M10 11v5M14 11v5" />
        }
      }
    </svg>
  `,
})
export class Icon {
  readonly name = input.required<IconName>();
  readonly size = input(20);
}
