import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  effect,
  input,
  viewChild
} from '@angular/core';
import Plotly from 'plotly.js-cartesian-dist-min';
import {LagrangePoint, TrajectoryResult, ZeroVelocityGrid} from '../api/models';

const MU = 9.5368e-4;

@Component({
  selector: 'app-trajectory-plot',
  standalone: true,
  template: `
    <div #plot class="plot-host"></div>`,
  styles: [`
    :host {
      display: block;
      flex: 1;
      min-height: 0;
    }

    .plot-host {
      width: 100%;
      height: 100%;
    }
  `]
})
export class TrajectoryPlot implements AfterViewInit, OnDestroy {
  readonly result = input.required<TrajectoryResult>();
  readonly lagrangePoints = input<LagrangePoint[]>([]);
  readonly zvcGrid = input<ZeroVelocityGrid | null>(null);
  private readonly plotEl = viewChild.required<ElementRef<HTMLDivElement>>('plot');
  private initialized = false;

  constructor() {
    effect(() => {
      const r = this.result();
      const pts = this.lagrangePoints();
      const zvc = this.zvcGrid();
      if (this.initialized && r) this.render(r, pts, zvc);
    });
  }

  ngAfterViewInit(): void {
    this.initialized = true;
    this.render(this.result(), this.lagrangePoints(), this.zvcGrid());
  }

  ngOnDestroy(): void {
    if (this.initialized) Plotly.purge(this.plotEl().nativeElement);
  }

  downloadPng(filename = 'trajectory'): void {
    if (!this.initialized) return;
    Plotly.downloadImage(this.plotEl().nativeElement, {
      format: 'png',
      filename,
      width: 1200,
      height: 900
    });
  }

  private render(result: TrajectoryResult, lagrangePoints: LagrangePoint[], zvc: ZeroVelocityGrid | null): void {
    const data: Plotly.Data[] = [];

    if (zvc && zvc.forbidden.length > 1 && zvc.forbidden[0].length > 1) {
      const {xMin, xMax, yMin, yMax, forbidden} = zvc;
      const nx = forbidden.length;
      const ny = forbidden[0].length;

      // Backend grid is [xIndex][yIndex]; Plotly heatmap expects z[rowY][colX]
      const z: number[][] = Array.from({length: ny}, (_, j) =>
        Array.from({length: nx}, (_, i) => (forbidden[i][j] ? 1 : 0))
      );
      const heatmapXs = Array.from({length: nx}, (_, i) => xMin + (i * (xMax - xMin)) / (nx - 1));
      const heatmapYs = Array.from({length: ny}, (_, j) => yMin + (j * (yMax - yMin)) / (ny - 1));

      data.push({
        type: 'heatmap',
        x: heatmapXs,
        y: heatmapYs,
        z,
        showscale: false,
        hoverinfo: 'skip',
        colorscale: [
          [0, 'rgba(0,0,0,0)'],
          [1, 'rgba(120,120,120,0.35)']
        ],
        zmin: 0,
        zmax: 1,
        name: 'forbidden region'
      } as Plotly.Data);
    }

    const xs = result.points.map(p => p.state.x);
    const ys = result.points.map(p => p.state.y);

    data.push(
      {
        type: 'scatter',
        mode: 'lines',
        x: xs,
        y: ys,
        name: 'trajectory',
        line: {color: '#2563eb', width: 1.5}
      },
      {
        type: 'scatter',
        mode: 'markers',
        x: [-MU],
        y: [0],
        name: 'Sun',
        marker: {color: '#f59e0b', size: 12, symbol: 'circle'}
      },
      {
        type: 'scatter',
        mode: 'markers',
        x: [1 - MU],
        y: [0],
        name: 'Jupiter',
        marker: {color: '#b45309', size: 8, symbol: 'circle'}
      }
    );

    const layout: Partial<Plotly.Layout> = {
      xaxis: {title: {text: 'x (normalized)'}, zeroline: true},
      yaxis: {
        title: {text: 'y (normalized)'},
        scaleanchor: 'x',
        scaleratio: 1,
        zeroline: true
      },
      margin: {l: 50, r: 20, t: 20, b: 40},
      showlegend: true,
      hovermode: 'closest'
    };

    if (lagrangePoints.length > 0) {
      data.push({
        type: 'scatter',
        mode: 'text+markers',
        x: lagrangePoints.map(p => p.x),
        y: lagrangePoints.map(p => p.y),
        text: lagrangePoints.map(p => p.name),
        textposition: 'top right',
        marker: {color: '#10b981', size: 8, symbol: 'x'},
        textfont: {size: 11, color: '#065f46'}
      })
    }

    Plotly.react(this.plotEl().nativeElement, data, layout, {responsive: true});
  }
}
