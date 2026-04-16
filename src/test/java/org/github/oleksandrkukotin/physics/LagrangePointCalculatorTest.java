package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.LagrangePoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

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

    // --- L2 ---

    @Test
    void computeL2_liesOnXAxis() {
        LagrangePoint l2 = calculator.computeL2();
        assertEquals(0.0, l2.y(), 1e-15);
    }

    @Test
    void computeL2_liesBeyondJupiter() {
        LagrangePoint l2 = calculator.computeL2();
        assertTrue(l2.x() > ONE_MU, "L2 must be to the right of Jupiter");
        assertTrue(l2.x() < 1.1,    "L2 must be near Jupiter, not far downrange");
    }

    @Test
    void computeL2_matchesKnownSunJupiterValue() {
        // Literature value for Sun–Jupiter L2 ≈ 1.06883 in normalized units
        LagrangePoint l2 = calculator.computeL2();
        assertEquals(1.06883, l2.x(), 1e-4);
    }

    @Test
    void computeL2_satisfiesEquilibriumCondition() {
        // f(x) = x − (1−μ)/(x+μ)² − μ/(x−(1−μ))² = 0 at L2
        LagrangePoint l2 = calculator.computeL2();
        double x  = l2.x();
        double r1 = x + MU;
        double r2 = x - ONE_MU;
        double fx = x - ONE_MU / (r1 * r1) - MU / (r2 * r2);
        assertEquals(0.0, fx, 1e-12);
    }

    // --- L3 ---

    @Test
    void computeL3_liesOnXAxis() {
        LagrangePoint l3 = calculator.computeL3();
        assertEquals(0.0, l3.y(), 1e-15);
    }

    @Test
    void computeL3_liesBeyondSunOppositeJupiter() {
        LagrangePoint l3 = calculator.computeL3();
        assertTrue(l3.x() < -MU, "L3 must be to the left of the Sun");
        assertTrue(l3.x() > -1.1, "L3 must be near the Sun–Jupiter mirror distance");
    }

    @Test
    void computeL3_matchesKnownSunJupiterValue() {
        // Classical approximation: x_L3 ≈ −(1 + 5μ/12) ≈ −1.00039732 for Sun–Jupiter μ
        LagrangePoint l3 = calculator.computeL3();
        assertEquals(-1.00039732, l3.x(), 1e-4);
    }

    @Test
    void computeL3_satisfiesEquilibriumCondition() {
        // For x < −μ: f(x) = x + (1−μ)/(x+μ)² + μ/((1−μ)−x)² = 0 at L3
        LagrangePoint l3 = calculator.computeL3();
        double x  = l3.x();
        double r1 = -(x + MU);
        double r2 = ONE_MU - x;
        double fx = x + ONE_MU / (r1 * r1) + MU / (r2 * r2);
        assertEquals(0.0, fx, 1e-12);
    }

    // --- L4 ---

    @Test
    void computeL4_hasExpectedCoordinates() {
        LagrangePoint l4 = calculator.computeL4();
        assertEquals(0.5 - MU, l4.x(), 1e-15);
        assertEquals(Math.sqrt(3) / 2.0, l4.y(), 1e-15);
    }

    @Test
    void computeL4_isEquidistantFromBothPrimaries() {
        // L4 forms an equilateral triangle with Sun (−μ, 0) and Jupiter (1−μ, 0);
        // both distances must equal the primaries' unit separation.
        LagrangePoint l4 = calculator.computeL4();
        double dSun     = Math.hypot(l4.x() + MU,     l4.y());
        double dJupiter = Math.hypot(l4.x() - ONE_MU, l4.y());
        assertEquals(1.0, dSun,     1e-15);
        assertEquals(1.0, dJupiter, 1e-15);
    }

    @Test
    void computeL4_liesAboveXAxis() {
        assertTrue(calculator.computeL4().y() > 0.0, "L4 is the leading (y > 0) triangular point");
    }

    // --- L5 ---

    @Test
    void computeL5_hasExpectedCoordinates() {
        LagrangePoint l5 = calculator.computeL5();
        assertEquals(0.5 - MU, l5.x(), 1e-15);
        assertEquals(-Math.sqrt(3) / 2.0, l5.y(), 1e-15);
    }

    @Test
    void computeL5_isEquidistantFromBothPrimaries() {
        LagrangePoint l5 = calculator.computeL5();
        double dSun     = Math.hypot(l5.x() + MU,     l5.y());
        double dJupiter = Math.hypot(l5.x() - ONE_MU, l5.y());
        assertEquals(1.0, dSun,     1e-15);
        assertEquals(1.0, dJupiter, 1e-15);
    }

    @Test
    void computeL5_liesBelowXAxis() {
        assertTrue(calculator.computeL5().y() < 0.0, "L5 is the trailing (y < 0) triangular point");
    }

    // --- computeAll ---

    @Test
    void computeAll_returnsFivePointsInOrderL1ThroughL5() {
        List<LagrangePoint> all = calculator.computeAll();
        assertEquals(5, all.size());

        LagrangePoint[] expected = {
                calculator.computeL1(), calculator.computeL2(), calculator.computeL3(),
                calculator.computeL4(), calculator.computeL5()
        };
        String[] names = {"L1", "L2", "L3", "L4", "L5"};
        for (int i = 0; i < 5; i++) {
            LagrangePoint actual = all.get(i);
            assertEquals(names[i],       actual.name());
            assertEquals(expected[i].x(), actual.x(), 1e-15);
            assertEquals(expected[i].y(), actual.y(), 1e-15);
        }
    }

    @Test
    void computeL4AndL5_areMirrorImagesAcrossXAxis() {
        LagrangePoint l4 = calculator.computeL4();
        LagrangePoint l5 = calculator.computeL5();
        assertEquals(l4.x(),  l5.x(), 1e-15);
        assertEquals(l4.y(), -l5.y(), 1e-15);
    }
}
