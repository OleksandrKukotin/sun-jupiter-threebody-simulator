# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**CR3BP Explorer** — a Java/Spring Boot backend simulator for the Circular Restricted Three-Body Problem (CR3BP), focused on the Sun–Jupiter system and its Lagrange points, paired with an Angular frontend (`frontend/`) that consumes its REST API.

## Build & Run Commands

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "org.github.oleksandrkukotin.physics.JacobiConstantTest"

# Run the application
./gradlew bootRun
```

## Frontend

Angular app in `frontend/`. During development it runs on `:4200` and proxies `/api/*` to Spring on `:8080` via `frontend/proxy.conf.json` (wired into `angular.json` under both the dev and prod `serve` configurations), so both `./gradlew bootRun` and `ng serve` must be running.

```bash
cd frontend
npm install
npm start       # ng serve with proxy
npm test
npm run build
```

- `src/app/api/models.ts` — TypeScript interfaces mirroring the Java records in `org.github.oleksandrkukotin.model` field-for-field (Jackson serializes records by component name)
- `src/app/api/api.service.ts` — `ApiService` wrapping every backend endpoint; all calls use the relative `/api` prefix so the proxy handles routing
- `HttpClient` is provided via `provideHttpClient()` in `app.config.ts`
- `src/app/trajectory-plot/` — Plotly-based 2D plot; renders trajectory, Sun/Jupiter, Lagrange points, and ZVC heatmap. Backend ZVC grid is `[xIndex][yIndex]`; Plotly heatmap expects `z[rowY][colX]`, so the component transposes. Exposes `downloadPng(filename)` using `Plotly.downloadImage`
- `src/app/preset-list/` — fetches `/api/presets`, runs a preset on click, emits `{preset, result}`
- `src/app/custom-run/` — reactive form for arbitrary initial conditions; can pre-fill from a selected preset
- `src/app/export/` — `ExportService` (JSON/CSV via Blob download) + `ExportControls` component; PNG is delegated up to `App` so the trajectory-plot's `downloadPng` can run with the Plotly graph div in scope
- `src/types/plotly-cartesian-dist-min.d.ts` — type shim re-exporting `plotly.js` types for the `plotly.js-cartesian-dist-min` bundle (partial bundle, ~700 kB; chosen over the full `dist-min` to fit the Angular initial-bundle budget)
- `src/app/trajectory-3d/` — Three.js 3D viewer; renders trajectory as a `BufferGeometry` line, Sun/Jupiter as spheres, Lagrange points as markers, on a dark ecliptic-plane grid. `OrbitControls` handles mouse rotate/zoom/pan. The component uses Angular `effect()` to rebuild dynamic scene objects when inputs change, and `ResizeObserver` to keep the renderer sized to its host. PNG export is unavailable in 3D mode (delegated to Plotly in the 2D view). Toggle between views via the button in the plot header (`App.viewMode` signal).

## Docker

`docker compose up --build` runs the full stack: backend (Spring Boot, internal-only) + frontend (nginx serving the Angular prod build, reverse-proxying `/api/*` to `backend:8080`). Frontend is published on host port `8090`. Per-service multi-stage Dockerfiles live at `Dockerfile` (root) and `frontend/Dockerfile`; nginx config at `frontend/nginx.conf`.

## Architecture

**Package root**: `org.github.oleksandrkukotin`

```
config/      PhysicsConstants (μ, 1−μ as static finals)
model/       Records: StateVector, SimulationRequest, TrajectoryResult,
             TrajectoryPoint, LagrangePoint, OrbitPreset
physics/     CR3BPEquations, LagrangePointCalculator, StateVectorPropagator,
             ZeroVelocityCurve, JacobiConstant
presets/     OrbitPresets (named initial condition library)
api/         SimulationController, PresetController (REST endpoints)
```

**REST API surface:**
- `POST /api/simulation/propagate` — run trajectory from `SimulationRequest`
- `GET  /api/simulation/lagrange-points` — returns L1–L5 in synodic coords
- `GET  /api/simulation/zero-velocity-curve` — forbidden region boolean grid
- `GET  /api/presets` — list named orbit presets
- `POST /api/presets/{id}/run` — run a preset through the propagator

**Key dependencies** (build.gradle.kts):
- `spring-boot-starter-web` — REST layer
- `commons-math3:3.6.1` — ODE integration (`DormandPrince853Integrator`, `FirstOrderDifferentialEquations`)
- `spring-boot-starter-test` — JUnit Jupiter + Spring test support

## Physics Domain

All quantities use **normalized CR3BP units**: distance = Sun–Jupiter separation, time = Jupiter orbital period / 2π.

- `PhysicsConstants.MU = 9.5368e-4` (mass parameter μ)
- `StateVector` is a record `(x, y, xDot, yDot)` with `toArray()`/`fromArray()` helpers
- `JacobiConstant`, `CR3BPEquations`, and `LagrangePointCalculator` (all five points) are fully implemented; `JacobiConstantTest` and `LagrangePointCalculatorTest` serve as the correctness baseline
- `CR3BPEquations` implements `FirstOrderDifferentialEquations` (Commons Math); it feeds into `StateVectorPropagator` which uses `DormandPrince853Integrator`. Its Javadoc documents a non-collision precondition: trajectories must not pass through either primary (r₁/r₂ → 0 diverges)
- `SimulationRequest` carries integrator tolerances (`absoluteTolerance`, `relativeTolerance`, `minStep`, `maxStep`) alongside the initial state and `duration`
- L1/L2/L3 use Newton's method on the collinear quintic (converges to 1e-12 in well under 50 iterations); L4/L5 are analytic at `(0.5−μ, ±√3/2)`
- `LagrangePointCalculator.computeAll()` returns an immutable `List.of(L1, L2, L3, L4, L5)`
- `CR3BPUtils` provides shared `distanceToSun` / `distanceToJupiter` helpers used across the physics package

**Synodic frame body positions**: Sun (primary) at `(−μ, 0)`, Jupiter (secondary) at `(1−μ, 0)`.

## Phase 1 Implementation Notes

Phase 1 (core physics) is complete. Non-obvious details worth knowing:

- `StateVectorPropagator` uses DormandPrince853 with a StepHandler that records per-step Jacobi values alongside each trajectory point
- Tadpole presets (L4/L5) are seeded at the triangular points with a 3e-3 radial offset; horseshoe uses `(-1.00045, 0, 0, 0.0012)`; expected Jacobi constants are computed at class-load from the seed state

## Collaboration Notes

The user implements features; Claude's role is reviewing physics correctness, writing tests, and running the test suite.