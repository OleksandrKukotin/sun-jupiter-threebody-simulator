package org.github.oleksandrkukotin.physics;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.github.oleksandrkukotin.model.SimulationRequest;
import org.github.oleksandrkukotin.model.StateVector;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StateTransitionMatrixTest {

    // Generic off-axis state in the tadpole region: mild growth, far from both primaries
    private static final StateVector REFERENCE_STATE = new StateVector(0.5, 0.4, 0.05, -0.02);

    private final StateTransitionMatrix stm = new StateTransitionMatrix(new CR3BPEquations());

    private static SimulationRequest request(StateVector state, double duration) {
        return new SimulationRequest(state, duration, 1e-12, 1e-12, 1e-12, 0.1);
    }

    @Test
    void secondPartialsMatchFiniteDifferencesOfEffectivePotential() {
        JacobiConstant jacobi = new JacobiConstant();
        double x = 0.3, y = 0.2, h = 1e-4;
        double[][] a = StateTransitionMatrix.variationalMatrix(x, y);

        double omegaXX = (jacobi.effectivePotential(x + h, y) - 2 * jacobi.effectivePotential(x, y)
                + jacobi.effectivePotential(x - h, y)) / (h * h);
        double omegaYY = (jacobi.effectivePotential(x, y + h) - 2 * jacobi.effectivePotential(x, y)
                + jacobi.effectivePotential(x, y - h)) / (h * h);
        double omegaXY = (jacobi.effectivePotential(x + h, y + h) - jacobi.effectivePotential(x + h, y - h)
                - jacobi.effectivePotential(x - h, y + h) + jacobi.effectivePotential(x - h, y - h)) / (4 * h * h);

        // FD truncation error is h²/12 · Ω⁗ ≈ 1e-6 at this point; the analytic values are exact
        assertEquals(omegaXX, a[2][0], 1e-5);
        assertEquals(omegaYY, a[3][1], 1e-5);
        assertEquals(omegaXY, a[2][1], 1e-5);
        assertEquals(a[2][1], a[3][0], 0.0); // Ωxy is symmetric
    }

    @Test
    void shortDurationMatchesLinearExpansion() {
        double dt = 1e-4;
        double[][] phi = stm.propagate(request(REFERENCE_STATE, dt)).matrix();
        double[][] a = StateTransitionMatrix.variationalMatrix(REFERENCE_STATE.x(), REFERENCE_STATE.y());
        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                double expected = (i == j ? 1.0 : 0.0) + a[i][j] * dt; // Φ(dt) = I + A·dt + O(dt²)
                assertEquals(expected, phi[i][j], 1e-5);
            }
        }
    }

    @Test
    void determinantIsOneByLiouville() {
        // trace(A) = 0 for all t, so det Φ(t) = exp(∫trace) = 1 exactly
        double[][] phi = stm.propagate(request(REFERENCE_STATE, 5.0)).matrix();
        double det = new LUDecomposition(new Array2DRowRealMatrix(phi)).getDeterminant();
        assertEquals(1.0, det, 1e-8);
    }

    @Test
    void preservesSymplecticForm() {
        // In canonical coordinates (x, y, px, py) with px = ẋ − y, py = ẏ + x, the STM
        // satisfies Φcᵀ J Φc = J. Pulled back to (x, y, ẋ, ẏ): Φᵀ (TᵀJT) Φ = TᵀJT.
        RealMatrix phi = new Array2DRowRealMatrix(stm.propagate(request(REFERENCE_STATE, 5.0)).matrix());
        RealMatrix t = new Array2DRowRealMatrix(new double[][]{
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, -1, 1, 0},
                {1, 0, 0, 1}});
        RealMatrix j = new Array2DRowRealMatrix(new double[][]{
                {0, 0, 1, 0},
                {0, 0, 0, 1},
                {-1, 0, 0, 0},
                {0, -1, 0, 0}});
        RealMatrix k = t.transpose().multiply(j).multiply(t);
        RealMatrix residual = phi.transpose().multiply(k).multiply(phi).subtract(k);
        assertEquals(0.0, residual.getNorm(), 1e-7);
    }

    @Test
    void matchesCentralFiniteDifferences() {
        double duration = 2.0, delta = 1e-6;
        double[][] phi = stm.propagate(request(REFERENCE_STATE, duration)).matrix();
        for (int j = 0; j < 4; j++) {
            double[] plus = REFERENCE_STATE.toArray();
            double[] minus = REFERENCE_STATE.toArray();
            plus[j] += delta;
            minus[j] -= delta;
            double[] finalPlus = stm.propagate(request(StateVector.fromArray(plus), duration)).finalState().toArray();
            double[] finalMinus = stm.propagate(request(StateVector.fromArray(minus), duration)).finalState().toArray();
            for (int i = 0; i < 4; i++) {
                double column = (finalPlus[i] - finalMinus[i]) / (2 * delta);
                assertEquals(column, phi[i][j], 1e-5);
            }
        }
    }
}
