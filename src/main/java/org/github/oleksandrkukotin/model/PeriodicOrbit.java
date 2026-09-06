package org.github.oleksandrkukotin.model;

/**
 * A converged planar Lyapunov orbit around a collinear Lagrange point (L1, L2, or L3).
 *
 * @param lagrangePoint  name of the collinear point this orbit librates around ("L1"/"L2"/"L3")
 * @param amplitude      x-amplitude Ax used to seed the family (normalized units)
 * @param initialState   differentially-corrected initial condition (y=0-crossing state on the x-axis half of the orbit is NOT assumed — this is whatever state the corrector converged to)
 * @param period         full orbital period (normalized time units)
 * @param jacobiConstant Jacobi constant of the orbit
 */
public record PeriodicOrbit(
        String lagrangePoint,
        double amplitude,
        StateVector initialState,
        double period,
        double jacobiConstant
) {}
