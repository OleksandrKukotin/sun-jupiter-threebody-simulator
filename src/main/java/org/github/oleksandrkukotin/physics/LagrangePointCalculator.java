package org.github.oleksandrkukotin.physics;

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
    private LagrangePoint computeL1() {
        LagrangePoint lp = new LagrangePoint();
        throw new UnsupportedOperationException("Not yet implemented — see issue #2");
    }

    /** L2: beyond Jupiter (opposite Sun). Found via Newton's method on the quintic. */
    private LagrangePoint computeL2() {
        // TODO (#2)
        throw new UnsupportedOperationException("Not yet implemented — see issue #2");
    }

    /** L3: beyond Sun (opposite Jupiter). Found via Newton's method on the quintic. */
    private LagrangePoint computeL3() {
        // TODO (#2)
        throw new UnsupportedOperationException("Not yet implemented — see issue #2");
    }

    /** L4: leading equilateral point at (0.5 − μ, √3/2). */
    private LagrangePoint computeL4() {
        // TODO (#2)
        throw new UnsupportedOperationException("Not yet implemented — see issue #2");
    }

    /** L5: trailing equilateral point at (0.5 − μ, −√3/2). */
    private LagrangePoint computeL5() {
        // TODO (#2)
        throw new UnsupportedOperationException("Not yet implemented — see issue #2");
    }
}