# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**CR3BP Explorer** — a Java/Spring Boot backend simulator for the Circular Restricted Three-Body Problem (CR3BP), focused on the Sun–Jupiter system and its Lagrange points. The Angular frontend (`frontend/`) is not yet scaffolded.

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

## Implementation Status

Unimplemented stubs throw `UnsupportedOperationException` with a GitHub issue reference. Dependency order:

1. ~~`CR3BPEquations.computeDerivatives` (issue #1)~~ — **done**
2. ~~`LagrangePointCalculator.computeAll` / L1–L5 (issue #2)~~ — **done**
3. ~~`StateVectorPropagator.propagate` (issue #3)~~ — **done** (DormandPrince853 + StepHandler recording per-step Jacobi)
4. ~~`ZeroVelocityCurve.computeForbiddenRegion` (issue #5)~~ — **done**
5. ~~`OrbitPresets` state vectors (issue #6)~~ — **done** (tadpole L4/L5 seeded at triangular points + 3e-3 radial offset; horseshoe at `(-1.00045, 0, 0, 0.0012)`; Jacobi constants computed at class-load from the seed state)

## Collaboration Notes

The user implements features; Claude's role is reviewing physics correctness, writing tests, and running the test suite.