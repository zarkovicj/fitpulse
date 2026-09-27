import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';

import { AuthService } from '../../core/auth/auth';
import { Exercise } from '../../core/exercise/exercise.model';
import { Template } from '../../core/template/template.model';
import { TemplateEditor } from './template-editor';

const bench: Exercise = { id: 1, name: 'Bench Press', muscleGroup: 'CHEST', imageUrl: null, description: null, videoUrl: null, createdBy: null, system: true };
const row: Exercise = { id: 2, name: 'Veslanje', muscleGroup: 'BACK', imageUrl: null, description: null, videoUrl: null, createdBy: null, system: true };

describe('TemplateEditor', () => {
  let fixture: ComponentFixture<TemplateEditor>;
  let http: HttpTestingController;
  let navigate: ReturnType<typeof vi.spyOn>;

  function create(id?: string) {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: { userId: signal(7), isAdmin: signal(false) } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fixture = TestBed.createComponent(TemplateEditor);
    if (id) fixture.componentRef.setInput('id', id);
    fixture.componentInstance.ngOnInit();
    return fixture.componentInstance as unknown as {
      add(e: Exercise): void;
      move(i: number, offset: -1 | 1): void;
      submit(): void;
      form: TemplateEditor['form'];
      error(): string | null;
      hasUnsavedChanges(): boolean;
    };
  }

  afterEach(() => http.verify());

  it('submit_withoutExercises_shouldNotCallBackend', () => {
    const editor = create();
    editor.form.controls.name.setValue('Prazan');

    editor.submit();

    expect(editor.error()).toBe('Dodaj bar jednu vežbu.');
    http.expectNone('/api/templates');
  });

  it('submit_newTemplate_shouldPostExercisesInChosenOrder', () => {
    const editor = create();
    editor.form.controls.name.setValue('  Gornji deo  ');
    editor.add(bench);
    editor.add(row);
    editor.move(1, -1);
    editor.form.controls.exercises.at(1).controls.weight.setValue(60);

    editor.submit();

    const request = http.expectOne('/api/templates');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({
      name: 'Gornji deo',
      description: null,
      exercises: [
        { exerciseId: 2, setCount: 3, reps: 10, weight: null },
        { exerciseId: 1, setCount: 3, reps: 10, weight: 60 },
      ],
    });
    request.flush({ id: 42 });
    expect(navigate).toHaveBeenCalledWith(['/templates', 42]);
    expect(editor.hasUnsavedChanges()).toBe(false);
  });

  it('existingTemplate_shouldLoadAndSaveWithPut', () => {
    const editor = create('5');
    const template: Template = {
      id: 5,
      name: 'Push',
      description: null,
      createdBy: 7,
      system: false,
      exercises: [
        { id: 9, exerciseId: 1, exerciseName: 'Bench Press', muscleGroup: 'CHEST', setCount: 4, reps: 8, weight: 70, position: 1 },
      ],
    };
    http.expectOne('/api/templates/5').flush(template);

    expect(editor.hasUnsavedChanges()).toBe(false);
    editor.form.controls.exercises.at(0).controls.setCount.setValue(5);
    editor.submit();

    const request = http.expectOne('/api/templates/5');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body.exercises).toEqual([{ exerciseId: 1, setCount: 5, reps: 8, weight: 70 }]);
    request.flush(template);
  });

  it('someoneElsesTemplate_shouldRedirectToDetail', () => {
    create('6');
    http.expectOne('/api/templates/6').flush({ id: 6, name: 'Tuđi', description: null, videoUrl: null, createdBy: null, system: true, exercises: [] });

    expect(navigate).toHaveBeenCalledWith(['/templates', 6]);
  });
});
