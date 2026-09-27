/** Nepotpisan JWT sa zadatim `exp`, dovoljan za klijentsku logiku. */
export function fakeJwt(expiresInSeconds: number): string {
  const exp = Math.floor(Date.now() / 1000) + expiresInSeconds;
  const payload = btoa(JSON.stringify({ sub: 'pera@example.com', exp }))
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/, '');
  return `eyJhbGciOiJIUzI1NiJ9.${payload}.signature`;
}
