import { Component, inject, input, output } from '@angular/core';
import { TrajectoryResult } from '../api/models';
import { ExportService } from './export.service';

@Component({
  selector: 'app-export-controls',
  standalone: true,
  template: `
    <div class="export-controls">
      <span class="label">Export:</span>
      <button type="button" (click)="onJson()" [disabled]="!result()">JSON</button>
      <button type="button" (click)="onCsv()" [disabled]="!result()">CSV</button>
      <button type="button" (click)="pngRequested.emit()" [disabled]="!result()">PNG</button>
    </div>
  `,
  styles: [`
    .export-controls { display: flex; align-items: center; gap: 0.5rem; font-family: system-ui, sans-serif; }
    .label { font-size: 0.8rem; color: #555; }
    button { padding: 0.3rem 0.7rem; border: 1px solid #ccc; border-radius: 4px; background: #fff; cursor: pointer; font-size: 0.8rem; }
    button:hover:not(:disabled) { border-color: #2563eb; color: #2563eb; }
    button:disabled { opacity: 0.5; cursor: not-allowed; }
  `]
})
export class ExportControls {
  private readonly exportService = inject(ExportService);

  readonly result = input<TrajectoryResult | null>(null);
  readonly filenameBase = input<string>('trajectory');
  readonly pngRequested = output<void>();

  onJson(): void {
    const r = this.result();
    if (r) this.exportService.exportJson(r, `${this.filenameBase()}.json`);
  }

  onCsv(): void {
    const r = this.result();
    if (r) this.exportService.exportCsv(r, `${this.filenameBase()}.csv`);
  }
}