const REMEMBERED_MAIL_KEY = 'fitpulse.remembered-mail';

// PasswordCredential postoji samo u Chromium browserima i nije deo standardnih TS tipova
declare const PasswordCredential: { new (data: { id: string; password: string; name?: string }): Credential } | undefined;

/**
 * Posle uspešne prijave nudi browser-u da sačuva lozinku u svom password manageru.
 */
export async function offerToSavePassword(email: string, password: string, name?: string): Promise<void> {
  if (typeof PasswordCredential === 'undefined' || !navigator.credentials) return;
  try {
    await navigator.credentials.store(new PasswordCredential({ id: email, password, name }));
  } catch {
    // korisnik je odbio ili browser ne dozvoljava; prijava je svejedno uspela
  }
}

/** Mail koji je korisnik tražio da se zapamti (samo mail, nikad lozinka). */
export function rememberedMail(): string {
  try {
    return localStorage.getItem(REMEMBERED_MAIL_KEY) ?? '';
  } catch {
    return '';
  }
}

export function rememberMail(email: string | null): void {
  try {
    if (email) localStorage.setItem(REMEMBERED_MAIL_KEY, email);
    else localStorage.removeItem(REMEMBERED_MAIL_KEY);
  } catch {}
}
