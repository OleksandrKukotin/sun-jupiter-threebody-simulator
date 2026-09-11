import { Component, effect, inject, output, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../api/api.service';
import { PeriodicOrbitResult } from '../api/models';

@Component({
  selector: 'app-periodic-orbit-panel',
  standalone: true,
  imports: [ReactiveFormsModule, DecimalPipe],
  template: `
    <section class="periodic-orbit-panel">
      <h2>Periodic Orbits</h2>
      <p class="hint">
        Find a planar Lyapunov orbit around a collinear Lagrange point via differential
        correction, then overlay it on the trajectory plot.
      </p>

      <form [formGroup]="form" (ngSubmit)="find()">
        <div class="grid">
          <label>Point
            <select formControlName="point">
              <option value="L1">L1</option>
              <option value="L2">L2</option>
              <option value="L3">L3</option>
            </select>
          </label>
          <label>Family
            <select formControlName="family">
              <option value="lyapunov">Lyapunov</option>
            </select>
          </label>
          <label class="full">Amplitude (Ax)
            <input type="number" step="any" formControlName="amplitude">
          </label>
        </div>

        <div class="actions">
          <button type="submit" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Finding…' : 'Find Orbit' }}
          </button>
          @if (orbit()) {
            <label class="overlay-toggle">
              <input type="checkbox" [checked]="showOverlay()" (change)="toggleOverlay()">
              Show overlay
            </label>
          }
        </div>

        @if (error()) {
          <p class="error">{{ error() }}</p>
        }

        @if (orbit(); as o) {
          <p class="result">
            T = {{ o.orbit.period | number:'1.4-4' }} ·
            C = {{ o.orbit.jacobiConstant | number:'1.6-6' }}
          </p>
        }
      </form>
    </section>
  `,
  styles: [`
    .periodic-orbit-panel { padding: 1rem; border-top: 1px solid #e5e5e5; font-family: system-ui, sans-serif; }
    .periodic-orbit-panel h2 { margin: 0 0 0.5rem; font-size: 1.25rem; }
    .hint { margin: 0 0 0.85rem; font-size: 0.8rem; color: #555; line-height: 1.4; }
    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem 0.75rem; }
    label { display: flex; flex-direction: column; font-size: 0.8rem; color: #555; gap: 0.25rem; }
    label.full { grid-column: 1 / -1; }
    input, select { padding: 0.35rem 0.5rem; border: 1px solid #ccc; border-radius: 4px; font-family: monospace; font-size: 0.9rem; }
    input:focus, select:focus { outline: none; border-color: #3b82f6; }
    .actions { display: flex; align-items: center; gap: 0.75rem; margin-top: 0.75rem; }
    .overlay-toggle { flex-direction: row; align-items: center; font-family: system-ui, sans-serif; }
    .overlay-toggle input { width: auto; }
    button { padding: 0.4rem 0.85rem; border: 1px solid #ccc; border-radius: 4px; background: #fff; cursor: pointer; font-size: 0.85rem; }
    button[type="submit"] { background: #2563eb; color: #fff; border-color: #2563eb; }
    button:disabled { opacity: 0.5; cursor: not-allowed; }
    .error { color: #b91c1c; font-size: 0.85rem; margin: 0.5rem 0 0; }
    .result { color: #555; font-size: 0.8rem; margin: 0.5rem 0 0; font-family: monospace; }
  `]
})
export class PeriodicOrbitPanel {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly overlayChange = output<PeriodicOrbitResult | null>();

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);
  protected readonly orbit = signal<PeriodicOrbitResult | null>(null);
  protected readonly showOverlay = signal(true);

  protected readonly form = this.fb.nonNullable.group({
    point: ['L1', Validators.required],
    family: ['lyapunov', Validators.required],
    amplitude: [1e-3, [Validators.required, Validators.min(1e-6)]],
  });

  constructor() {
    effect(() => {
      this.overlayChange.emit(this.showOverlay() ? this.orbit() : null);
    });
  }

  find(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();

    this.loading.set(true);
    this.error.set(null);
    this.api.getPeriodicOrbit(v.point, v.amplitude, v.family).subscribe({
      next: (result) => {
        this.loading.set(false);
        this.orbit.set(result);
        this.showOverlay.set(true);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(`Search failed: ${e.message ?? e}`);
      }
    });
  }

  toggleOverlay(): void {
    this.showOverlay.update(v => !v);
  }
}
