package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.SimulationRequest;
import org.github.oleksandrkukotin.model.StateVector;
import org.github.oleksandrkukotin.model.TrajectoryPoint;
import org.github.oleksandrkukotin.model.TrajectoryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.ToDoubleFunction;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A trajectory that hits a primary is a physical outcome, not an error: the propagator
 * should stop near the body and hand back everything computed up to that point.
 *
 * <p>The impact seeds were found by integrating backward from a radial, parabolic infall
 * at r = 1e-5 of each primary. Propagated forward with the Custom Run form's default
 * tolerances, they reach minStep at r ≈ 5e-5 (Jupiter) and r ≈ 5e-4 (Sun), inside each
 * body's physical radius (≈ 9.0e-5 and 8.9e-4 in normalized units).
 */
class StateVectorPropagatorCollisionTest {

    // Same values as the defaults in frontend custom-run.ts.
    private static final double ABS_TOL = 1e-10;
    private static final double REL_TOL = 1e-10;
    private static final double MIN_STEP = 1e-6;
    private static final double MAX_STEP = 1.0;

    private static final double DURATION = 5.0;

    private static final StateVector JUPITER_IMPACT =
            new StateVector(1.14819396326, 0.164613921996, -0.062164220388, -0.27417844989);
    private static final double JUPITER_IMPACT_TIME = 1.0;

    private static final StateVector SUN_IMPACT =
            new StateVector(0.70571396203, 0.21856960347, -1.352426650561, -1.192176007811);
    private static final double SUN_IMPACT_TIME = 0.3;

    /** Deliberately loose: pins "stopped at the body" without pinning the collision radius. */
    private static final double NEAR_BODY = 5e-3;
    private static final double IMPACT_TIME_TOLERANCE = 0.05;

    private StateVectorPropagator propagator;

    @BeforeEach
    void setUp() {
        propagator = new StateVectorPropagator(new CR3BPEquations(), new JacobiConstant());
    }

    private static SimulationRequest request(StateVector s0, double duration) {
        return new SimulationRequest(s0, duration, ABS_TOL, REL_TOL, MIN_STEP, MAX_STEP);
    }

    private static double distanceToJupiter(TrajectoryPoint p) {
        return CR3BPUtils.distanceToJupiter(p.state().x(), p.state().y());
    }

    private static double distanceToSun(TrajectoryPoint p) {
        return CR3BPUtils.distanceToSun(p.state().x(), p.state().y());
    }

    private void assertStoppedAtBody(StateVector s0, double impactTime, ToDoubleFunction<TrajectoryPoint> distance) {
        TrajectoryResult result = propagator.propagate(request(s0, DURATION));

        List<TrajectoryPoint> pts = result.points();
        assertFalse(pts.isEmpty(), "the trajectory before impact must be returned");
        TrajectoryPoint last = pts.get(pts.size() - 1);

        assertEquals(impactTime, last.time(), IMPACT_TIME_TOLERANCE, "must stop at the impact, not before or after");
        assertTrue(distance.applyAsDouble(last) < NEAR_BODY,
                "last point should be at the body, was r=" + distance.applyAsDouble(last));
    }

    @Test
    void propagate_onJupiterImpact_stopsAtJupiterAndReturnsTrajectory() {
        assertStoppedAtBody(JUPITER_IMPACT, JUPITER_IMPACT_TIME, StateVectorPropagatorCollisionTest::distanceToJupiter);
    }

    @Test
    void propagate_onSunImpact_stopsAtSunAndReturnsTrajectory() {
        assertStoppedAtBody(SUN_IMPACT, SUN_IMPACT_TIME, StateVectorPropagatorCollisionTest::distanceToSun);
    }

    @Test
    void propagate_onImpact_lastPointIsTheClosestApproach() {
        // Nothing may be recorded after contact: the trajectory cannot pass through the body.
        List<TrajectoryPoint> pts = propagator.propagate(request(JUPITER_IMPACT, DURATION)).points();

        double lastDistance = distanceToJupiter(pts.get(pts.size() - 1));
        for (TrajectoryPoint p : pts) {
            assertTrue(distanceToJupiter(p) >= lastDistance, "got closer than the final point at t=" + p.time());
        }
    }

    @Test
    void propagate_onImpact_recordsOnlyFiniteStatesAndConservesJacobi() {
        TrajectoryResult result = propagator.propagate(request(JUPITER_IMPACT, DURATION));

        double cInit = result.initialJacobiConstant();
        for (TrajectoryPoint p : result.points()) {
            for (double component : p.state().toArray()) {
                assertTrue(Double.isFinite(component), "non-finite state at t=" + p.time());
            }
            double relDrift = Math.abs(p.jacobiConstant() - cInit) / Math.abs(cInit);
            assertTrue(relDrift < 1e-6, "Jacobi drift at t=" + p.time() + " was " + relDrift);
        }
    }

    @Test
    void propagate_closeOrbitAroundJupiter_isNotTreatedAsCollision() {
        // Guards against an oversized collision radius. r = 0.01 is roughly 110 Jupiter radii.
        double r = 0.01;
        double circularSpeed = Math.sqrt(PhysicsConstants.MU / r);
        StateVector s0 = new StateVector(PhysicsConstants.ONE_MINUS_MU + r, 0.0, 0.0, circularSpeed);
        double duration = 0.5;

        List<TrajectoryPoint> pts = propagator.propagate(request(s0, duration)).points();

        assertEquals(duration, pts.get(pts.size() - 1).time(), 1e-9);
    }
}
