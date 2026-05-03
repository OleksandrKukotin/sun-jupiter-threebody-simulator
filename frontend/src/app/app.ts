import {Component, inject, signal, viewChild} from '@angular/core';
import {ExportControls} from './export/export-controls';
import {PresetList} from './preset-list/preset-list';
import {TrajectoryPlot} from './trajectory-plot/trajectory-plot';
import {CustomRun} from './custom-run/custom-run';
import {LagrangePoint, OrbitPreset, TrajectoryResult, ZeroVelocityGrid} from './api/models';
import {DecimalPipe} from '@angular/common';
import {ApiService} from './api/api.service';
import {Trajectory3d} from './trajectory-3d/trajectory-3d';

@Component({
  selector: 'app-root',
  imports: [PresetList, TrajectoryPlot, CustomRun, DecimalPipe, ExportControls, Trajectory3d],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly api = inject(ApiService);

  protected readonly selectedPreset = signal<OrbitPreset | null>(null);
  protected readonly trajectoryResult = signal<TrajectoryResult | null>(null);
  protected readonly lagrangePoints = signal<LagrangePoint[]>([]);
  protected readonly zvcGrid = signal<ZeroVelocityGrid | null>(null);

  private readonly plot = viewChild(TrajectoryPlot);
  protected readonly viewMode = signal<'2d' | '3d'>('2d');

  protected toggleView(): void {
    this.viewMode.update(m => m === '2d' ? '3d' : '2d');
  }

  ngOnInit(): void {
    this.api.getLagrangePoints().subscribe({
      next: (pts) => this.lagrangePoints.set(pts),
      error: (e) => console.error('Lagrange points fetch has failed', e)
    });
  }

  onPresetRun(event: { preset: OrbitPreset; result: TrajectoryResult }): void {
    this.selectedPreset.set(event.preset);
    this.applyResult(event.result);
  }

  onCustomRun(result: TrajectoryResult): void {
    this.selectedPreset.set(null);
    this.applyResult(result);
  }

  protected onPngRequested(): void {
    const name = this.selectedPreset()?.id ?? 'custom-run';
    this.plot()?.downloadPng(name);
  }

  protected filenameBase(): string {
    return this.selectedPreset()?.id ?? 'custom-run';
  }

  private applyResult(result: TrajectoryResult): void {
    this.trajectoryResult.set(result);
    this.zvcGrid.set(null);

    const c = result.initialJacobiConstant;
    const bounds = {xMin: -1.5, xMax: 1.5, yMin: -1.5, yMax: 1.5};
    const resolution = 200;
    this.api.getZeroVelocity(c, {...bounds, resolution}).subscribe({
      next: (forbidden) => this.zvcGrid.set({...bounds, forbidden}),
      error: (e) => console.error('ZVC fetch has failed', e)
    });
  }
}
