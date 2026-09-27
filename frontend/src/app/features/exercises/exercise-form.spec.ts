import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { Exercise } from '../../core/exercise/exercise.model';
import { ImageResizer } from '../../shared/image/image-resizer';
import { ExerciseForm } from './exercise-form';

const saved = (imageUrl: string | null = null): Exercise => ({
  id: 5,
  name: 'Kosi potisak',
  muscleGroup: 'CHEST',
  imageUrl,
  description: null,
  videoUrl: null,
  createdBy: 7,
  system: false,
});

describe('ExerciseForm', () => {
  let fixture: ComponentFixture<ExerciseForm>;
  let http: HttpTestingController;
  let resize: ReturnType<typeof vi.fn>;
  let form: {
    form: ExerciseForm['form'];
    submit(): void;
    pickImage(input: HTMLInputElement): Promise<void>;
    removeImage(): void;
    error(): string | null;
    imageError(): string | null;
  };

  function create(exercise: Exercise | null = null) {
    resize = vi.fn().mockResolvedValue(new Blob(['jpeg'], { type: 'image/jpeg' }));
    URL.createObjectURL = vi.fn(() => 'blob:preview');
    URL.revokeObjectURL = vi.fn();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), { provide: ImageResizer, useValue: { resize } }],
    });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ExerciseForm);
    fixture.componentRef.setInput('exercise', exercise);
    fixture.componentInstance.ngOnInit();
    form = fixture.componentInstance as unknown as typeof form;
    form.form.patchValue({ name: 'Kosi potisak', muscleGroup: 'CHEST' });
  }

  const fileInput = (name = 'foto.png', type = 'image/png') =>
    ({ files: [new File(['x'], name, { type })], value: 'C:\\fakepath\\' + name }) as unknown as HTMLInputElement;

  afterEach(() => http.verify());

  it('newExerciseWithImage_shouldCreateThenUploadResizedImage', async () => {
    create();
    const emitted = vi.fn();
    fixture.componentInstance.saved.subscribe(emitted);

    await form.pickImage(fileInput());
    form.submit();

    const createRequest = http.expectOne('/api/exercises');
    expect(createRequest.request.body).not.toHaveProperty('imageUrl');
    createRequest.flush(saved());

    const upload = http.expectOne('/api/exercises/5/image');
    expect(upload.request.method).toBe('PUT');
    expect((upload.request.body as FormData).get('file')).toBeInstanceOf(Blob);
    upload.flush(saved('/api/images/abc'));

    expect(resize).toHaveBeenCalledOnce();
    expect(emitted).toHaveBeenCalledWith(expect.objectContaining({ imageUrl: '/api/images/abc' }));
  });

  it('removeImage_onExistingExercise_shouldDeleteItAfterSaving', () => {
    create(saved('/api/images/old'));

    form.removeImage();
    form.submit();

    http.expectOne({ method: 'PUT', url: '/api/exercises/5' }).flush(saved('/api/images/old'));
    http.expectOne({ method: 'DELETE', url: '/api/exercises/5/image' }).flush(null);
  });

  it('failedUploadAfterCreate_shouldRetryAsUpdateNotDuplicate', async () => {
    create();
    await form.pickImage(fileInput());

    form.submit();
    http.expectOne({ method: 'POST', url: '/api/exercises' }).flush(saved());
    http.expectOne('/api/exercises/5/image').flush({ message: 'Slika može imati najviše 2 MB' }, { status: 413, statusText: 'Too Large' });
    expect(form.error()).toBe('Vežba je sačuvana, ali slika nije: Slika može imati najviše 2 MB');

    form.submit();
    http.expectOne({ method: 'PUT', url: '/api/exercises/5' }).flush(saved());
    http.expectOne('/api/exercises/5/image').flush(saved('/api/images/new'));
  });

  it('unsupportedFile_shouldShowMessageWithoutRequest', async () => {
    create();
    resize.mockRejectedValue(new Error('Izaberi JPEG, PNG ili WebP sliku.'));

    await form.pickImage(fileInput('dokument.pdf', 'application/pdf'));

    expect(form.imageError()).toBe('Izaberi JPEG, PNG ili WebP sliku.');
  });
});
