package org.github.oleksandrkukotin.model;

import java.util.List;

/**
 * One branch of an invariant manifold: "unstable-plus", "unstable-minus",
 * "stable-plus", or "stable-minus". Plus/minus is the sign of the eigenvector
 * displacement; stable branches carry decreasing (negative) times because they
 * are integrated backward.
 */
public record ManifoldBranch(String name, List<TrajectoryPoint> points) {}
