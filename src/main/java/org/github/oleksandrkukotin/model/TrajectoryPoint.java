package org.github.oleksandrkukotin.model;

/** A single recorded step in a propagated trajectory. */
public record TrajectoryPoint(double time, StateVector state, double jacobiConstant) {}