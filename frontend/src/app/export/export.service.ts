import { Injectable } from '@angular/core';
import { TrajectoryResult } from '../api/models';

export type ExportFormat = 'json' | 'csv' | 'png';

@Injectable({ providedIn: 'root' })
export class ExportService {

  exportJson(result: TrajectoryResult, filename = 'trajectory.json'): void {
    const blob = new Blob([JSON.stringify(result, null, 2)], { type: 'application/json' });
    this.download(blob, filename);
  }

  exportCsv(result: TrajectoryResult, filename = 'trajectory.csv'): void {
    const header = 'time,x,y,xDot,yDot,jacobiConstant\n';
    const rows = result.points.map(p =>
      `${p.time},${p.state.x},${p.state.y},${p.state.xDot},${p.state.yDot},${p.jacobiConstant}`
    ).join('\n');
    const blob = new Blob([header + rows + '\n'], { type: 'text/csv' });
    this.download(blob, filename);
  }

  // PNG export is delegated to Plotly's toImage from the trajectory-plot component,
  // since it owns the Plotly graph div. This stub exists so call sites can route
  // all export intents through one service.
  exportPng(_result: TrajectoryResult, _filename = 'trajectory.png'): void {
    throw new Error('PNG export must be invoked from the trajectory-plot component (uses Plotly.toImage).');
  }

  private download(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  }
}
