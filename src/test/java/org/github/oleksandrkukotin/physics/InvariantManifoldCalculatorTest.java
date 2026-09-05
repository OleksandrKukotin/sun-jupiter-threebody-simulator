package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.model.LagrangePoint;
import org.github.oleksandrkukotin.model.ManifoldBranch;
import org.github.oleksandrkukotin.model.ManifoldRequest;
import org.github.oleksandrkukotin.model.ManifoldResult;
import org.github.oleksandrkukotin.model.StateVector;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvariantManifoldCalculatorTest {

    private static final double PERTURBATION = 1e-5;
    private static final double DURATION = 6.0;

    private final LagrangePointCalculator lagrangePoints = new LagrangePointCalculator();
    private final InvariantManifoldCalculator calculator = new InvariantManifoldCalculator(
            lagrangePoints, new StateVectorPropagator(new CR3BPEquations(), new JacobiConstant()));

    private static ManifoldRequest request(String id) {
        return new ManifoldRequest(id, PERTURBATION, DURATION, 1e-12, 1e-12, 1e-12, 0.1);
    }

    private static double distance(StateVector state, LagrangePoint point) {
        double dx = state.x() - point.x();
        double dy = state.y() - point.y();
        return Math.sqrt(dx * dx + dy * dy);
    }

    @Test
    void hyperbolicEigenpairSatisfiesLinearizedDynamics() {
        for (LagrangePoint point : List.of(
                lagrangePoints.computeL1(), lagrangePoints.computeL2(), lagrangePoints.computeL3())) {
            double[][] a = StateTransitionMatrix.variationalMatrix(point.x(), point.y());
            double lambda = InvariantManifoldCalculator.unstableEigenvalue(a[2][0], a[3][1]);
            assertTrue(lambda > 0, point.name() + " must have a positive real eigenvalue");
            // Check A·v = λ·v for both the unstable (+λ) and stable (−λ) directions
            for (double eigenvalue : new double[]{lambda, -lambda}) {
                double[] v = InvariantManifoldCalculator.hyperbolicEigenvector(a[2][0], eigenvalue);
                for (int i = 0; i < 4; i++) {
                    double av = 0.0;
                    for (int k = 0; k < 4; k++) {
                        av += a[i][k] * v[k];
                    }
                    assertEquals(eigenvalue * v[i], av, 1e-9,
                            point.name() + " eigenpair residual at component " + i);
                }
            }
        }
    }

    @Test
    void computesFourBranchesForL1() {
        ManifoldResult result = calculator.compute(request("L1"));
        assertEquals("L1", result.lagrangePoint().name());
        assertTrue(result.unstableEigenvalue() > 0);
        assertEquals(List.of("unstable-plus", "unstable-minus", "stable-plus", "stable-minus"),
                result.branches().stream().map(ManifoldBranch::name).toList());
    }

    @Test
    void unstableBranchesDepartFromL1() {
        ManifoldResult result = calculator.compute(request("L1"));
        for (ManifoldBranch branch : result.branches()) {
            if (!branch.name().startsWith("unstable")) continue;
            assertTrue(distance(branch.points().getFirst().state(), result.lagrangePoint()) < 1e-3,
                    branch.name() + " must start near L1");
            assertTrue(distance(branch.points().getLast().state(), result.lagrangePoint()) > 1e-2,
                    branch.name() + " must have left the vicinity of L1");
        }
    }

    @Test
    void stableBranchesAreIntegratedBackward() {
        ManifoldResult result = calculator.compute(request("L1"));
        for (ManifoldBranch branch : result.branches()) {
            if (!branch.name().startsWith("stable")) continue;
            assertEquals(-DURATION, branch.points().getLast().time(), 1e-9);
            assertTrue(distance(branch.points().getLast().state(), result.lagrangePoint()) > 1e-2,
                    branch.name() + " must arrive at L1 from far away");
        }
    }

    @Test
    void jacobiConstantIsConservedAlongEveryBranch() {
        for (String id : List.of("L1", "L2")) {
            ManifoldResult result = calculator.compute(request(id));
            for (ManifoldBranch branch : result.branches()) {
                double first = branch.points().getFirst().jacobiConstant();
                double last = branch.points().getLast().jacobiConstant();
                assertEquals(first, last, 1e-7, id + " " + branch.name());
            }
        }
    }

    @Test
    void triangularPointsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculator.compute(request("L4")));
        assertThrows(IllegalArgumentException.class, () -> calculator.compute(request("L5")));
    }

    @Test
    void unknownPointIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculator.compute(request("L9")));
    }

    @Test
    void nonPositiveParametersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> calculator.compute(
                new ManifoldRequest("L1", 0.0, DURATION, 1e-12, 1e-12, 1e-12, 0.1)));
        assertThrows(IllegalArgumentException.class, () -> calculator.compute(
                new ManifoldRequest("L1", PERTURBATION, -1.0, 1e-12, 1e-12, 1e-12, 0.1)));
    }
}
