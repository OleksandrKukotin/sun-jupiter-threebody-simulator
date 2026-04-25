export interface StateVector {
  x: number;
  y: number;
  xDot: number;
  yDot: number;
}

export interface SimulationRequest {
  initialState: StateVector;
  duration: number;
  absoluteTolerance: number;
  relativeTolerance: number;
  minStep: number;
  maxStep: number;
}

export interface TrajectoryPoint {
  time: number;
  state: StateVector;
  jacobiConstant: number;
}

export interface TrajectoryResult {
  points: TrajectoryPoint[];
  initialJacobiConstant: number;
  finalJacobiConstant: number;
}

export interface LagrangePoint {
  name: string;
  x: number;
  y: number;
}

export interface OrbitPreset {
  id: string;
  name: string;
  description: string;
  initialState: StateVector;
  duration: number;
  expectedJacobiConstant: number;
}

export interface ZeroVelocityGrid {
  xMin: number;
  xMax: number;
  yMin: number;
  yMax: number;
  forbidden: boolean[][];
}
