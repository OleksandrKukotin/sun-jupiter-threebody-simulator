package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.SimulationRequest;
import org.github.oleksandrkukotin.model.StateVector;
import org.github.oleksandrkukotin.model.TrajectoryPoint;
import org.github.oleksandrkukotin.model.TrajectoryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.function.DoubleBinaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * A trajectory that hits a primary is a physical outcome, not an error: the propagator
 * should stop at the body's surface and hand back everything computed up to that point.
 *
 * <p>The impact seeds were found by integrating backward from a radial, parabolic infall
 * at r = 1e-5 of each primary. Propagated forward with the Custom Run form's default
 * tolerances, they reach minStep at r ≈ 5e-5 (Jupiter) and r ≈ 5e-4 (Sun), inside each
 * body's physical radius.
 */
class StateVectorPropagatorCollisionTest {

    // Same values as the defaults in frontend custom-run.ts.
    private static final double ABS_TOL = 1e-10;
    private static final double REL_TOL = 1e-10;
    private static final double MIN_STEP = 1e-6;
    private static final double MAX_STEP = 1.0;

    private static final double DURATION = 5.0;
    private static final double IMPACT_TIME_TOLERANCE = 0.05;

    /** Wide enough for either mean or equatorial radius, which differ by about 2% for Jupiter. */
    private static final double RADIUS_TOLERANCE = 0.1;

    /**
     * {@code jacobiDriftLimit} is 5-12x the drift measured at the form's default tolerances
     * (8.4e-10 Jupiter, 1.0e-4 Sun). Drift scales linearly with tolerance, and is larger at the
     * Sun because 2(1-mu)/r is roughly 750x the Jacobi constant at its surface.
     */
    private enum Body {
        JUPITER(new StateVector(1.14819396326, 0.164613921996, -0.062164220388, -0.27417844989),
                1.0, 8.98e-5, PhysicsConstants.MU, PhysicsConstants.ONE_MINUS_MU, 1e-8, CR3BPUtils::distanceToJupiter),
        SUN(new StateVector(0.70571396203, 0.21856960347, -1.352426650561, -1.192176007811),
                0.3, 8.94e-4, PhysicsConstants.ONE_MINUS_MU, -PhysicsConstants.MU, 5e-4, CR3BPUtils::distanceToSun);

        final StateVector impactSeed;
        final double impactTime;
        final double radius;
        final double mass;
        final double centreX;
        final double jacobiDriftLimit;
        final DoubleBinaryOperator distance;

        Body(StateVector impactSeed, double impactTime, double radius, double mass, double centreX,
             double jacobiDriftLimit, DoubleBinaryOperator distance) {
            this.impactSeed = impactSeed;
            this.impactTime = impactTime;
            this.radius = radius;
            this.mass = mass;
            this.centreX = centreX;
            this.jacobiDriftLimit = jacobiDriftLimit;
            this.distance = distance;
        }

        double distanceTo(TrajectoryPoint p) {
            return distance.applyAsDouble(p.state().x(), p.state().y());
        }
    }

    private StateVectorPropagator propagator;

    @BeforeEach
    void setUp() {
        propagator = new StateVectorPropagator(new CR3BPEquations(), new JacobiConstant());
    }

    private static SimulationRequest request(StateVector s0, double duration) {
        return new SimulationRequest(s0, duration, ABS_TOL, REL_TOL, MIN_STEP, MAX_STEP);
    }

    private static TrajectoryPoint last(List<TrajectoryPoint> points) {
        return points.get(points.size() - 1);
    }

    @ParameterizedTest
    @EnumSource(Body.class)
    void propagate_onImpact_stopsAtBodySurfaceAndReturnsTrajectory(Body body) {
        List<TrajectoryPoint> pts = propagator.propagate(request(body.impactSeed, DURATION)).points();

        assertTrue(pts.size() > 1, "the trajectory before impact must be returned");
        assertEquals(body.impactTime, last(pts).time(), IMPACT_TIME_TOLERANCE,
                "must stop at the impact, not before or after");
        assertEquals(body.radius, body.distanceTo(last(pts)), RADIUS_TOLERANCE * body.radius,
                "last point should be on the surface of " + body);
    }

    @ParameterizedTest
    @EnumSource(Body.class)
    void propagate_onImpact_lastPointIsTheClosestApproach(Body body) {
        // Nothing may be recorded after contact: the trajectory cannot pass through the body.
        List<TrajectoryPoint> pts = propagator.propagate(request(body.impactSeed, DURATION)).points();

        double lastDistance = body.distanceTo(last(pts));
        for (TrajectoryPoint p : pts) {
            assertTrue(body.distanceTo(p) >= lastDistance, "got closer than the final point at t=" + p.time());
        }
    }

    @ParameterizedTest
    @EnumSource(Body.class)
    void propagate_onImpact_recordsOnlyFiniteStatesAndConservesJacobi(Body body) {
        TrajectoryResult result = propagator.propagate(request(body.impactSeed, DURATION));

        double cInit = result.initialJacobiConstant();
        for (TrajectoryPoint p : result.points()) {
            for (double component : p.state().toArray()) {
                assertTrue(Double.isFinite(component), "non-finite state at t=" + p.time());
            }
            double relDrift = Math.abs(p.jacobiConstant() - cInit) / Math.abs(cInit);
            assertTrue(relDrift < body.jacobiDriftLimit, "Jacobi drift at t=" + p.time() + " was " + relDrift);
        }
    }

    @ParameterizedTest
    @EnumSource(Body.class)
    void propagate_orbitAtTwoBodyRadii_isNotTreatedAsCollision(Body body) {
        // Guards against an oversized collision radius. Starts at periapsis, so r never drops below 2R.
        double r = 2 * body.radius;
        StateVector s0 = new StateVector(body.centreX + r, 0.0, 0.0, Math.sqrt(body.mass / r));
        double duration = 0.01;

        List<TrajectoryPoint> pts = propagator.propagate(request(s0, duration)).points();

        assertEquals(duration, last(pts).time(), 1e-9);
        for (TrajectoryPoint p : pts) {
            assertTrue(body.distanceTo(p) > body.radius, "orbit touched " + body + " at t=" + p.time());
        }
    }
}
