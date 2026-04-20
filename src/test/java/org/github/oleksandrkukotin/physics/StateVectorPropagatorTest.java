package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.SimulationRequest;
import org.github.oleksandrkukotin.model.StateVector;
import org.github.oleksandrkukotin.model.TrajectoryPoint;
import org.github.oleksandrkukotin.model.TrajectoryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StateVectorPropagatorTest {

    private StateVectorPropagator propagator;
    private JacobiConstant jacobiConstant;

    private static final double ABS_TOL = 1e-12;
    private static final double REL_TOL = 1e-12;
    private static final double MIN_STEP = 1e-8;
    private static final double MAX_STEP = 1.0;

    @BeforeEach
    void setUp() {
        jacobiConstant = new JacobiConstant();
        propagator = new StateVectorPropagator(new CR3BPEquations(), jacobiConstant);
    }

    private SimulationRequest request(StateVector s0, double duration) {
        return new SimulationRequest(s0, duration, ABS_TOL, REL_TOL, MIN_STEP, MAX_STEP);
    }

    @Test
    void propagate_producesNonEmptyTrajectoryWithMonotonicTime() {
        StateVector s0 = new StateVector(0.5, 0.1, 0.0, 0.5);
        TrajectoryResult result = propagator.propagate(request(s0, 2.0));

        List<TrajectoryPoint> pts = result.points();
        assertFalse(pts.isEmpty(), "trajectory must contain at least one recorded step");
        for (int i = 1; i < pts.size(); i++) {
            assertTrue(pts.get(i).time() > pts.get(i - 1).time(),
                    "time must be strictly increasing at step " + i);
        }
    }

    @Test
    void propagate_reachesRequestedDuration() {
        StateVector s0 = new StateVector(0.5, 0.1, 0.0, 0.5);
        double duration = 3.0;
        TrajectoryResult result = propagator.propagate(request(s0, duration));

        double finalTime = result.points().get(result.points().size() - 1).time();
        assertEquals(duration, finalTime, 1e-9);
    }

    @Test
    void propagate_conservesJacobiConstant() {
        // Jacobi's constant is a first integral of the CR3BP — a well-tuned
        // Dormand–Prince 8(5,3) run should hold |ΔC/C| well below 1e-10.
        StateVector s0 = new StateVector(0.5, 0.1, 0.0, 0.5);
        TrajectoryResult result = propagator.propagate(request(s0, 5.0));

        double cInit = result.initialJacobiConstant();
        double cFin  = result.finalJacobiConstant();
        double relDrift = Math.abs(cFin - cInit) / Math.abs(cInit);
        assertTrue(relDrift < 1e-10, "Jacobi drift too large: " + relDrift);
    }

    @Test
    void propagate_startsFromInitialStateJacobi() {
        StateVector s0 = new StateVector(0.5, 0.1, 0.0, 0.5);
        TrajectoryResult result = propagator.propagate(request(s0, 1.0));

        assertEquals(jacobiConstant.compute(s0), result.initialJacobiConstant(), 1e-12);
    }

    @Test
    void propagate_atL4WithZeroVelocity_stateStaysNearL4() {
        // L4 is an equilibrium; with zero velocity the state should barely move
        // over a modest integration interval (linear instability is weak at Sun–Jupiter μ).
        StateVector l4 = new StateVector(0.5 - PhysicsConstants.MU, Math.sqrt(3) / 2.0, 0.0, 0.0);
        TrajectoryResult result = propagator.propagate(request(l4, 1.0));

        StateVector last = result.points().get(result.points().size() - 1).state();
        assertEquals(l4.x(),    last.x(),    1e-8);
        assertEquals(l4.y(),    last.y(),    1e-8);
        assertEquals(l4.xDot(), last.xDot(), 1e-8);
        assertEquals(l4.yDot(), last.yDot(), 1e-8);
    }

    @Test
    void propagate_jacobiConstantIsConsistentAlongEveryStep() {
        StateVector s0 = new StateVector(0.5, 0.1, 0.0, 0.5);
        TrajectoryResult result = propagator.propagate(request(s0, 4.0));

        double cInit = result.initialJacobiConstant();
        for (TrajectoryPoint p : result.points()) {
            double relDrift = Math.abs(p.jacobiConstant() - cInit) / Math.abs(cInit);
            assertTrue(relDrift < 1e-10,
                    "Jacobi drift at t=" + p.time() + " was " + relDrift);
        }
    }
}
