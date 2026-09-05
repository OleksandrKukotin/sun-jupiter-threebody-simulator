package org.github.oleksandrkukotin.model;

import java.util.List;

/** Stable and unstable invariant manifold branches of a collinear Lagrange point. */
public record ManifoldResult(
        LagrangePoint lagrangePoint,
        double unstableEigenvalue,
        List<ManifoldBranch> branches
) {}
