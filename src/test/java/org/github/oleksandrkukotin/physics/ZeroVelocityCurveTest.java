package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ZeroVelocityCurveTest {

    private ZeroVelocityCurve zvc;
    private JacobiConstant jacobi;

    @BeforeEach
    void setUp() {
        jacobi = new JacobiConstant();
        zvc = new ZeroVelocityCurve(jacobi);
    }

    @Test
    void computeForbiddenRegion_returnsSquareGridMatchingResolution() {
        boolean[][] grid = zvc.computeForbiddenRegion(3.0, -1.5, 1.5, -1.5, 1.5, 50);
        assertEquals(50, grid.length);
        for (boolean[] row : grid) {
            assertEquals(50, row.length);
        }
    }

    @Test
    void computeForbiddenRegion_isSymmetricAboutXAxis() {
        // Ω(x, y) = Ω(x, −y) because r1, r2 depend on |y|.
        int n = 41;
        boolean[][] grid = zvc.computeForbiddenRegion(3.0, -1.5, 1.5, -1.5, 1.5, n);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                assertEquals(grid[i][j], grid[i][n - 1 - j],
                        "asymmetry at (" + i + "," + j + ")");
            }
        }
    }

    @Test
    void computeForbiddenRegion_veryLowC_allowsEverything() {
        // C ≪ 2Ω_min means 2Ω > C everywhere → nothing forbidden.
        boolean[][] grid = zvc.computeForbiddenRegion(-100.0, -1.5, 1.5, -1.5, 1.5, 30);
        for (boolean[] row : grid) {
            for (boolean cell : row) {
                assertFalse(cell, "no cell should be forbidden at C = -100");
            }
        }
    }

    @Test
    void computeForbiddenRegion_veryHighC_forbidsEverything() {
        // C ≫ 2Ω everywhere on the grid → every cell forbidden.
        // The primaries themselves are at x = -μ and x = 1-μ; keep the grid off them
        // (singular Ω → 2Ω > any finite C there, so those cells would be "allowed").
        boolean[][] grid = zvc.computeForbiddenRegion(1e6, 0.2, 0.8, 0.2, 0.8, 20);
        for (boolean[] row : grid) {
            for (boolean cell : row) {
                assertTrue(cell, "every cell should be forbidden at C = 1e6");
            }
        }
    }

    @Test
    void computeForbiddenRegion_atL4_justBelowCL4_L4CellIsAccessible() {
        // At L4, 2Ω(L4) = C_L4 (zero-velocity equilibrium). For C slightly below C_L4,
        // the neighborhood of L4 is accessible (not forbidden).
        double xL4 = 0.5 - PhysicsConstants.MU;
        double yL4 = Math.sqrt(3) / 2.0;
        double cL4 = 2.0 * jacobi.effectivePotential(xL4, yL4);

        int n = 21;
        double half = 0.05;
        boolean[][] grid = zvc.computeForbiddenRegion(
                cL4 - 1e-6, xL4 - half, xL4 + half, yL4 - half, yL4 + half, n);

        // Center cell ≈ L4 must be accessible.
        assertFalse(grid[n / 2][n / 2], "L4 itself must be accessible for C < C_L4");
    }

    @Test
    void computeForbiddenRegion_primaryCellsAreAccessible() {
        // Ω diverges to +∞ at the primaries, so 2Ω is never < C there → not forbidden.
        int n = 11; // odd → center cell at i = n/2
        double mu = PhysicsConstants.MU;

        // Grid centered on Jupiter at (1 − μ, 0).
        double cx = 1.0 - mu;
        boolean[][] grid = zvc.computeForbiddenRegion(1e6, cx - 0.1, cx + 0.1, -0.1, 0.1, n);
        assertFalse(grid[n / 2][n / 2], "cell at Jupiter must be accessible (Ω → ∞)");
    }
}
