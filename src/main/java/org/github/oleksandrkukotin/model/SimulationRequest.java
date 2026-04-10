package org.github.oleksandrkukotin.model;

/** Input parameters for a CR3BP trajectory simulation. */
public record SimulationRequest(
        StateVector initialState,
        double duration,
        double absoluteTolerance,
        double relativeTolerance,
        double minStep,
        double maxStep
) {}