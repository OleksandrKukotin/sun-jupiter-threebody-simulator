import {Component, signal} from '@angular/core';
import {PresetList} from './preset-list/preset-list';
import {TrajectoryPlot} from './trajectory-plot/trajectory-plot';
import {OrbitPreset, TrajectoryResult} from './api/models';
import {DecimalPipe} from '@angular/common';

@Component({
  selector: 'app-root',
  imports: [PresetList, TrajectoryPlot, DecimalPipe],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {
  protected readonly selectedPreset = signal<OrbitPreset | null>(null);
  protected readonly trajectoryResult = signal<TrajectoryResult | null>(null);

  onPresetRun(event: { preset: OrbitPreset; result: TrajectoryResult }): void {
    this.selectedPreset.set(event.preset);
    this.trajectoryResult.set(event.result);
  }
}
