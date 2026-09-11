package org.github.oleksandrkukotin.presets;

import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.OrbitPreset;
import org.github.oleksandrkukotin.model.PeriodicOrbit;
import org.github.oleksandrkukotin.model.StateVector;
import org.github.oleksandrkukotin.physics.JacobiConstant;
import org.github.oleksandrkukotin.physics.LyapunovOrbitFinder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ready-to-run initial conditions for well-known Sun–Jupiter CR3BP orbits.
 *
 * <p>All state vectors use normalized units in the rotating synodic frame. The tadpole and
 * horseshoe presets are hardcoded literals; the Lyapunov preset is instead computed once at
 * construction time via {@link LyapunovOrbitFinder}'s differential corrector, since (unlike
 * the others) its exact initial state depends on {@link PhysicsConstants#MU} and can't be
 * hand-picked.
 *
 * @see <a href="https://github.com/OleksandrKukotin/sun-jupiter-threebody-simulator/issues/6">Issue #6</a>
 * @see <a href="https://github.com/OleksandrKukotin/sun-jupiter-threebody-simulator/issues/16">Issue #16</a>
 */
@Component
public class OrbitPresets {

    private static final double L4_X = 0.5 - PhysicsConstants.MU;
    private static final double L4_Y =  Math.sqrt(3.0) / 2.0;
    private static final double L5_Y = -Math.sqrt(3.0) / 2.0;

    // Small radial displacement from the triangular point seeds the long-period
    // libration while keeping the orbit inside the tadpole regime.
    private static final double TADPOLE_DX = 3.0e-3;

    // Well inside the linear-theory regime, so the corrector converges reliably.
    private static final double LYAPUNOV_L1_AMPLITUDE = 1e-3;

    private static final JacobiConstant JACOBI = new JacobiConstant();

    private static final StateVector TADPOLE_L4_STATE =
            new StateVector(L4_X + TADPOLE_DX, L4_Y, 0.0, 0.0);
    private static final StateVector TADPOLE_L5_STATE =
            new StateVector(L4_X + TADPOLE_DX, L5_Y, 0.0, 0.0);
    private static final StateVector HORSESHOE_STATE =
            new StateVector(-1.00045, 0.0, 0.0, 0.0012);

    private final OrbitPreset lyapunovL1Preset;

    public OrbitPresets(LyapunovOrbitFinder lyapunovOrbitFinder) {
        PeriodicOrbit orbit = lyapunovOrbitFinder.findLyapunovOrbit("L1", LYAPUNOV_L1_AMPLITUDE);
        this.lyapunovL1Preset = new OrbitPreset(
                "lyapunov-l1-small",
                "Small Lyapunov orbit around L1",
                "Planar Lyapunov orbit around L1 (Ax=" + LYAPUNOV_L1_AMPLITUDE
                        + "), found via differential correction. Shown for exactly one "
                        + "period — L1's saddle instability amplifies numerical error too "
                        + "fast for more laps to stay visually closed.",
                orbit.initialState(),
                orbit.period(),
                orbit.jacobiConstant()
        );
    }

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
                ),
                lyapunovL1Preset
        );
    }

    public OrbitPreset findById(String id) {
        return getAll().stream()
                .filter(p -> p.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown preset id: " + id));
    }
}
