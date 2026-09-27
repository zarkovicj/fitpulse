/** Za pretragu bez obzira na kvačice: "cucanj" pronalazi "Čučanj". */
export function normalize(text: string): string {
  return text
    .toLowerCase()
    .replace(/đ/g, 'dj')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .trim();
}
