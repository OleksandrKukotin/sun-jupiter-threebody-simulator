package org.github.oleksandrkukotin.physics;

import org.springframework.stereotype.Component;

/**
 * Computes zero-velocity curve (ZVC) data for a given Jacobi constant.
 *
 * <p>A particle with Jacobi constant C cannot enter regions where 2Ω(x, y) &lt; C
 * (the forbidden regions). The ZVC is the boundary where 2Ω(x, y) = C.
 *
 * @see <a href="https://github.com/OleksandrKukotin/sun-jupiter-threebody-simulator/issues/5">Issue #5</a>
 */
@Component
public class ZeroVelocityCurve {

    private final JacobiConstant jacobiConstant;

    public ZeroVelocityCurve(JacobiConstant jacobiConstant) {
        this.jacobiConstant = jacobiConstant;
    }

    /**
     * Samples the effective potential on a 2D grid and marks forbidden regions.
     *
     * @param jacobiC    target Jacobi constant C
     * @param xMin       grid x minimum (normalized units)
     * @param xMax       grid x maximum
     * @param yMin       grid y minimum
     * @param yMax       grid y maximum
     * @param resolution number of grid points per axis
     * @return 2D boolean array [xIndex][yIndex]; {@code true} = forbidden region (2Ω &lt; C)
     */
    public boolean[][] computeForbiddenRegion(double jacobiC,
                                              double xMin, double xMax,
                                              double yMin, double yMax,
                                              int resolution) {
        boolean[][] grid = new boolean[resolution][resolution];
        double dx = (xMax - xMin) / (resolution - 1);
        double dy = (yMax - yMin) / (resolution - 1);

        for (int i = 0; i < resolution; i++) {
            double x = xMin + i * dx;
            for (int j = 0; j < resolution; j++) {
                double y = yMin + j * dy;
                grid[i][j] = 2.0 * jacobiConstant.effectivePotential(x, y) < jacobiC;
            }
        }
        return grid;
    }
}