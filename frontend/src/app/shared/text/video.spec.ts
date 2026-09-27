import { youtubeId } from './video';

describe('youtubeId', () => {
  it.each([
    ['https://www.youtube.com/watch?v=rT7DgCr-3pg', 'rT7DgCr-3pg'],
    ['https://youtube.com/watch?v=rT7DgCr-3pg&t=42s', 'rT7DgCr-3pg'],
    ['https://youtu.be/rT7DgCr-3pg?si=abc', 'rT7DgCr-3pg'],
    ['https://m.youtube.com/shorts/rT7DgCr-3pg', 'rT7DgCr-3pg'],
    ['https://www.youtube.com/embed/rT7DgCr-3pg', 'rT7DgCr-3pg'],
  ])('%s → %s', (link, id) => {
    expect(youtubeId(link)).toBe(id);
  });

  it.each([
    'https://vimeo.com/123456',
    'https://www.youtube.com/watch?v=kratak',
    'https://evil.example/watch?v=rT7DgCr-3pg',
    'nije link',
    null,
  ])('ne prepoznaje %s', (link) => {
    expect(youtubeId(link)).toBeNull();
  });
});
