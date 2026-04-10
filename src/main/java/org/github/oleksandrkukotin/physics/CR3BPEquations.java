package org.github.oleksandrkukotin.physics;

import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.springframework.stereotype.Component;

/**
 * CR3BP equations of motion in the rotating (synodic) reference frame.
 *
 * <p>State vector: [x, y, ẋ, ẏ]
 *
 * <p>Equations (normalized units):
 * <pre>
 *   ẍ = 2ẏ + x − (1−μ)(x+μ)/r₁³ − μ(x−(1−μ))/r₂³
 *   ÿ = −2ẋ + y − (1−μ)y/r₁³ − μy/r₂³
 * </pre>
 * where r₁ = distance to Sun, r₂ = distance to Jupiter.
 *
 * @see <a href="https://github.com/OleksandrKukotin/sun-jupiter-threebody-simulator/issues/1">Issue #1</a>
 */
@Component
public class CR3BPEquations implements FirstOrderDifferentialEquations {

    @Override
    public int getDimension() {
        return 4;
    }

    @Override
    public void computeDerivatives(double t, double[] y, double[] yDot) {
        // TODO (#1): Implement CR3BP equations of motion
        // y[0] = x,  y[1] = y,  y[2] = xDot,  y[3] = yDot
        throw new UnsupportedOperationException("Not yet implemented — see issue #1");
    }
}