package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.model.LagrangePoint;
import org.github.oleksandrkukotin.model.ManifoldBranch;
import org.github.oleksandrkukotin.model.ManifoldRequest;
import org.github.oleksandrkukotin.model.ManifoldResult;
import org.github.oleksandrkukotin.model.SimulationRequest;
import org.github.oleksandrkukotin.model.StateVector;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Computes the stable and unstable invariant manifold branches of the collinear
 * Lagrange points (L1, L2, L3).
 *
 * <p>At a collinear point (y = 0, so Ωxy = 0) the linearization has the
 * characteristic polynomial
 * <pre>
 *   λ⁴ + (4 − Ωxx − Ωyy)λ² + ΩxxΩyy = 0
 * </pre>
 * Since Ωxx &gt; 0 and Ωyy &lt; 0 there, the product ΩxxΩyy &lt; 0 guarantees one
 * real pair ±λ (saddle) alongside the imaginary center pair. The eigenvector for
 * a real eigenvalue λ is, up to scale,
 * <pre>
 *   v = [1, (λ² − Ωxx)/(2λ), λ, (λ² − Ωxx)/2]
 * </pre>
 *
 * <p>Each branch is seeded by displacing the equilibrium by ±ε along the
 * eigenvector; unstable branches (+λ) are integrated forward in time, stable
 * branches (−λ) backward, so both trace the manifold's global geometry.
 *
 * <p>L4 and L5 are linearly stable for the Sun–Jupiter mass ratio (μ is far below
 * Routh's critical value ≈ 0.03852): they have no hyperbolic eigenvalues, hence
 * no stable/unstable manifolds, and are rejected.
 */
@Component
public class InvariantManifoldCalculator {

    private final LagrangePointCalculator lagrangePointCalculator;
    private final StateVectorPropagator propagator;

    public InvariantManifoldCalculator(LagrangePointCalculator lagrangePointCalculator,
                                       StateVectorPropagator propagator) {
        this.lagrangePointCalculator = lagrangePointCalculator;
        this.propagator = propagator;
    }

    /** Computes all four manifold branches for the requested collinear point. */
    public ManifoldResult compute(ManifoldRequest request) {
        if (request.perturbation() <= 0) {
            throw new IllegalArgumentException("perturbation must be positive");
        }
        if (request.duration() <= 0) {
            throw new IllegalArgumentException("duration must be positive");
        }
        LagrangePoint point = findCollinearPoint(request.lagrangePointId());

        double[][] a = StateTransitionMatrix.variationalMatrix(point.x(), point.y());
        double omegaXX = a[2][0];
        double omegaYY = a[3][1];
        double lambda = unstableEigenvalue(omegaXX, omegaYY);
        double[] unstableDirection = hyperbolicEigenvector(omegaXX, lambda);
        double[] stableDirection = hyperbolicEigenvector(omegaXX, -lambda);

        List<ManifoldBranch> branches = List.of(
                branch("unstable-plus", point, unstableDirection, request.perturbation(), request.duration(), request),
                branch("unstable-minus", point, unstableDirection, -request.perturbation(), request.duration(), request),
                branch("stable-plus", point, stableDirection, request.perturbation(), -request.duration(), request),
                branch("stable-minus", point, stableDirection, -request.perturbation(), -request.duration(), request)
        );
        return new ManifoldResult(point, lambda, branches);
    }

    private ManifoldBranch branch(String name, LagrangePoint point, double[] direction,
                                  double signedPerturbation, double signedDuration, ManifoldRequest request) {
        StateVector seed = new StateVector(
                point.x() + signedPerturbation * direction[0],
                point.y() + signedPerturbation * direction[1],
                signedPerturbation * direction[2],
                signedPerturbation * direction[3]);
        SimulationRequest simulationRequest = new SimulationRequest(
                seed, signedDuration,
                request.absoluteTolerance(), request.relativeTolerance(),
                request.minStep(), request.maxStep());
        return new ManifoldBranch(name, propagator.propagate(simulationRequest).points());
    }

    private LagrangePoint findCollinearPoint(String id) {
        if ("L4".equalsIgnoreCase(id) || "L5".equalsIgnoreCase(id)) {
            throw new IllegalArgumentException(
                    id + " is linearly stable for the Sun–Jupiter mass ratio and has no stable/unstable manifolds");
        }
        return lagrangePointCalculator.computeAll().stream()
                .filter(p -> p.name().equalsIgnoreCase(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Lagrange point id: " + id));
    }

    /** The positive real eigenvalue of the collinear-point linearization. */
    static double unstableEigenvalue(double omegaXX, double omegaYY) {
        // λ⁴ + pλ² + q = 0 with q < 0 has exactly one positive root for λ²
        double p = 4.0 - omegaXX - omegaYY;
        double q = omegaXX * omegaYY;
        double lambdaSquared = (-p + Math.sqrt(p * p - 4.0 * q)) / 2.0;
        if (lambdaSquared <= 0) {
            throw new IllegalStateException("No hyperbolic eigenvalue: Ωxx·Ωyy = " + q + " is not negative");
        }
        return Math.sqrt(lambdaSquared);
    }

    /** Unit eigenvector of the linearization for real eigenvalue λ (valid when Ωxy = 0). */
    static double[] hyperbolicEigenvector(double omegaXX, double lambda) {
        double vy = (lambda * lambda - omegaXX) / (2.0 * lambda);
        double[] v = {1.0, vy, lambda, lambda * vy};
        double norm = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2] + v[3] * v[3]);
        for (int i = 0; i < v.length; i++) {
            v[i] /= norm;
        }
        return v;
    }
}
