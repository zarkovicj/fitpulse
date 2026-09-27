import { Injectable } from '@angular/core';

export const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
// veći fajl ni ne pokušavamo da otvorimo; posle smanjivanja slika ima oko 100-200 KB
const MAX_INPUT_BYTES = 20 * 1024 * 1024;

/**
 * Smanjuje sliku u browseru pre slanja: najduža strana do 1000 px, JPEG.
 * Ponovnim crtanjem na canvas nestaju i EXIF podaci (npr. GPS lokacija sa telefona).
 */
@Injectable({ providedIn: 'root' })
export class ImageResizer {
  async resize(file: File, maxSide = 1000): Promise<Blob> {
    if (!ACCEPTED_IMAGE_TYPES.includes(file.type)) {
      throw new Error('Izaberi JPEG, PNG ili WebP sliku.');
    }
    if (file.size > MAX_INPUT_BYTES) {
      throw new Error('Slika je prevelika (najviše 20 MB).');
    }

    let bitmap: ImageBitmap;
    try {
      bitmap = await createImageBitmap(file);
    } catch {
      throw new Error('Slika ne može da se otvori.');
    }

    const scale = Math.min(1, maxSide / Math.max(bitmap.width, bitmap.height));
    const canvas = document.createElement('canvas');
    canvas.width = Math.round(bitmap.width * scale);
    canvas.height = Math.round(bitmap.height * scale);
    const context = canvas.getContext('2d')!;
    // providni delovi PNG-a bi u JPEG-u postali crni
    context.fillStyle = '#ffffff';
    context.fillRect(0, 0, canvas.width, canvas.height);
    context.drawImage(bitmap, 0, 0, canvas.width, canvas.height);
    bitmap.close();

    return new Promise((resolve, reject) =>
      canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('Slika ne može da se obradi.'))), 'image/jpeg', 0.85),
    );
  }
}
