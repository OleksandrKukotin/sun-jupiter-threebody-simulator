package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.model.StateVector;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CR3BPVariationalEquationsTest {

    private final CR3BPVariationalEquations variational = new CR3BPVariationalEquations();
    private final CR3BPEquations equations = new CR3BPEquations();

    @Test
    void getDimension_returnsTwenty() {
        assertEquals(20, variational.getDimension());
    }

    @Test
    void initialAugmentedState_packsStateAndIdentitySTM() {
        StateVector state = new StateVector(0.3, 0.5, 0.1, -0.2);
        double[] augmented = CR3BPVariationalEquations.initialAugmentedState(state);

        assertEquals(20, augmented.length);
        assertEquals(state.x(), augmented[0], 1e-15);
        assertEquals(state.y(), augmented[1], 1e-15);
        assertEquals(state.xDot(), augmented[2], 1e-15);
        assertEquals(state.yDot(), augmented[3], 1e-15);

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                double expected = (i == j) ? 1.0 : 0.0;
                assertEquals(expected, augmented[4 + i * 4 + j], 1e-15,
                        "Phi(0) must be the identity at [" + i + "][" + j + "]");
            }
        }
    }

    /**
     * Cross-checks the analytical Jacobian A(x,y) baked into
     * {@link CR3BPVariationalEquations#computeDerivatives} against a
     * central-finite-difference Jacobian of {@link CR3BPEquations#computeDerivatives},
     * as prescribed by this class's own Javadoc.
     *
     * <p>Feeding Phi = I as the STM block isolates A directly: since
     * dPhi/dt = A * Phi, with Phi = I the returned STM-derivative block equals A.
     */
    @Test
    void computeDerivatives_jacobianMatchesFiniteDifference_atMultiplePoints() {
        double[][] offAxisStates = {
                {0.3, 0.5, 0.1, -0.2},
                {0.7, -0.4, 0.0, 0.0},
                {-0.2, 0.9, 0.05, 0.05},
                {1.2, 0.1, -0.3, 0.4}
        };

        for (double[] state : offAxisStates) {
            double[] analyticalA = extractA(state);
            double[][] numericalA = finiteDifferenceJacobian(state);

            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    assertEquals(numericalA[i][j], analyticalA[i * 4 + j], 1e-6,
                            "A[" + i + "][" + j + "] mismatch at state "
                                    + Arrays.toString(state));
                }
            }
        }
    }

    private double[] extractA(double[] state) {
        double[] y = new double[20];
        System.arraycopy(state, 0, y, 0, 4);
        for (int i = 0; i < 4; i++) {
            y[4 + i * 4 + i] = 1.0;
        }
        double[] yDot = new double[20];
        variational.computeDerivatives(0.0, y, yDot);
        return Arrays.copyOfRange(yDot, 4, 20);
    }

    private double[][] finiteDifferenceJacobian(double[] state) {
        double h = 1e-6;
        double[][] jacobian = new double[4][4];

        for (int col = 0; col < 4; col++) {
            double[] plus = state.clone();
            double[] minus = state.clone();
            plus[col] += h;
            minus[col] -= h;

            double[] dPlus = new double[4];
            double[] dMinus = new double[4];
            equations.computeDerivatives(0.0, plus, dPlus);
            equations.computeDerivatives(0.0, minus, dMinus);

            for (int row = 0; row < 4; row++) {
                jacobian[row][col] = (dPlus[row] - dMinus[row]) / (2 * h);
            }
        }

        return jacobian;
    }
}
