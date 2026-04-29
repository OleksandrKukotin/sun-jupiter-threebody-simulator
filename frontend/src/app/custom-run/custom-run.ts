import { Component, inject, input, output, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ApiService } from '../api/api.service';
import { OrbitPreset, SimulationRequest, TrajectoryResult } from '../api/models';

@Component({
  selector: 'app-custom-run',
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <section class="custom-run">
      <h2>Custom Run</h2>
      <p class="hint">
        Set an initial state in the rotating synodic frame (normalized units: distance = Sun–Jupiter
        separation, time = Jupiter period / 2π). Sun sits at (−μ, 0), Jupiter at (1−μ, 0).
        Click a preset and use <em>Load from preset</em> to start from a known orbit and perturb it.
      </p>

      <form [formGroup]="form" (ngSubmit)="submit()">
        <div class="grid">
          <label>x <input type="number" step="any" formControlName="x"></label>
          <label>y <input type="number" step="any" formControlName="y"></label>
          <label>ẋ <input type="number" step="any" formControlName="xDot"></label>
          <label>ẏ <input type="number" step="any" formControlName="yDot"></label>
          <label class="full">Duration <input type="number" step="any" formControlName="duration"></label>
        </div>

        <details class="advanced">
          <summary>Advanced (tolerances)</summary>
          <div class="grid">
            <label>abs tol <input type="number" step="any" formControlName="absoluteTolerance"></label>
            <label>rel tol <input type="number" step="any" formControlName="relativeTolerance"></label>
            <label>min step <input type="number" step="any" formControlName="minStep"></label>
            <label>max step <input type="number" step="any" formControlName="maxStep"></label>
          </div>
        </details>

        <div class="actions">
          <button type="button" (click)="loadFromPreset()" [disabled]="!preset()">Load from preset</button>
          <button type="submit" [disabled]="form.invalid || loading()">
            {{ loading() ? 'Running…' : 'Run' }}
          </button>
        </div>

        @if (error()) {
          <p class="error">{{ error() }}</p>
        }
      </form>
    </section>
  `,
  styles: [`
    .custom-run { padding: 1rem; border-top: 1px solid #e5e5e5; font-family: system-ui, sans-serif; }
    .custom-run h2 { margin: 0 0 0.5rem; font-size: 1.25rem; }
    .hint { margin: 0 0 0.85rem; font-size: 0.8rem; color: #555; line-height: 1.4; }
    .hint em { font-style: italic; color: #333; }
    .grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0.5rem 0.75rem; }
    label { display: flex; flex-direction: column; font-size: 0.8rem; color: #555; gap: 0.25rem; }
    label.full { grid-column: 1 / -1; }
    input { padding: 0.35rem 0.5rem; border: 1px solid #ccc; border-radius: 4px; font-family: monospace; font-size: 0.9rem; }
    input:focus { outline: none; border-color: #3b82f6; }
    .advanced { margin: 0.75rem 0; font-size: 0.85rem; }
    .advanced summary { cursor: pointer; color: #555; }
    .advanced .grid { margin-top: 0.5rem; }
    .actions { display: flex; gap: 0.5rem; margin-top: 0.75rem; }
    button { padding: 0.4rem 0.85rem; border: 1px solid #ccc; border-radius: 4px; background: #fff; cursor: pointer; font-size: 0.85rem; }
    button[type="submit"] { background: #2563eb; color: #fff; border-color: #2563eb; }
    button:disabled { opacity: 0.5; cursor: not-allowed; }
    .error { color: #b91c1c; font-size: 0.85rem; margin: 0.5rem 0 0; }
  `]
})
export class CustomRun {
  private readonly api = inject(ApiService);
  private readonly fb = inject(FormBuilder);

  readonly preset = input<OrbitPreset | null>(null);
  readonly customRun = output<TrajectoryResult>();

  protected readonly loading = signal(false);
  protected readonly error = signal<string | null>(null);

  protected readonly form = this.fb.nonNullable.group({
    x: [0.4877, Validators.required],
    y: [0.866, Validators.required],
    xDot: [0, Validators.required],
    yDot: [0, Validators.required],
    duration: [100, [Validators.required, Validators.min(0.0001)]],
    absoluteTolerance: [1e-10, Validators.required],
    relativeTolerance: [1e-10, Validators.required],
    minStep: [1e-6, Validators.required],
    maxStep: [1.0, Validators.required],
  });

  loadFromPreset(): void {
    const p = this.preset();
    if (!p) return;
    this.form.patchValue({
      x: p.initialState.x,
      y: p.initialState.y,
      xDot: p.initialState.xDot,
      yDot: p.initialState.yDot,
      duration: p.duration,
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    const v = this.form.getRawValue();
    const request: SimulationRequest = {
      initialState: { x: v.x, y: v.y, xDot: v.xDot, yDot: v.yDot },
      duration: v.duration,
      absoluteTolerance: v.absoluteTolerance,
      relativeTolerance: v.relativeTolerance,
      minStep: v.minStep,
      maxStep: v.maxStep,
    };

    this.loading.set(true);
    this.error.set(null);
    this.api.propagate(request).subscribe({
      next: (result) => {
        this.loading.set(false);
        this.customRun.emit(result);
      },
      error: (e) => {
        this.loading.set(false);
        this.error.set(`Run failed: ${e.message ?? e}`);
      }
    });
  }
}