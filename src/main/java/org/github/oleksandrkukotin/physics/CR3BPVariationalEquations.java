package org.github.oleksandrkukotin.physics;

import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.StateVector;
import org.springframework.stereotype.Component;

/**
 * PSEUDOCODE — augmented CR3BP + State Transition Matrix (STM) equations.
 *
 * <p>State layout (dimension 20):
 * <pre>
 *   y[0..3]  = [x, y, xDot, yDot]                  (same as CR3BPEquations)
 *   y[4..19] = Phi, the 4x4 STM, flattened ROW-MAJOR:
 *              Phi[i][j] = y[4 + i*4 + j]
 * </pre>
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
 * <p><b>Correctness check once implemented:</b> pick a handful of random (x, y)
 * away from the primaries, compute A analytically here, and compare each entry
 * against a central-finite-difference Jacobian of
 * {@link CR3BPEquations#computeDerivatives} (perturb each of x, y, xDot, yDot by
 * +-1e-6 and divide). They should agree to ~1e-8. Do this as a unit test before
 * trusting anything built on top (the Lyapunov corrector will silently produce
 * garbage if a sign here is wrong).
 */
@Component
public class CR3BPVariationalEquations implements FirstOrderDifferentialEquations {

    private static final int STATE_DIM = 4;
    private static final int STM_DIM = 16;

    @Override
    public int getDimension() {
        return STATE_DIM + STM_DIM;
    }

    @Override
    public void computeDerivatives(double t, double[] y, double[] yDot) {
        double x = y[0];
        double yPos = y[1];
        double r1 = CR3BPUtils.distanceToSun(x, yPos);
        double r2 = CR3BPUtils.distanceToJupiter(x, yPos);
        double xDot = y[2];
        double yDot_ = y[3];
        yDot[0] = xDot;
        yDot[1] = yDot_;

        double r1_3 = r1 * r1 * r1;
        double r1_5 = r1_3 * r1 * r1;
        double r2_3 = r2 * r2 * r2;
        double r2_5 = r2_3 * r2 * r2;

        double Uxx = 1 - (PhysicsConstants.ONE_MINUS_MU / r1_3) - PhysicsConstants.MU / r2_3 +
                3 * PhysicsConstants.ONE_MINUS_MU * (x + PhysicsConstants.MU) * (x + PhysicsConstants.MU) / r1_5 +
                3 * PhysicsConstants.MU * (x - PhysicsConstants.ONE_MINUS_MU) * (x - PhysicsConstants.ONE_MINUS_MU) / r2_5;
        double Uxy = 3 * PhysicsConstants.ONE_MINUS_MU * (x + PhysicsConstants.MU) * yPos / r1_5
                + 3 * PhysicsConstants.MU * (x - PhysicsConstants.ONE_MINUS_MU) * yPos / r2_5;
        double Uyy = 1 - PhysicsConstants.ONE_MINUS_MU / r1_3 - PhysicsConstants.MU/r2_3
                + 3 * PhysicsConstants.ONE_MINUS_MU * (yPos * yPos) / r1_5
                + 3 * PhysicsConstants.MU * (yPos*yPos) / r2_5;

        double[][] A = { {0, 0, 1, 0}, {0, 0, 0, 1}, {Uxx, Uxy, 0, 2}, {Uxy, Uyy, -2, 0} };
        int matrixSize = 4;
        double[][] Phi = new double[matrixSize][matrixSize];
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                Phi[i][j] = y[4 + i*4 + j];
            }
        }
        double[][] dPhi = new double[matrixSize][matrixSize];
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                for (int k = 0; k < matrixSize; k++) {
                    dPhi[i][j] += A[i][k] * Phi[k][j];
                }
            }
        }

        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                yDot[4 + i*4 +j] = dPhi[i][j];
            }
        }
    }

    public static double[] initialAugmentedState(StateVector state) {
        double[] result = new double[20];
        System.arraycopy(state.toArray(), 0, result, 0, 4);
        int matrixSize = 4;
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                result[4 + i * 4 + j] = (i == j) ? 1.0 : 0.0;
            }
        }
        return result;
    }
}
