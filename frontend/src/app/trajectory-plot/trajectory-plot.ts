import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  effect,
  input,
  viewChild
} from '@angular/core';
import {DecimalPipe} from '@angular/common';
import Plotly from 'plotly.js-dist-min';
import {TrajectoryResult} from '../api/models';

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
  private readonly plotEl = viewChild.required<ElementRef<HTMLDivElement>>('plot');
  private initialized = false;

  constructor() {
    effect(() => {
      const r = this.result();
      if (this.initialized && r) this.render(r);
    });
  }

  ngAfterViewInit(): void {
    this.initialized = true;
    this.render(this.result());
  }

  ngOnDestroy(): void {
    if (this.initialized) Plotly.purge(this.plotEl().nativeElement);
  }

  private render(result: TrajectoryResult): void {
    const xs = result.points.map(p => p.state.x);
    const ys = result.points.map(p => p.state.y);

    const data: Plotly.Data[] = [
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
    ];

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

    Plotly.react(this.plotEl().nativeElement, data, layout, {responsive: true});
  }
}
