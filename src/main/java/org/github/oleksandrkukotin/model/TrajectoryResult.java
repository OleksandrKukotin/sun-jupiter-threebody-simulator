package org.github.oleksandrkukotin.model;

import java.util.List;

/** Result of a CR3BP trajectory integration. */
public record TrajectoryResult(
        List<TrajectoryPoint> points,
        double initialJacobiConstant,
        double finalJacobiConstant
) {}