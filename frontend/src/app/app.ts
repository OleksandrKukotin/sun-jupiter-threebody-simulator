import {Component, inject, OnInit, signal} from '@angular/core';
import {RouterOutlet} from '@angular/router';
import {ApiService} from './api/api.service';
import {LagrangePoint, OrbitPreset} from './api/models';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App implements OnInit {
  private readonly api = inject(ApiService);

  protected readonly title = signal('frontend');
  protected readonly presets = signal<OrbitPreset[]>([]);
  protected readonly lagrangePoints = signal<LagrangePoint[]>([]);

  ngOnInit() {
    this.api.listPresets().subscribe({
      next: (p) => {
        console.log('presets', p);
        this.presets.set(p);
      },
      error: (e) => console.error('presets failed', e)
    });
    this.api.getLagrangePoints().subscribe({
      next: (l) => {
        console.log('Lagrange', l);
        this.lagrangePoints.set(l)
      },
      error: (e) => console.error('Lagrange failed', e)
    });
  }
}
