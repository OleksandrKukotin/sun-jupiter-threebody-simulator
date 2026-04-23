import { Component, OnInit, inject, output, signal } from '@angular/core';
import { ApiService } from '../api/api.service';
import { OrbitPreset } from '../api/models';
import {DecimalPipe} from '@angular/common';

@Component({
  selector: 'app-preset-list',
  standalone: true,
  templateUrl: './preset-list.html',
  imports: [
    DecimalPipe
  ],
  styleUrl: './preset-list.css'
})
export class PresetList implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly presets = signal<OrbitPreset[]>([]);
  protected readonly selectedId = signal<string | null>(null);
  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  readonly presetRun = output<{ preset: OrbitPreset; result: import('../api/models').TrajectoryResult }>();

  ngOnInit(): void {
    this.api.listPresets().subscribe({
      next: (p) => this.presets.set(p),
      error: (e) => this.error.set(`Failed to load presets: ${e.message ?? e}`)
    });
  }

  select(preset: OrbitPreset): void {
    this.selectedId.set(preset.id);
    this.loading.set(true);
    this.error.set(null);
    this.api.runPreset(preset.id).subscribe({
      next: (result) => {
        this.loading.set(false);
        this.presetRun.emit({ preset, result });
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(`Run failed: ${e.message ?? e}`);
      }
    });
  }
}
