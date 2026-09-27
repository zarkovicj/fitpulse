import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { ExerciseInfo } from '../../core/exercise/exercise-info';
import { Exercise } from '../../core/exercise/exercise.model';
import { ExerciseDetail } from './exercise-detail';

const exercise = (videoUrl: string | null): Exercise => ({
  id: 1,
  name: 'Bench Press',
  muscleGroup: 'CHEST',
  imageUrl: null,
  description: 'Potisak sa šipkom u ležećem položaju',
  videoUrl,
  createdBy: null,
  system: true,
});

describe('ExerciseDetail', () => {
  let fixture: ComponentFixture<ExerciseDetail>;
  let http: HttpTestingController;

  async function open(videoUrl: string | null) {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ExerciseDetail);
    TestBed.inject(ExerciseInfo).open(1);
    TestBed.tick();
    http.expectOne('/api/exercises/1').flush(exercise(videoUrl));
    http.expectOne('/api/progress/exercises/1').flush([]);
    http.expectOne('/api/progress/records?exerciseId=1').flush([]);
    await fixture.whenStable();
    return fixture.nativeElement as HTMLElement;
  }

  it('youtubeLink_shouldBeEmbeddedWithoutTrackingCookies', async () => {
    const root = await open('https://youtu.be/rT7DgCr-3pg');

    const iframe = root.querySelector('iframe')!;
    expect(iframe.src).toBe('https://www.youtube-nocookie.com/embed/rT7DgCr-3pg?rel=0');
    expect(root.textContent).toContain('Potisak sa šipkom');
  });

  it('otherVideoLink_shouldOpenInNewTabInsteadOfEmbedding', async () => {
    const root = await open('https://vimeo.com/123456');

    expect(root.querySelector('iframe')).toBeNull();
    const link = root.querySelector<HTMLAnchorElement>('a[target=_blank]')!;
    expect(link.href).toBe('https://vimeo.com/123456');
    expect(link.rel).toContain('noopener');
  });
});
