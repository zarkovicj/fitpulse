import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';

import { AuthService } from '../../core/auth/auth';
import { Exercise } from '../../core/exercise/exercise.model';
import { Admin } from './admin';

const exercise = (id: number, name: string, createdBy: number | null): Exercise => ({
  id,
  name,
  muscleGroup: 'CHEST',
  imageUrl: null,
  description: null,
  videoUrl: null,
  createdBy,
  system: createdBy === null,
});

describe('Admin', () => {
  let fixture: ComponentFixture<Admin>;
  let http: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { userId: signal(1), isAdmin: signal(true) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(Admin);
    TestBed.tick();
    // admin dobija i tuđe privatne vežbe, a ovde treba da vidi samo sistemske
    http.expectOne('/api/exercises').flush([exercise(1, 'Bench Press', null), exercise(2, 'Moja vežba', 7)]);
    http.expectOne('/api/templates').flush([]);
    await fixture.whenStable();
  });

  it('shouldListOnlySystemExercises', () => {
    const names = [...fixture.nativeElement.querySelectorAll('tbody td.font-semibold')].map((td: Element) =>
      td.textContent!.trim(),
    );
    expect(names).toEqual(['Bench Press']);
  });

  it('deleteInUse_shouldExplainWhy', async () => {
    fixture.nativeElement.querySelector('button[aria-label="Obriši: Bench Press"]').click();
    await fixture.whenStable();
    [...fixture.nativeElement.querySelectorAll('dialog button')]
      .find((b: HTMLButtonElement) => b.textContent!.includes('Obriši za sve'))
      .click();

    http.expectOne('/api/exercises/1').flush({ message: 'Resurs je u upotrebi' }, { status: 409, statusText: 'Conflict' });
    await fixture.whenStable();

    expect(fixture.nativeElement.querySelector('dialog [role=alert]').textContent).toContain(
      'koristi u nečijem šablonu ili treningu',
    );
  });
});
