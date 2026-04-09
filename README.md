# Sun-Jupiter Three-Body Simulator

**CR3BP Explorer** — A modern web-based simulator for the **Circular Restricted Three-Body Problem** focused on the **Sun–Jupiter** system and its Lagrange (libration) points.

![Java](https://img.shields.io/badge/Java-21%2B-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=spring&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.x-02303A?logo=gradle&logoColor=white)
![Angular](https://img.shields.io/badge/Angular-18%2B-DD0031?logo=angular&logoColor=white)
![License](https://img.shields.io/badge/license-MIT-blue)

## About the Project

Watching the Artemis II mission’s impressive lunar free-return trajectory surpass Apollo 13’s historic distance record reminded me of my own master’s work on Jupiter’s libration points and motivated me to return to this fascinating topic with modern tools.

This simulator is a modern re-implementation and significant upgrade of my master's thesis defended in 2022 at Cherkasy National University (specialty 104 Physics and Astronomy).

**Master's Thesis Title:**  
"Моделювання космічних польотів до та між точками лібрації на орбіті Юпітера"  
(Modeling of Space Flights to and between Libration Points on Jupiter's Orbit)

### Background

In the original work I used the educational software **VPNBody** (based on Visual Python) to model the Sun–Jupiter system. The spacecraft was placed using mean orbital elements, demonstrating entry into Jupiter’s gravitational field with a characteristic wavy trajectory and stable positions near the triangular Lagrange points **L4** (Greek camp) and **L5** (Trojan camp).

While the 2022 model was sufficient for demonstration purposes, it had important limitations:
- Approximate representation of L4 and L5 using shifted Keplerian elements
- Simple numerical integration (Euler method) with large time steps
- Lack of proper rotating (synodic) reference frame
- No conservation of the Jacobi integral
- Limited accuracy for long-term libration dynamics (tadpole and horseshoe orbits)

This new project rebuilds the concept using a rigorous **Circular Restricted Three-Body Problem (CR3BP)** formulation and modern technology stack.

## What is CR3BP?

The Circular Restricted Three-Body Problem describes the motion of a massless particle (spacecraft or asteroid) in the gravitational field of two primaries (Sun and Jupiter) moving in circular orbits around their common barycenter.

In the rotating synodic frame, five equilibrium points exist — the **Lagrange points** (L1–L5). For the Sun–Jupiter system (μ ≈ 9.5368 × 10⁻⁴), L4 and L5 are stable and host thousands of Trojan asteroids.

The simulator lets you explore:
- Tadpole orbits around L4 and L5
- Horseshoe orbits
- Zero-velocity curves defined by the Jacobi constant
- Libration dynamics and low-energy trajectory behavior

## Features

### Current (MVP)
- Accurate CR3BP equations in normalized units
- Lagrange points calculator (L1–L5) for Sun–Jupiter
- High-order numerical integration (Apache Commons Math)
- REST API for simulation control
- Angular frontend for parameter input and visualization

### Roadmap

#### Phase 1 – Core Physics (In Progress)
- [ ] CR3BP dynamical model
- [ ] Lagrange point calculator
- [ ] State vector propagation with adaptive step size
- [ ] Jacobi constant calculation
- [ ] Zero-velocity curves visualization
- [ ] Preset tadpole and horseshoe orbit examples

#### Phase 2 – Web Application
- [ ] Responsive Angular UI with reactive forms
- [ ] 2D trajectory plotting (Chart.js or D3.js)
- [ ] 3D visualization (Three.js planned)
- [ ] Real-time / on-demand simulation runner
- [ ] Export functionality (JSON, CSV, PNG)

#### Phase 3 – Advanced Astrodynamics
- [ ] Invariant manifolds and low-energy transfers
- [ ] Periodic orbits near collinear Lagrange points
- [ ] Support for orbital inclinations (3D)
- [ ] Integration with real Trojan asteroid data (JPL)
- [ ] Station-keeping maneuver estimation

#### Phase 4 – Future Enhancements
- Multi-system support (Sun-Earth, Earth-Moon, etc.)
- Integration with Orekit library
- User simulation history and comparison
- Public demo deployment

## Technology Stack

- **Backend**: Java 21 + Spring Boot 3
- **Build Tool**: Gradle (Kotlin DSL recommended)
- **Math Core**: Apache Commons Math
- **Frontend**: Angular 18+
- **Visualization**: Chart.js (2D) + Three.js (planned for 3D)
- **Optional**: Orekit (future high-precision astrodynamics)

## Getting Started

### Prerequisites
- Java 21 or higher
- Node.js 20+ and Angular CLI (for frontend)
- Gradle 8+

### Backend

```bash
git clone https://github.com/sashko_master/sun-jupiter-threebody-simulator.git
cd sun-jupiter-threebody-simulator/backend
./gradlew bootRun