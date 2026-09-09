package org.github.oleksandrkukotin.physics;

import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.github.oleksandrkukotin.model.PeriodicOrbit;
import org.github.oleksandrkukotin.model.StateVector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LyapunovOrbitFinderTest {

    private final LagrangePointCalculator lagrangePointCalculator = new LagrangePointCalculator();
    private final CR3BPEquations equations = new CR3BPEquations();
    private final CR3BPVariationalEquations variationalEquations = new CR3BPVariationalEquations(equations);
    private final JacobiConstant jacobiConstant = new JacobiConstant();
    private final LyapunovOrbitFinder finder = new LyapunovOrbitFinder(
            lagrangePointCalculator, equations, variationalEquations, jacobiConstant);

    @Test
    void findLyapunovOrbit_rejectsTriangularPoints() {
        assertThrows(IllegalArgumentException.class, () -> finder.findLyapunovOrbit("L4", 1e-3));
        assertThrows(IllegalArgumentException.class, () -> finder.findLyapunovOrbit("L5", 1e-3));
    }

    @Test
    void findLyapunovOrbit_rejectsUnknownPoint() {
        assertThrows(IllegalArgumentException.class, () -> finder.findLyapunovOrbit("L6", 1e-3));
    }

    /**
     * The corrector's whole purpose is to make the trajectory close on itself after one
     * period. This propagates the converged initial state for exactly that period using
     * the plain (non-augmented) CR3BP equations and checks it returns to where it started.
     */
    @Test
    void findLyapunovOrbit_convergesToAPeriodicOrbitNearL1() {
        double amplitude = 1e-3;
        PeriodicOrbit orbit = finder.findLyapunovOrbit("L1", amplitude);

        assertEquals("L1", orbit.lagrangePoint());
        assertEquals(amplitude, orbit.amplitude(), 1e-15);
        assertTrue(orbit.period() > 0);

        double[] initialState = orbit.initialState().toArray();
        double[] finalState = propagateOnePeriod(orbit.initialState(), orbit.period());

        for (int i = 0; i < 4; i++) {
            assertEquals(initialState[i], finalState[i], 1e-6,
                    "component " + i + " should return to its initial value after one period");
        }
    }

    @Test
    void continueFamily_producesRequestedNumberOfOrbitsWithIncreasingAmplitude() {
        // Step size is a fraction of the start amplitude: natural-parameter continuation
        // (unlike pseudo-arc-length) has a limited basin of convergence per step, and a
        // 100%-of-amplitude jump this close to L1 is enough to throw Newton's method
        // outside it.
        double startAmplitude = 1e-3;
        double stepAmplitude = 2e-4;
        List<PeriodicOrbit> family = finder.continueFamily("L1", startAmplitude, stepAmplitude, 4);

        assertEquals(4, family.size());
        for (int i = 0; i < family.size(); i++) {
            PeriodicOrbit orbit = family.get(i);
            assertEquals("L1", orbit.lagrangePoint());
            assertEquals(startAmplitude + stepAmplitude * i, orbit.amplitude(), 1e-12);

            double[] initialState = orbit.initialState().toArray();
            double[] finalState = propagateOnePeriod(orbit.initialState(), orbit.period());
            for (int j = 0; j < 4; j++) {
                assertEquals(initialState[j], finalState[j], 1e-5,
                        "orbit at amplitude " + orbit.amplitude() + " should be periodic in component " + j);
            }
        }
    }

    private double[] propagateOnePeriod(StateVector initialState, double duration) {
        DormandPrince853Integrator integrator = new DormandPrince853Integrator(1e-12, 0.5, 1e-13, 1e-13);
        double[] finalState = new double[4];
        integrator.integrate(equations, 0.0, initialState.toArray(), duration, finalState);
        return finalState;
    }
}
