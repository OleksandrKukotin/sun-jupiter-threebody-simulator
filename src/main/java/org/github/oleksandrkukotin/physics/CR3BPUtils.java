package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.config.PhysicsConstants;

/** Shared distance calculations for the CR3BP in normalized synodic coordinates. */
public final class CR3BPUtils {

    private CR3BPUtils() {}

    /** Distance from the third body to the Sun (primary). r₁ = √((x+μ)² + y²) */
    public static double distanceToSun(double x, double y) {
        double dx = x + PhysicsConstants.MU;
        return Math.sqrt(dx * dx + y * y);
    }

    /** Distance from the third body to Jupiter (secondary). r₂ = √((x−(1−μ))² + y²) */
    public static double distanceToJupiter(double x, double y) {
        double dx = x - PhysicsConstants.ONE_MINUS_MU;
        return Math.sqrt(dx * dx + y * y);
    }
}