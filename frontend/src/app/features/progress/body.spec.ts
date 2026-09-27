import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { CurrentUser } from '../../core/user/current-user';
import { localDate } from '../../shared/text/format';
import { Body } from './body';

describe('Body', () => {
  let http: HttpTestingController;
  let reloadUser: ReturnType<typeof vi.fn>;
  let body: {
    weightForm: Body['weightForm'];
    saveWeight(): void;
    weightError(): string | null;
    toGoal(): string | null;
  };

  beforeEach(() => {
    reloadUser = vi.fn();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: CurrentUser, useValue: { user: signal({ weight: 80.9 }), reload: reloadUser } },
      ],
    });
    http = TestBed.inject(HttpTestingController);
    body = TestBed.createComponent(Body).componentInstance as unknown as typeof body;
    TestBed.tick();
    http.expectOne((r) => r.url === '/api/progress/weight').flush([]);
    http.expectOne('/api/progress/goal').flush({ weight: 78, bodyFatPercent: null });
    TestBed.tick();
  });

  it('toGoal_shouldShowRemainingDifference', () => {
    expect(body.toGoal()).toBe('Još 2,9 kg do cilja.');
  });

  it('saveWeight_shouldLogAndRefreshHistoryAndProfile', () => {
    body.weightForm.setValue({ date: localDate(), weight: 80.4 });
    body.saveWeight();

    const request = http.expectOne(`/api/progress/weight/${localDate()}`);
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ weight: 80.4 });
    request.flush({ date: localDate(), weight: 80.4 });
    TestBed.tick();

    // istorija se ponovo učitava, a profil osvežava jer masa prati poslednje merenje
    http.expectOne((r) => r.url === '/api/progress/weight');
    expect(reloadUser).toHaveBeenCalled();
  });

  it('saveWeight_inFuture_shouldNotCallBackend', () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    body.weightForm.setValue({ date: localDate(tomorrow), weight: 80 });

    body.saveWeight();

    expect(body.weightError()).toBe('Merenje ne može biti u budućnosti.');
    http.expectNone((r) => r.url.startsWith('/api/progress/weight/'));
  });
});
