package org.github.oleksandrkukotin.presets;

import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.OrbitPreset;
import org.github.oleksandrkukotin.model.SimulationRequest;
import org.github.oleksandrkukotin.model.TrajectoryPoint;
import org.github.oleksandrkukotin.model.TrajectoryResult;
import org.github.oleksandrkukotin.physics.CR3BPEquations;
import org.github.oleksandrkukotin.physics.CR3BPVariationalEquations;
import org.github.oleksandrkukotin.physics.JacobiConstant;
import org.github.oleksandrkukotin.physics.LagrangePointCalculator;
import org.github.oleksandrkukotin.physics.LyapunovOrbitFinder;
import org.github.oleksandrkukotin.physics.StateVectorPropagator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation harness for the three Phase-1 orbit presets. Each preset is
 * propagated with tight tolerances and checked for:
 *   1. Jacobi constant conservation at every recorded step (|ΔC/C| < 1e-8)
 *   2. Agreement between the stored expectedJacobiConstant and the true value
 *   3. A regime-specific spatial bounding box (tadpoles stay near L4/L5,
 *      horseshoe wraps both triangular points without hitting Jupiter).
 *
 * Use this test to iterate on the initial conditions in {@link OrbitPresets}
 * until all assertions pass, then lock the numbers in.
 */
class OrbitPresetsValidationTest {

    private static final double ABS_TOL = 1e-12;
    private static final double REL_TOL = 1e-12;
    private static final double MIN_STEP = 1e-8;
    private static final double MAX_STEP = 1.0;

    private static final double JACOBI_DRIFT_TOL = 1e-8;

    private static final double MU = PhysicsConstants.MU;
    private static final double L4_X = 0.5 - MU;
    private static final double L4_Y =  Math.sqrt(3.0) / 2.0;
    private static final double L5_Y = -Math.sqrt(3.0) / 2.0;

    private OrbitPresets presets;
    private StateVectorPropagator propagator;
    private JacobiConstant jacobi;

    @BeforeEach
    void setUp() {
        CR3BPEquations equations = new CR3BPEquations();
        jacobi = new JacobiConstant();
        LyapunovOrbitFinder finder = new LyapunovOrbitFinder(
                new LagrangePointCalculator(), equations, new CR3BPVariationalEquations(equations), jacobi);
        presets = new OrbitPresets(finder);
        propagator = new StateVectorPropagator(equations, jacobi);
    }

    @Test
    void tadpoleL4_staysBoundedNearL4() {
        OrbitPreset p = presets.findById("tadpole-l4");
        TrajectoryResult r = propagate(p);

        assertJacobiConserved(r, p);
        assertAllPointsWithin(r, L4_X, L4_Y, 0.15,
                "tadpole-l4 drifted outside 0.15 of L4");
        // tadpole stays on the leading (y > 0) side
        for (TrajectoryPoint pt : r.points()) {
            assertTrue(pt.state().y() > 0.3,
                    "tadpole-l4 crossed below y=0.3 at t=" + pt.time());
        }
    }

    @Test
    void tadpoleL5_staysBoundedNearL5() {
        OrbitPreset p = presets.findById("tadpole-l5");
        TrajectoryResult r = propagate(p);

        assertJacobiConserved(r, p);
        assertAllPointsWithin(r, L4_X, L5_Y, 0.15,
                "tadpole-l5 drifted outside 0.15 of L5");
        for (TrajectoryPoint pt : r.points()) {
            assertTrue(pt.state().y() < -0.3,
                    "tadpole-l5 crossed above y=-0.3 at t=" + pt.time());
        }
    }

    @Test
    void horseshoe_wrapsBothTriangularPointsWithoutHittingJupiter() {
        OrbitPreset p = presets.findById("horseshoe");
        TrajectoryResult r = propagate(p);

        assertJacobiConserved(r, p);

        boolean sawLeading = false;   // y > +0.5 (near L4)
        boolean sawTrailing = false;  // y < -0.5 (near L5)
        double jupiterX = 1.0 - MU;
        double minDistToJupiter = Double.POSITIVE_INFINITY;

        for (TrajectoryPoint pt : r.points()) {
            double y = pt.state().y();
            if (y >  0.5) sawLeading  = true;
            if (y < -0.5) sawTrailing = true;

            double dx = pt.state().x() - jupiterX;
            double dy = pt.state().y();
            minDistToJupiter = Math.min(minDistToJupiter, Math.hypot(dx, dy));
        }

        assertTrue(sawLeading,  "horseshoe never reached the L4 side");
        assertTrue(sawTrailing, "horseshoe never reached the L5 side");
        assertTrue(minDistToJupiter > 0.1,
                "horseshoe came too close to Jupiter: " + minDistToJupiter);
    }

    @Test
    void lyapunovL1_isPeriodicAndStaysNearL1() {
        OrbitPreset p = presets.findById("lyapunov-l1-small");
        TrajectoryResult r = propagate(p);

        assertJacobiConserved(r, p);

        double l1X = new LagrangePointCalculator().computeAll().stream()
                .filter(lp -> lp.name().equals("L1"))
                .findFirst().orElseThrow()
                .x();
        // Radius is ~20x the seed amplitude, well clear of both primaries, but tight
        // enough to catch the corrector converging to the wrong (non-periodic) orbit.
        assertAllPointsWithin(r, l1X, 0.0, 0.02,
                "lyapunov-l1-small drifted outside 0.02 of L1");

        double[] initial = p.initialState().toArray();
        TrajectoryPoint last = r.points().get(r.points().size() - 1);
        double[] finalState = last.state().toArray();
        for (int i = 0; i < 4; i++) {
            assertEquals(initial[i], finalState[i], 1e-6,
                    "lyapunov-l1-small should return to its initial state after one period");
        }
    }

    @Test
    void expectedJacobiConstant_matchesInitialState() {
        for (OrbitPreset p : presets.getAll()) {
            double actual = jacobi.compute(p.initialState());
            assertEquals(p.expectedJacobiConstant(), actual, 1e-10,
                    "expectedJacobiConstant mismatch for preset " + p.id());
        }
    }

    // --- helpers ---------------------------------------------------------

    private TrajectoryResult propagate(OrbitPreset p) {
        SimulationRequest req = new SimulationRequest(
                p.initialState(), p.duration(),
                ABS_TOL, REL_TOL, MIN_STEP, MAX_STEP);
        return propagator.propagate(req);
    }

    private void assertJacobiConserved(TrajectoryResult r, OrbitPreset p) {
        double c0 = r.initialJacobiConstant();
        for (TrajectoryPoint pt : r.points()) {
            double drift = Math.abs(pt.jacobiConstant() - c0) / Math.abs(c0);
            assertTrue(drift < JACOBI_DRIFT_TOL,
                    "preset " + p.id() + ": Jacobi drift " + drift
                            + " at t=" + pt.time());
        }
    }

    private void assertAllPointsWithin(TrajectoryResult r,
                                       double cx, double cy,
                                       double radius, String msg) {
        for (TrajectoryPoint pt : r.points()) {
            double dx = pt.state().x() - cx;
            double dy = pt.state().y() - cy;
            double d  = Math.hypot(dx, dy);
            assertTrue(d <= radius,
                    msg + " (d=" + d + " at t=" + pt.time() + ")");
        }
    }
}
