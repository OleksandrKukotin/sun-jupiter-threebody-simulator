package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.exception.IllegalStateDataException;
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
        try {
            return 2 * effectivePotential(state.x(), state.y())
                    - (state.xDot() * state.xDot() + state.yDot() * state.yDot());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateDataException("Input StateVector is wrong or empty");
        }
    }

    public double effectivePotential(double x, double y) {
        return (x * x + y * y)/2
                + PhysicsConstants.ONE_MINUS_MU / CR3BPUtils.distanceToSun(x, y)
                + PhysicsConstants.MU / CR3BPUtils.distanceToJupiter(x, y);
    }
}