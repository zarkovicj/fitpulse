import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { AdminUser } from '../../core/admin/admin-users.model';
import { AdminUsers } from './admin-users';

const user = (id: number, email: string, extra: Partial<AdminUser> = {}): AdminUser => ({
  id,
  firstName: 'Pera',
  lastName: 'Perić',
  email,
  role: 'USER',
  active: true,
  createdAt: '2026-09-01T10:00:00Z',
  workoutCount: 4,
  ...extra,
});

describe('AdminUsers', () => {
  let fixture: ComponentFixture<AdminUsers>;
  let http: HttpTestingController;
  let root: HTMLElement;

  beforeEach(async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AdminUsers);
    TestBed.tick();
    http.expectOne((r) => r.url === '/api/admin/users').flush({
      content: [user(1, 'admin@fitpulse.local', { role: 'ADMIN' }), user(2, 'pera@example.com')],
      page: 0,
      size: 20,
      totalElements: 2,
      totalPages: 1,
      last: true,
    });
    await fixture.whenStable();
    root = fixture.nativeElement;
  });

  const buttons = (text: string) =>
    [...root.querySelectorAll<HTMLButtonElement>('button')].filter((b) => b.textContent!.trim() === text);

  it('adminAccounts_shouldHaveNoActions', () => {
    expect(buttons('Blokiraj')).toHaveLength(1);
    expect(root.querySelectorAll('button[aria-label^="Obriši nalog"]')).toHaveLength(1);
  });

  it('block_shouldUpdateStatusInPlace', async () => {
    buttons('Blokiraj')[0].click();

    const request = http.expectOne('/api/admin/users/2/status');
    expect(request.request.body).toEqual({ active: false });
    request.flush(user(2, 'pera@example.com', { active: false }));
    await fixture.whenStable();

    expect(root.textContent).toContain('Blokiran');
    expect(buttons('Odblokiraj')).toHaveLength(1);
  });

  it('delete_shouldRequireTypingTheMail', async () => {
    root.querySelector<HTMLButtonElement>('button[aria-label="Obriši nalog: pera@example.com"]')!.click();
    await fixture.whenStable();
    const confirm = () => buttons('Obriši nalog')[0];
    const input = root.querySelector<HTMLInputElement>('#confirm-email')!;

    expect(confirm().disabled).toBe(true);
    input.value = 'pera@exam';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(confirm().disabled).toBe(true);

    input.value = 'Pera@Example.com';
    input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(confirm().disabled).toBe(false);

    confirm().click();
    http.expectOne({ method: 'DELETE', url: '/api/admin/users/2' }).flush(null);
  });
});
