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
- `JacobiConstant` is the only fully-implemented physics class; its tests in `JacobiConstantTest` serve as the correctness baseline
- `CR3BPEquations` implements `FirstOrderDifferentialEquations` (Commons Math interface); once implemented it feeds into `StateVectorPropagator` which uses `DormandPrince853Integrator`
- L1/L2/L3 require Newton's method on the collinear quintic; L4/L5 are analytic at `(0.5−μ, ±√3/2)`

## Implementation Status

Each stub throws `UnsupportedOperationException` with a GitHub issue reference. Implement in dependency order:

1. `CR3BPEquations.computeDerivatives` (issue #1) — no dependencies
2. `LagrangePointCalculator.computeAll` (issue #2) — no dependencies
3. `StateVectorPropagator.propagate` (issue #3) — depends on #1
4. `ZeroVelocityCurve.computeForbiddenRegion` (issue #5) — depends on `JacobiConstant.effectivePotential` (already done)