import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from '../../core/auth/auth';
import { Exercise } from '../../core/exercise/exercise.model';
import { Exercises } from './exercises';

const LIBRARY: Exercise[] = [
  { id: 1, name: 'Čučanj', muscleGroup: 'LEGS', imageUrl: null, description: 'Sa šipkom', videoUrl: null, createdBy: null, system: true },
  { id: 2, name: 'Bench Press', muscleGroup: 'CHEST', imageUrl: null, description: null, videoUrl: null, createdBy: null, system: true },
  { id: 3, name: 'Bugarski čučanj', muscleGroup: 'LEGS', imageUrl: null, description: null, videoUrl: null, createdBy: 7, system: false },
];

describe('Exercises', () => {
  async function render() {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { userId: signal(7), isAdmin: signal(false) } },
      ],
    });
    const fixture = TestBed.createComponent(Exercises);
    // whenStable čeka i otvorene HTTP zahteve, pa zahtev prvo pokrećemo i odgovaramo na njega
    TestBed.tick();
    TestBed.inject(HttpTestingController).expectOne('/api/exercises').flush(LIBRARY);
    await fixture.whenStable();
    return fixture;
  }

  const names = (root: HTMLElement) =>
    [...root.querySelectorAll('li button.exercise-link')].map((b) => b.textContent!.trim());

  it('shouldGroupExercisesByMuscleGroup', async () => {
    const fixture = await render();
    const headings = [...fixture.nativeElement.querySelectorAll('section h2')].map((h: Element) => h.textContent!.trim());

    expect(headings).toEqual(['Grudi 1', 'Noge 2']);
  });

  it('search_withoutDiacritics_shouldMatch', async () => {
    const fixture = await render();
    const input: HTMLInputElement = fixture.nativeElement.querySelector('input[type=search]');

    input.value = 'cucanj';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();

    expect(names(fixture.nativeElement)).toEqual(['Bugarski čučanj', 'Čučanj']);
  });

  it('editButtons_shouldAppearOnlyForOwnExercises', async () => {
    const fixture = await render();
    const editButtons = fixture.nativeElement.querySelectorAll('button[aria-label^="Izmeni"]');

    expect(editButtons.length).toBe(1);
    expect(editButtons[0].getAttribute('aria-label')).toBe('Izmeni: Bugarski čučanj');
  });
});
