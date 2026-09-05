package org.github.oleksandrkukotin.model;

/**
 * Input parameters for computing the invariant manifold branches of a collinear
 * Lagrange point.
 *
 * @param lagrangePointId "L1", "L2", or "L3" (L4/L5 are linearly stable — no manifolds)
 * @param perturbation    displacement ε along the eigenvector, in normalized distance units
 * @param duration        integration time per branch (positive; stable branches run backward)
 */
public record ManifoldRequest(
        String lagrangePointId,
        double perturbation,
        double duration,
        double absoluteTolerance,
        double relativeTolerance,
        double minStep,
        double maxStep
) {}
