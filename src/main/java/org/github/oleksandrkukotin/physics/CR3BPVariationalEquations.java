package org.github.oleksandrkukotin.physics;

import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.StateVector;
import org.springframework.stereotype.Component;

/**
 * Augmented CR3BP + State Transition Matrix (STM) equations.
 *
 * <p>State layout (dimension 20):
 * <pre>
 *   y[0..3]  = [x, y, xDot, yDot]                  (same as CR3BPEquations)
 *   y[4..19] = Phi, the 4x4 STM, flattened ROW-MAJOR:
 *              Phi[i][j] = y[4 + i*4 + j]
 * </pre>
 * The y[0..3] block's derivative is delegated to {@link CR3BPEquations#computeDerivatives}
 * rather than duplicated here, so the augmented (state + STM) trajectory follows the exact
 * same dynamics as a plain propagation.
 *
 * <p>The STM obeys the variational equation {@code dPhi/dt = A(x,y) * Phi},
 * where A is the Jacobian of the state derivative:
 * <pre>
 *   A = [[  0,    0,   1,  0],
 *        [  0,    0,   0,  1],
 *        [Uxx,  Uxy,   0,  2],
 *        [Uxy,  Uyy,  -2,  0]]
 * </pre>
 * with (second partials of the effective potential — see JacobiConstant#effectivePotential):
 * <pre>
 *   Uxx = 1 - (1-mu)/r1^3 - mu/r2^3
 *         + 3*(1-mu)*(x+mu)^2 / r1^5
 *         + 3*mu*(x-(1-mu))^2 / r2^5
 *
 *   Uyy = 1 - (1-mu)/r1^3 - mu/r2^3
 *         + 3*(1-mu)*y^2            / r1^5
 *         + 3*mu*y^2                / r2^5
 *
 *   Uxy = 3*(1-mu)*(x+mu)*y / r1^5 + 3*mu*(x-(1-mu))*y / r2^5
 * </pre>
 * r1 = distanceToSun(x,y), r2 = distanceToJupiter(x,y) — reuse {@link CR3BPUtils}.
 *
 * <p>Correctness is cross-checked in {@code CR3BPVariationalEquationsTest} against a
 * central-finite-difference Jacobian of {@link CR3BPEquations#computeDerivatives}.
 */
@Component
public class CR3BPVariationalEquations implements FirstOrderDifferentialEquations {

    private static final int STATE_DIM = 4;
    private static final int STM_DIM = 16;

    private final CR3BPEquations equations;

    public CR3BPVariationalEquations(CR3BPEquations equations) {
        this.equations = equations;
    }

    @Override
    public int getDimension() {
        return STATE_DIM + STM_DIM;
    }

    @Override
    public void computeDerivatives(double t, double[] y, double[] yDot) {
        double[] state = {y[0], y[1], y[2], y[3]};
        double[] stateDot = new double[STATE_DIM];
        equations.computeDerivatives(t, state, stateDot);
        System.arraycopy(stateDot, 0, yDot, 0, STATE_DIM);

        double[][] a = jacobianAt(y[0], y[1]);

        for (int i = 0; i < STATE_DIM; i++) {
            for (int j = 0; j < STATE_DIM; j++) {
                double dPhiIj = 0.0;
                for (int k = 0; k < STATE_DIM; k++) {
                    dPhiIj += a[i][k] * y[4 + k * STATE_DIM + j];
                }
                yDot[4 + i * STATE_DIM + j] = dPhiIj;
            }
        }
    }

    /**
     * The Jacobian A(x,y) of the CR3BP state derivative, evaluated at (x, y) — velocity
     * doesn't affect A since the CR3BP equations are linear in velocity. Shared by
     * {@link #computeDerivatives} and {@link LyapunovOrbitFinder}, which needs A at a
     * Lagrange point to build the linear-theory seed for its differential corrector.
     */
    static double[][] jacobianAt(double x, double y) {
        double r1 = CR3BPUtils.distanceToSun(x, y);
        double r2 = CR3BPUtils.distanceToJupiter(x, y);
        double r1_3 = r1 * r1 * r1;
        double r1_5 = r1_3 * r1 * r1;
        double r2_3 = r2 * r2 * r2;
        double r2_5 = r2_3 * r2 * r2;
        double mu = PhysicsConstants.MU;
        double oneMinusMu = PhysicsConstants.ONE_MINUS_MU;

        double uxx = 1 - oneMinusMu / r1_3 - mu / r2_3
                + 3 * oneMinusMu * (x + mu) * (x + mu) / r1_5
                + 3 * mu * (x - oneMinusMu) * (x - oneMinusMu) / r2_5;
        double uxy = 3 * oneMinusMu * (x + mu) * y / r1_5
                + 3 * mu * (x - oneMinusMu) * y / r2_5;
        double uyy = 1 - oneMinusMu / r1_3 - mu / r2_3
                + 3 * oneMinusMu * y * y / r1_5
                + 3 * mu * y * y / r2_5;

        return new double[][]{
                {0, 0, 1, 0},
                {0, 0, 0, 1},
                {uxx, uxy, 0, 2},
                {uxy, uyy, -2, 0}
        };
    }

    public static double[] initialAugmentedState(StateVector state) {
        double[] result = new double[STATE_DIM + STM_DIM];
        System.arraycopy(state.toArray(), 0, result, 0, STATE_DIM);
        for (int i = 0; i < STATE_DIM; i++) {
            result[4 + i * STATE_DIM + i] = 1.0;
        }
        return result;
    }
}
