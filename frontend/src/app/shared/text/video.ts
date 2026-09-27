const YOUTUBE_ID = /^[A-Za-z0-9_-]{11}$/;

/**
 * ID YouTube videa iz uobičajenih oblika linka:
 * youtube.com/watch?v=ID, youtu.be/ID, youtube.com/shorts/ID, youtube.com/embed/ID.
 */
export function youtubeId(link: string | null | undefined): string | null {
  if (!link) return null;
  let url: URL;
  try {
    url = new URL(link);
  } catch {
    return null;
  }
  const host = url.hostname.replace(/^(www\.|m\.)/, '');
  let id: string | null = null;
  if (host === 'youtu.be') {
    id = url.pathname.slice(1);
  } else if (host === 'youtube.com' || host === 'youtube-nocookie.com') {
    const [, kind, value] = url.pathname.split('/');
    id = kind === 'watch' ? url.searchParams.get('v') : ['shorts', 'embed', 'live'].includes(kind) ? value : null;
  }
  // samo ispravan ID ide u adresu iframe-a
  return id && YOUTUBE_ID.test(id) ? id : null;
}

/** Ugrađeni plejer bez kolačića za praćenje. */
export function youtubeEmbedUrl(id: string): string {
  return `https://www.youtube-nocookie.com/embed/${id}?rel=0`;
}
