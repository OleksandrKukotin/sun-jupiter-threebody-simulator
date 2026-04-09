# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**CR3BP Explorer** — a Java/Spring Boot backend + Angular frontend simulator for the Circular Restricted Three-Body Problem (CR3BP), focused on the Sun–Jupiter system and its Lagrange points. Currently in early MVP phase: the repo contains an empty Gradle skeleton; the real implementation is being built out.

## Build & Run Commands

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "org.github.oleksandrkukotin.SomeTest"

# Run the application (once Spring Boot is wired in)
./gradlew bootRun
```

## Architecture

The project is a planned full-stack application:

- **Backend**: Java 21 + Spring Boot 3, built with Gradle (Kotlin DSL). Package root: `org.github.oleksandrkukotin`. Math core uses Apache Commons Math for high-order numerical integration of CR3BP equations.
- **Frontend**: Angular 18+ with Chart.js (2D trajectory visualization) and Three.js (planned 3D). Lives in a separate `frontend/` directory (not yet scaffolded).
- **REST API**: Spring Boot exposes endpoints for simulation control; Angular consumes them.

### Physics domain

The simulator works in the **rotating (synodic) reference frame** of the CR3BP. Key quantities:
- Mass parameter μ ≈ 9.5368×10⁻⁴ (Sun–Jupiter)
- Lagrange points L1–L5 calculated analytically/numerically
- State vector propagation with adaptive step size
- Jacobi constant conservation as a correctness check
- Zero-velocity curves separate accessible from forbidden regions

Planned orbit families: tadpole orbits (around L4/L5), horseshoe orbits, and eventually invariant manifolds for low-energy transfers.

## Dependencies (build.gradle.kts)

Currently only JUnit Jupiter (via `junit-bom:6.0.0`) for testing. Spring Boot, Apache Commons Math, and other dependencies will need to be added as implementation proceeds.