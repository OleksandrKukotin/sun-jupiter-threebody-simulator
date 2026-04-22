import {HttpClient, HttpParams} from '@angular/common/http';
import {Injectable, inject} from '@angular/core';
import {Observable} from 'rxjs';
import {
  LagrangePoint,
  OrbitPreset,
  SimulationRequest,
  TrajectoryResult
} from './models';

@Injectable({providedIn: 'root'})
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  propagate(request: SimulationRequest): Observable<TrajectoryResult> {
    return this.http.post<TrajectoryResult>(`${this.base}/simulation/propagate`, request);
  }

  getLagrangePoints(): Observable<LagrangePoint[]> {
    return this.http.get<LagrangePoint[]>(`${this.base}/simulation/lagrange-points`);
  }

  getZeroVelocity(
    jacobiConstant: number,
    opts: { xMin?: number; xMax?: number; yMin?: number; yMax?: number; resolution?: number } = {},
  ): Observable<boolean[][]> {
    let params = new HttpParams().set('jacobiConstant', jacobiConstant);
    for (const [k, v] of Object.entries(opts)) {
      if (v !== undefined) params = params.set(k, v);
    }
    return this.http.get<boolean[][]>(`${this.base}/simulation/zero-velocity-curve`, {params});
  }

  listPresets(): Observable<OrbitPreset[]> {
    return this.http.get<OrbitPreset[]>(`${this.base}/presets`);
  }

  runPreset(id: string): Observable<TrajectoryResult> {
    return this.http.post<TrajectoryResult>(`${this.base}/presets/${id}/run`, {});
  }
}
