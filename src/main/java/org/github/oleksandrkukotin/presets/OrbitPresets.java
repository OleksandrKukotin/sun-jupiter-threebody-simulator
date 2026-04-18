package org.github.oleksandrkukotin.presets;

import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.OrbitPreset;
import org.github.oleksandrkukotin.model.StateVector;
import org.github.oleksandrkukotin.physics.JacobiConstant;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hardcoded initial conditions for well-known Sun–Jupiter CR3BP orbits.
 *
 * <p>All state vectors use normalized units in the rotating synodic frame.
 *
 * @see <a href="https://github.com/OleksandrKukotin/sun-jupiter-threebody-simulator/issues/6">Issue #6</a>
 */
@Component
public class OrbitPresets {

    private static final double L4_X = 0.5 - PhysicsConstants.MU;
    private static final double L4_Y =  Math.sqrt(3.0) / 2.0;
    private static final double L5_Y = -Math.sqrt(3.0) / 2.0;

    // Small radial displacement from the triangular point seeds the long-period
    // libration while keeping the orbit inside the tadpole regime.
    private static final double TADPOLE_DX = 3.0e-3;

    private static final JacobiConstant JACOBI = new JacobiConstant();

    private static final StateVector TADPOLE_L4_STATE =
            new StateVector(L4_X + TADPOLE_DX, L4_Y, 0.0, 0.0);
    private static final StateVector TADPOLE_L5_STATE =
            new StateVector(L4_X + TADPOLE_DX, L5_Y, 0.0, 0.0);
    private static final StateVector HORSESHOE_STATE =
            new StateVector(-1.00045, 0.0, 0.0, 0.0012);

    public List<OrbitPreset> getAll() {
        return List.of(
                new OrbitPreset(
                        "tadpole-l4",
                        "Tadpole orbit around L4",
                        "Small-amplitude libration around Jupiter's L4 (Greek camp, leading 60°)",
                        TADPOLE_L4_STATE,
                        100.0,
                        JACOBI.compute(TADPOLE_L4_STATE)
                ),
                new OrbitPreset(
                        "tadpole-l5",
                        "Tadpole orbit around L5",
                        "Small-amplitude libration around Jupiter's L5 (Trojan camp, trailing 60°)",
                        TADPOLE_L5_STATE,
                        100.0,
                        JACOBI.compute(TADPOLE_L5_STATE)
                ),
                new OrbitPreset(
                        "horseshoe",
                        "Horseshoe orbit",
                        "Large-amplitude orbit librating around both L4 and L5 via L3",
                        HORSESHOE_STATE,
                        2000.0,
                        JACOBI.compute(HORSESHOE_STATE)
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
