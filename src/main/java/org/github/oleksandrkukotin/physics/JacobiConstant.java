package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.model.StateVector;
import org.springframework.stereotype.Component;

/**
 * Computes the Jacobi integral — the only known first integral of the CR3BP.
 *
 * <p>Definition: C = 2Ω(x, y) − v², where Ω is the effective potential
 * and v² = ẋ² + ẏ².
 *
 * <p>Effective potential: Ω(x, y) = (x² + y²)/2 + (1−μ)/r₁ + μ/r₂
 *
 * @see <a href="https://github.com/OleksandrKukotin/sun-jupiter-threebody-simulator/issues/4">Issue #4</a>
 */
@Component
public class JacobiConstant {

    /** Computes the Jacobi constant C for the given state vector. */
    public double compute(StateVector state) {
        // TODO (#4): C = 2 * effectivePotential(x, y) - (xDot² + yDot²)
        throw new UnsupportedOperationException("Not yet implemented — see issue #4");
    }

    /** Computes Ω(x, y) = (x² + y²)/2 + (1−μ)/r₁ + μ/r₂. */
    public double effectivePotential(double x, double y) {
        // TODO (#4)
        throw new UnsupportedOperationException("Not yet implemented — see issue #4");
    }
}