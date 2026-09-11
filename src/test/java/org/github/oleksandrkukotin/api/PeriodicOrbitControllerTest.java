package org.github.oleksandrkukotin.api;

import org.github.oleksandrkukotin.model.PeriodicOrbitResult;
import org.github.oleksandrkukotin.physics.CR3BPEquations;
import org.github.oleksandrkukotin.physics.CR3BPVariationalEquations;
import org.github.oleksandrkukotin.physics.JacobiConstant;
import org.github.oleksandrkukotin.physics.LagrangePointCalculator;
import org.github.oleksandrkukotin.physics.LyapunovOrbitFinder;
import org.github.oleksandrkukotin.physics.StateVectorPropagator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PeriodicOrbitControllerTest {

    private final CR3BPEquations equations = new CR3BPEquations();
    private final JacobiConstant jacobiConstant = new JacobiConstant();
    private final LyapunovOrbitFinder finder = new LyapunovOrbitFinder(
            new LagrangePointCalculator(), equations, new CR3BPVariationalEquations(equations), jacobiConstant);
    private final StateVectorPropagator propagator = new StateVectorPropagator(equations, jacobiConstant);
    private final PeriodicOrbitController controller = new PeriodicOrbitController(finder, propagator);

    @Test
    void getPeriodicOrbit_returnsOrbitWithATrajectoryClosingOnItself() {
        PeriodicOrbitResult result = controller.getPeriodicOrbit("L1", "lyapunov", 1e-3);

        assertEquals("L1", result.orbit().lagrangePoint());
        assertTrue(result.points().size() > 1);

        double[] initial = result.orbit().initialState().toArray();
        double[] last = result.points().get(result.points().size() - 1).state().toArray();
        for (int i = 0; i < 4; i++) {
            assertEquals(initial[i], last[i], 1e-6, "component " + i + " should return to its initial value");
        }
    }

    @Test
    void getPeriodicOrbit_rejectsUnknownFamily() {
        assertThrows(IllegalArgumentException.class,
                () -> controller.getPeriodicOrbit("L1", "halo", 1e-3));
    }
}
