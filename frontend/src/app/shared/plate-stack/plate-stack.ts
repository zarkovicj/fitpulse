import { ChangeDetectionStrategy, Component } from '@angular/core';

interface Plate {
  color: string;
  width: number;
  height: number;
}

// kraj šipke sa boka: najteži teg uz obruč, lakši ka spolja (takmičarske boje)
const PLATES: Plate[] = [
  { color: 'bg-plate-red', width: 30, height: 100 },
  { color: 'bg-plate-blue', width: 26, height: 100 },
  { color: 'bg-plate-yellow', width: 22, height: 100 },
  { color: 'bg-plate-green', width: 18, height: 100 },
  { color: 'bg-paper', width: 12, height: 52 },
  { color: 'bg-plate-red', width: 9, height: 40 },
];

@Component({
  selector: 'app-plate-stack',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'block', 'aria-hidden': 'true' },
  template: `
    <div class="flex h-44 items-center">
      <div class="h-2.5 flex-1 rounded-l-sm bg-line/60"></div>
      <div class="mr-[3px] h-12 w-4 rounded-sm bg-line"></div>
      @for (plate of plates; track $index) {
        <div
          class="mr-[3px] rounded-[5px] shadow-[inset_-3px_0_0_rgba(0,0,0,0.18)]"
          [class]="plate.color"
          [style.width.px]="plate.width"
          [style.height.%]="plate.height"
        ></div>
      }
      <div class="h-7 w-3 rounded-sm bg-slate"></div>
      <div class="h-4 w-10 rounded-r-sm bg-line/80"></div>
    </div>
  `,
})
export class PlateStack {
  protected readonly plates = PLATES;
}
