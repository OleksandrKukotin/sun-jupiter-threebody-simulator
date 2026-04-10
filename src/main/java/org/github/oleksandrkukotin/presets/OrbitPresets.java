package org.github.oleksandrkukotin.presets;

import org.github.oleksandrkukotin.model.OrbitPreset;
import org.github.oleksandrkukotin.model.StateVector;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hardcoded initial conditions for well-known Sun–Jupiter CR3BP orbits.
 *
 * <p>All state vectors use normalized units in the rotating synodic frame.
 * Placeholder zeros must be replaced with validated values from literature.
 *
 * @see <a href="https://github.com/OleksandrKukotin/sun-jupiter-threebody-simulator/issues/6">Issue #6</a>
 */
@Component
public class OrbitPresets {

    /**
     * Returns all available orbit presets.
     * TODO (#6): Replace placeholder StateVectors, durations, and Jacobi constants
     *            with values validated against the propagator.
     */
    public List<OrbitPreset> getAll() {
        return List.of(
                new OrbitPreset(
                        "tadpole-l4",
                        "Tadpole orbit around L4",
                        "Small-amplitude libration around Jupiter's L4 (Greek camp, leading 60°)",
                        new StateVector(0.0, 0.0, 0.0, 0.0), // TODO (#6)
                        0.0,  // TODO (#6): integration duration (normalized time units)
                        0.0   // TODO (#6): expected Jacobi constant
                ),
                new OrbitPreset(
                        "tadpole-l5",
                        "Tadpole orbit around L5",
                        "Small-amplitude libration around Jupiter's L5 (Trojan camp, trailing 60°)",
                        new StateVector(0.0, 0.0, 0.0, 0.0), // TODO (#6)
                        0.0,
                        0.0
                ),
                new OrbitPreset(
                        "horseshoe",
                        "Horseshoe orbit",
                        "Large-amplitude orbit librating around both L4 and L5 via L3",
                        new StateVector(0.0, 0.0, 0.0, 0.0), // TODO (#6)
                        0.0,
                        0.0
                )
        );
    }

    public OrbitPreset findById(String id) {
        return getAll().stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown preset id: " + id));
    }
}