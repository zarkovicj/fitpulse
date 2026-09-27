import { httpResource } from '@angular/common/http';
import { ChangeDetectionStrategy, Component, computed, inject, signal } from '@angular/core';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { debounceTime } from 'rxjs';

import { AdminUsersApi } from '../../core/admin/admin-users-api';
import { AdminUser } from '../../core/admin/admin-users.model';
import { errorMessage } from '../../core/api/api-error';
import { Page } from '../../core/workout/workout.model';
import { Dialog } from '../../shared/dialog/dialog';
import { Icon } from '../../shared/icon/icon';
import { formatDate } from '../../shared/text/format';

const PAGE_SIZE = 20;

/** Nalozi korisnika: pregled, blokiranje i brisanje. Treninge i merenja korisnika admin ne vidi. */
@Component({
  selector: 'app-admin-users',
  imports: [Icon, Dialog],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './admin-users.html',
})
export class AdminUsers {
  private readonly api = inject(AdminUsersApi);

  protected readonly search = signal('');
  // pretraga ide na server tek kad korisnik zastane sa kucanjem
  private readonly query = toSignal(toObservable(this.search).pipe(debounceTime(300)), { initialValue: '' });
  protected readonly page = signal(0);

  protected readonly users = httpResource<Page<AdminUser>>(() => ({
    url: '/api/admin/users',
    params: { search: this.query().trim(), page: this.page(), size: PAGE_SIZE },
  }));
  protected readonly result = computed(() => (this.users.hasValue() ? this.users.value() : null));

  protected readonly busyId = signal<number | null>(null);
  protected readonly error = signal<string | null>(null);

  // brisanje je trajno, pa se traži da admin ukuca mail korisnika
  protected readonly deleting = signal<AdminUser | null>(null);
  protected readonly confirmText = signal('');
  protected readonly deleteBusy = signal(false);
  protected readonly deleteError = signal<string | null>(null);
  protected readonly canConfirmDelete = computed(
    () => this.confirmText().trim().toLowerCase() === this.deleting()?.email.toLowerCase(),
  );

  protected readonly formatDate = formatDate;

  protected onSearch(value: string): void {
    this.search.set(value);
    this.page.set(0);
  }

  protected toggleActive(user: AdminUser): void {
    this.busyId.set(user.id);
    this.error.set(null);
    this.api.setActive(user.id, !user.active).subscribe({
      next: (updated) => {
        this.busyId.set(null);
        this.replace(updated);
      },
      error: (err) => {
        this.busyId.set(null);
        this.error.set(errorMessage(err));
      },
    });
  }

  protected askDelete(user: AdminUser): void {
    this.confirmText.set('');
    this.deleteError.set(null);
    this.deleting.set(user);
  }

  protected confirmDelete(): void {
    const user = this.deleting();
    if (!user || !this.canConfirmDelete()) return;

    this.deleteBusy.set(true);
    this.api.delete(user.id).subscribe({
      next: () => {
        this.deleteBusy.set(false);
        this.deleting.set(null);
        this.users.reload();
      },
      error: (err) => {
        this.deleteBusy.set(false);
        this.deleteError.set(errorMessage(err));
      },
    });
  }

  private replace(updated: AdminUser): void {
    const current = this.result();
    if (!current) return;
    this.users.set({ ...current, content: current.content.map((u) => (u.id === updated.id ? updated : u)) });
  }
}
