package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.LagrangePoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LagrangePointCalculatorTest {

    private LagrangePointCalculator calculator;

    private static final double MU     = PhysicsConstants.MU;
    private static final double ONE_MU = PhysicsConstants.ONE_MINUS_MU;

    @BeforeEach
    void setUp() {
        calculator = new LagrangePointCalculator();
    }

    // --- L1 ---

    @Test
    void computeL1_liesOnXAxis() {
        LagrangePoint l1 = calculator.computeL1();
        assertEquals(0.0, l1.y(), 1e-15);
    }

    @Test
    void computeL1_liesBetweenSunAndJupiter() {
        LagrangePoint l1 = calculator.computeL1();
        // Sun (primary) at −μ, Jupiter (secondary) at 1−μ
        assertTrue(l1.x() > -MU,    "L1 must be to the right of the Sun");
        assertTrue(l1.x() < ONE_MU, "L1 must be to the left of Jupiter");
    }

    @Test
    void computeL1_satisfiesEquilibriumCondition() {
        // f(x) = x − (1−μ)/(x+μ)² + μ/(1−μ−x)² = 0 at L1
        LagrangePoint l1 = calculator.computeL1();
        double x  = l1.x();
        double r1 = x + MU;
        double r2 = ONE_MU - x;
        double fx = x - ONE_MU / (r1 * r1) + MU / (r2 * r2);
        assertEquals(0.0, fx, 1e-12);
    }
}
