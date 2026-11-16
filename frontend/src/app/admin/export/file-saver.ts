import { Injectable } from '@angular/core';

/** Hands a downloaded file to the browser; a class of its own so tests can replace it. */
@Injectable({ providedIn: 'root' })
export class FileSaver {
  save(filename: string, blob: Blob): void {
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    anchor.style.display = 'none';
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    setTimeout(() => URL.revokeObjectURL(url));
  }
}