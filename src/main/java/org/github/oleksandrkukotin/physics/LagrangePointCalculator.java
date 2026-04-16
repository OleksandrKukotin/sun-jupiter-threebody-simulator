package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.LagrangePoint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Computes the five Lagrange equilibrium points for the Sun–Jupiter CR3BP.
 *
 * <p>L4 and L5 are found analytically; L1, L2, L3 require numerical root-finding
 * on the quintic polynomial derived from the collinear equilibrium condition.
 *
 * @see <a href="https://github.com/OleksandrKukotin/sun-jupiter-threebody-simulator/issues/2">Issue #2</a>
 */
@Component
public class LagrangePointCalculator {

    /** Returns all five Lagrange points in normalized synodic coordinates. */
    public List<LagrangePoint> computeAll() {
        List<LagrangePoint> points = new ArrayList();
        points.add(computeL1());
        points.add(computeL2());
        points.add(computeL3());
        points.add(computeL4());
        points.add(computeL5());
        return points;
    }

    /** L1: between Sun and Jupiter. Found via Newton's method on the quintic. */
    LagrangePoint computeL1() {
        double mu = PhysicsConstants.MU;
        double oneMu = PhysicsConstants.ONE_MINUS_MU;
        double gamma = Math.pow(mu / 3.0, 1.0 / 3.0);
        double x = oneMu - gamma;
        for (int i = 0; i < 50; i++) {
            double r1 = x + mu;
            double r2 = oneMu - x;

            double fx = x - oneMu / (r1 * r1) + mu / (r2 * r2);
            double fpx = 1.0 + 2.0 * oneMu / (r1 * r1 * r1) + 2.0 * mu / (r2 * r2 * r2);

            double dx = fx / fpx;
            x -= dx;
            if (Math.abs(dx) < 1e-12) break;
        }
        return new LagrangePoint("L1", x, 0.0);
    }

    /** L2: beyond Jupiter (opposite Sun). Found via Newton's method on the quintic. */
    LagrangePoint computeL2() {
        double mu = PhysicsConstants.MU;
        double oneMu = PhysicsConstants.ONE_MINUS_MU;
        double gamma = Math.pow(mu / 3.0, 1.0 / 3.0);
        double x = oneMu + gamma;
        for (int i = 0; i < 50; i++) {
            double r1 = x + mu;
            double r2 = x - oneMu;

            double fx = x - oneMu / (r1 * r1) - mu / (r2 * r2);
            double fpx = 1 + 2 * oneMu / (r1 * r1 * r1) + 2 * mu / (r2 * r2 * r2);

            double dx = fx / fpx;
            x -= dx;
            if (Math.abs(dx) < 1e-12) break;
        }
        return new LagrangePoint("L2", x, 0.0);
    }

    /** L3: beyond Sun (opposite Jupiter). Found via Newton's method on the quintic. */
    LagrangePoint computeL3() {
        double mu = PhysicsConstants.MU;
        double oneMu = PhysicsConstants.ONE_MINUS_MU;
        double x = -1.0 - 5.0 * mu / 12.0;
        for (int i = 0; i < 50; i++) {
            double r1 = -(x + mu);
            double r2 = oneMu - x;

            double fx = x + oneMu / (r1 * r1) + mu / (r2 * r2);
            double fpx = 1.0 + 2.0 * oneMu / (r1 * r1 * r1) + 2.0 * mu / (r2 * r2 * r2);
            double dx = fx / fpx;

            x -= dx;
            if (Math.abs(dx) < 1e-12) break;
        }
        return new LagrangePoint("L3", x, 0.0);
    }

    /** L4: leading equilateral point at (0.5 − μ, √3/2). */
    LagrangePoint computeL4() {
        return new LagrangePoint("L4", 0.5 - PhysicsConstants.MU, Math.sqrt(3) / 2.0);
    }

    /** L5: trailing equilateral point at (0.5 − μ, −√3/2). */
    LagrangePoint computeL5() {
        return new LagrangePoint("L5", 0.5 - PhysicsConstants.MU, -Math.sqrt(3) / 2);
    }
}