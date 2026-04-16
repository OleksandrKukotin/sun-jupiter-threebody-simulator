package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CR3BPEquationsTest {

    private CR3BPEquations equations;

    @BeforeEach
    void setUp() {
        equations = new CR3BPEquations();
    }

    @Test
    void getDimension_returnsFour() {
        assertEquals(4, equations.getDimension());
    }

    /**
     * At L4 (equilateral point) with zero velocity the net acceleration must be zero —
     * L4 is an equilibrium of the CR3BP by definition.
     *
     * L4 = (0.5 − μ, √3/2), r₁ = r₂ = 1 exactly.
     */
    @Test
    void computeDerivatives_atL4WithZeroVelocity_accelerationsAreZero() {
        double x = 0.5 - PhysicsConstants.MU;
        double y = Math.sqrt(3.0) / 2.0;
        double[] state = {x, y, 0.0, 0.0};
        double[] yDot  = new double[4];

        equations.computeDerivatives(0.0, state, yDot);

        assertEquals(0.0, yDot[0], 1e-15, "ẋ should equal xDot input (0)");
        assertEquals(0.0, yDot[1], 1e-15, "ẏ should equal yDot input (0)");
        assertEquals(0.0, yDot[2], 1e-10, "ẍ at L4 equilibrium should be 0");
        assertEquals(0.0, yDot[3], 1e-10, "ÿ at L4 equilibrium should be 0");
    }

    /**
     * Velocity derivatives (slots 0 and 1) must simply mirror the input velocities,
     * regardless of position.
     */
    @Test
    void computeDerivatives_velocitySlotsMirrorInput() {
        double[] state = {0.3, 0.4, 1.5, -2.3};
        double[] yDot  = new double[4];

        equations.computeDerivatives(0.0, state, yDot);

        assertEquals(1.5,  yDot[0], 1e-15);
        assertEquals(-2.3, yDot[1], 1e-15);
    }

    /**
     * The y-acceleration ÿ is antisymmetric about the x-axis:
     * ÿ(x, y) = −ÿ(x, −y).
     * This follows from the y-symmetry of the CR3BP potential.
     */
    @Test
    void computeDerivatives_yAcceleration_isAntisymmetricAboutXAxis() {
        double[] statePos = {0.3,  0.5, 0.0, 0.0};
        double[] stateNeg = {0.3, -0.5, 0.0, 0.0};
        double[] yDotPos  = new double[4];
        double[] yDotNeg  = new double[4];

        equations.computeDerivatives(0.0, statePos, yDotPos);
        equations.computeDerivatives(0.0, stateNeg, yDotNeg);

        assertEquals(yDotPos[3], -yDotNeg[3], 1e-15);
    }

    /**
     * The x-acceleration ẍ is symmetric about the x-axis:
     * ẍ(x, y) = ẍ(x, −y).
     */
    @Test
    void computeDerivatives_xAcceleration_isSymmetricAboutXAxis() {
        double[] statePos = {0.3,  0.5, 0.0, 0.0};
        double[] stateNeg = {0.3, -0.5, 0.0, 0.0};
        double[] yDotPos  = new double[4];
        double[] yDotNeg  = new double[4];

        equations.computeDerivatives(0.0, statePos, yDotPos);
        equations.computeDerivatives(0.0, stateNeg, yDotNeg);

        assertEquals(yDotPos[2], yDotNeg[2], 1e-15);
    }

    /**
     * Jacobi constant consistency: the time-derivative of C along a trajectory must be zero.
     * dC/dt = ∂C/∂state · ẏ = 0 — verified by a finite-difference step.
     *
     * We integrate one tiny Euler step and check C is conserved to first order.
     */
    @Test
    void computeDerivatives_jacobiConstantIsConserved_overTinyStep() {
        JacobiConstant jacobi = new JacobiConstant();
        double[] state = {0.3, 0.5, 0.1, -0.2};
        double[] yDot  = new double[4];
        equations.computeDerivatives(0.0, state, yDot);

        double dt = 1e-6;
        double[] next = {
            state[0] + yDot[0] * dt,
            state[1] + yDot[1] * dt,
            state[2] + yDot[2] * dt,
            state[3] + yDot[3] * dt
        };

        org.github.oleksandrkukotin.model.StateVector sv0 =
                org.github.oleksandrkukotin.model.StateVector.fromArray(state);
        org.github.oleksandrkukotin.model.StateVector sv1 =
                org.github.oleksandrkukotin.model.StateVector.fromArray(next);

        assertEquals(jacobi.compute(sv0), jacobi.compute(sv1), 1e-9,
                "Jacobi constant must be conserved over a tiny Euler step");
    }
}
