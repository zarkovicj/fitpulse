import { ChangeDetectionStrategy, Component } from '@angular/core';

import { PlateStack } from '../../shared/plate-stack/plate-stack';

@Component({
  selector: 'app-auth-layout',
  imports: [PlateStack],
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block min-h-dvh lg:grid lg:grid-cols-[minmax(0,5fr)_minmax(0,6fr)]' },
  template: `
    <aside class="flex flex-col justify-between bg-steel px-6 py-6 text-paper lg:px-12 lg:py-12">
      <p class="font-display text-2xl font-bold tracking-wide">FitPulse</p>

      <div class="hidden lg:block">
        <app-plate-stack class="max-w-sm" />
        <p class="mt-10 max-w-sm font-display text-5xl leading-[0.95] font-semibold">
          Svaka serija ostaje zapisana.
        </p>
        <p class="mt-4 max-w-sm text-lg text-paper/70">
          Beleži treninge, prati lične rekorde i gledaj kako kilaža raste iz nedelje u nedelju.
        </p>
      </div>

      <div class="hidden lg:block"></div>
    </aside>

    <main class="flex items-start justify-center px-6 py-10 lg:items-center lg:py-12">
      <div class="w-full max-w-md">
        <ng-content />
      </div>
    </main>
  `,
})
export class AuthLayout {}
