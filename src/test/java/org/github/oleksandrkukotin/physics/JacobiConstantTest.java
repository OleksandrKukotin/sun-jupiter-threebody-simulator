package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.StateVector;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JacobiConstantTest {

    private JacobiConstant jacobiConstant;

    // L4 sits at the equilateral point (0.5 − μ, √3/2); r₁ = r₂ = 1 exactly.
    private static final double X_L4 = 0.5 - PhysicsConstants.MU;
    private static final double Y_L4 = Math.sqrt(3.0) / 2.0;

    // At L4 with zero velocity: C = 2Ω = 3 − μ + μ²  (analytical result)
    private static final double C_L4 = 3.0 - PhysicsConstants.MU + PhysicsConstants.MU * PhysicsConstants.MU;

    @BeforeEach
    void setUp() {
        jacobiConstant = new JacobiConstant();
    }

    @Test
    void compute_atL4WithZeroVelocity_returnsAnalyticalValue() {
        StateVector state = new StateVector(X_L4, Y_L4, 0.0, 0.0);
        assertEquals(C_L4, jacobiConstant.compute(state), 1e-10);
    }

    @Test
    void effectivePotential_atL4_equalsHalfOfJacobiConstant() {
        // C = 2Ω when v = 0, so Ω = C/2
        assertEquals(C_L4 / 2.0, jacobiConstant.effectivePotential(X_L4, Y_L4), 1e-10);
    }

    @Test
    void effectivePotential_isSymmetricAboutXAxis() {
        double x = 0.3, y = 0.5;
        assertEquals(
                jacobiConstant.effectivePotential(x, y),
                jacobiConstant.effectivePotential(x, -y),
                1e-15
        );
    }

    @Test
    void compute_addingVelocity_decreasesJacobiConstant() {
        StateVector atRest = new StateVector(X_L4, Y_L4, 0.0, 0.0);
        StateVector moving = new StateVector(X_L4, Y_L4, 0.1, 0.0);
        assertTrue(jacobiConstant.compute(moving) < jacobiConstant.compute(atRest));
    }
}
