package org.github.oleksandrkukotin.model;

import java.util.List;

/**
 * A converged {@link PeriodicOrbit} paired with its trajectory over one full period,
 * ready for plotting.
 *
 * @param orbit  the differentially-corrected periodic orbit
 * @param points the orbit's initial state propagated forward through one full period
 */
public record PeriodicOrbitResult(PeriodicOrbit orbit, List<TrajectoryPoint> points) {}
