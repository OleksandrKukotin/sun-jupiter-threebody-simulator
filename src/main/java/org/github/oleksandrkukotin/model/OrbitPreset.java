package org.github.oleksandrkukotin.model;

/** A named, ready-to-run orbit configuration with validated initial conditions. */
public record OrbitPreset(
        String id,
        String name,
        String description,
        StateVector initialState,
        double duration,
        double expectedJacobiConstant
) {}