package org.github.oleksandrkukotin.physics;

import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.SimulationRequest;
import org.github.oleksandrkukotin.model.StateVector;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Propagates the state transition matrix Φ(t, 0) alongside the CR3BP state.
 *
 * <p>Φ satisfies the variational equation Φ̇ = A(t)Φ, Φ(0) = I, where A is the
 * Jacobian of the CR3BP vector field evaluated along the reference trajectory:
 * <pre>
 *   A = ⎡ 0    0    1  0 ⎤
 *       ⎢ 0    0    0  1 ⎥
 *       ⎢ Ωxx  Ωxy  0  2 ⎥
 *       ⎣ Ωxy  Ωyy −2  0 ⎦
 * </pre>
 * with Ωxx, Ωxy, Ωyy the second partials of the effective potential Ω.
 *
 * <p>The 4-dimensional state and the 16 matrix entries (row-major) are integrated
 * together as a single 20-dimensional ODE, so Φ stays consistent with the exact
 * trajectory the adaptive integrator follows.
 *
 * <p>The non-collision precondition of {@link CR3BPEquations} applies: as r₁ or
 * r₂ → 0 the 1/r⁵ terms in the second partials diverge even faster than the
 * equations of motion.
 */
@Component
public class StateTransitionMatrix {

    private static final int STATE_DIM = 4;
    private static final int AUGMENTED_DIM = STATE_DIM + STATE_DIM * STATE_DIM;

    private final CR3BPEquations equations;

    public StateTransitionMatrix(CR3BPEquations equations) {
        this.equations = equations;
    }

    /** Final state and the 4×4 state transition matrix Φ(duration, 0). */
    public record Propagation(StateVector finalState, double[][] matrix) {}

    /**
     * Integrates the state and variational equations over the request duration.
     *
     * @param request initial state, duration (may be negative for backward time), and tolerances
     * @return the final state together with Φ(duration, 0)
     */
    public Propagation propagate(SimulationRequest request) {
        DormandPrince853Integrator integrator = new DormandPrince853Integrator(
                request.minStep(), request.maxStep(),
                request.absoluteTolerance(), request.relativeTolerance());

        double[] y0 = new double[AUGMENTED_DIM];
        System.arraycopy(request.initialState().toArray(), 0, y0, 0, STATE_DIM);
        for (int i = 0; i < STATE_DIM; i++) {
            y0[STATE_DIM + i * STATE_DIM + i] = 1.0; // Φ(0) = I
        }

        double[] yOut = new double[AUGMENTED_DIM];
        integrator.integrate(new AugmentedEquations(), 0.0, y0, request.duration(), yOut);

        double[][] phi = new double[STATE_DIM][STATE_DIM];
        for (int i = 0; i < STATE_DIM; i++) {
            System.arraycopy(yOut, STATE_DIM + i * STATE_DIM, phi[i], 0, STATE_DIM);
        }
        return new Propagation(StateVector.fromArray(Arrays.copyOf(yOut, STATE_DIM)), phi);
    }

    /**
     * Jacobian A(x, y) of the CR3BP vector field at the given position.
     * The velocity blocks are constant; only the Ω second partials depend on position.
     */
    public static double[][] variationalMatrix(double x, double y) {
        double mu = PhysicsConstants.MU;
        double oneMu = PhysicsConstants.ONE_MINUS_MU;
        double dx1 = x + mu;
        double dx2 = x - oneMu;

        double r1 = CR3BPUtils.distanceToSun(x, y);
        double r2 = CR3BPUtils.distanceToJupiter(x, y);
        double r1Cubed = r1 * r1 * r1;
        double r2Cubed = r2 * r2 * r2;
        double r1Fifth = r1Cubed * r1 * r1;
        double r2Fifth = r2Cubed * r2 * r2;

        double omegaXX = 1.0 - oneMu / r1Cubed - mu / r2Cubed
                + 3.0 * oneMu * dx1 * dx1 / r1Fifth + 3.0 * mu * dx2 * dx2 / r2Fifth;
        double omegaYY = 1.0 - oneMu / r1Cubed - mu / r2Cubed
                + 3.0 * oneMu * y * y / r1Fifth + 3.0 * mu * y * y / r2Fifth;
        double omegaXY = 3.0 * oneMu * dx1 * y / r1Fifth + 3.0 * mu * dx2 * y / r2Fifth;

        return new double[][]{
                {0.0, 0.0, 1.0, 0.0},
                {0.0, 0.0, 0.0, 1.0},
                {omegaXX, omegaXY, 0.0, 2.0},
                {omegaXY, omegaYY, -2.0, 0.0}
        };
    }

    /** State + variational equations as one first-order system: [x y ẋ ẏ, Φ row-major]. */
    private final class AugmentedEquations implements FirstOrderDifferentialEquations {

        @Override
        public int getDimension() {
            return AUGMENTED_DIM;
        }

        @Override
        public void computeDerivatives(double t, double[] y, double[] yDot) {
            equations.computeDerivatives(t, y, yDot); // only touches indices 0–3
            double[][] a = variationalMatrix(y[0], y[1]);
            for (int i = 0; i < STATE_DIM; i++) {
                for (int j = 0; j < STATE_DIM; j++) {
                    double sum = 0.0;
                    for (int k = 0; k < STATE_DIM; k++) {
                        sum += a[i][k] * y[STATE_DIM + k * STATE_DIM + j];
                    }
                    yDot[STATE_DIM + i * STATE_DIM + j] = sum;
                }
            }
        }
    }
}
