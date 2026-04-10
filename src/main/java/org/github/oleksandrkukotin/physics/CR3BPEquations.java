package org.github.oleksandrkukotin.physics;

import org.apache.commons.math3.ode.FirstOrderDifferentialEquations;
import org.github.oleksandrkukotin.config.PhysicsConstants;
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
        double x = y[0];
        double yPos = y[1];
        double r1 = CR3BPUtils.distanceToSun(x, yPos);
        double r2 = CR3BPUtils.distanceToJupiter(x, yPos);
        double xDot = y[2];
        double yDot_ = y[3];
        yDot[0] = xDot;
        yDot[1] = yDot_;
        yDot[2] = 2 * yDot_ + x - PhysicsConstants.ONE_MINUS_MU * (x + PhysicsConstants.MU) / (r1 * r1 * r1) -
                PhysicsConstants.MU * ((x - PhysicsConstants.ONE_MINUS_MU) / (r2 * r2 * r2));
        yDot[3] = -2 * xDot + yPos - PhysicsConstants.ONE_MINUS_MU * yPos / (r1 * r1 * r1) -
                PhysicsConstants.MU * yPos / (r2 * r2 * r2);
    }
}