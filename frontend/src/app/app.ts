import {Component, inject, signal} from '@angular/core';
import {PresetList} from './preset-list/preset-list';
import {TrajectoryPlot} from './trajectory-plot/trajectory-plot';
import {CustomRun} from './custom-run/custom-run';
import {LagrangePoint, OrbitPreset, TrajectoryResult, ZeroVelocityGrid} from './api/models';
import {DecimalPipe} from '@angular/common';
import {ApiService} from './api/api.service';

@Component({
  selector: 'app-root',
  imports: [PresetList, TrajectoryPlot, CustomRun, DecimalPipe],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  private readonly api = inject(ApiService);

  protected readonly selectedPreset = signal<OrbitPreset | null>(null);
  protected readonly trajectoryResult = signal<TrajectoryResult | null>(null);
  protected readonly lagrangePoints = signal<LagrangePoint[]>([]);
  protected readonly zvcGrig = signal<ZeroVelocityGrid | null>(null);

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

  private applyResult(result: TrajectoryResult): void {
    this.trajectoryResult.set(result);
    this.zvcGrig.set(null);

    const c = result.initialJacobiConstant;
    const bounds = {xMin: -1.5, xMax: 1.5, yMin: -1.5, yMax: 1.5, resolution: 200};
    this.api.getZeroVelocity(c, bounds).subscribe({
      next: (forbidden) => this.zvcGrig.set({...bounds, forbidden}),
      error: (e) => console.error('ZVC fetch has failed', e)
    });
  }
}
