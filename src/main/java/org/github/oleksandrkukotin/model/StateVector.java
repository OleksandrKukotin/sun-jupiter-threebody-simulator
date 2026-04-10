package org.github.oleksandrkukotin.model;

/**
 * Represents a CR3BP state vector in the rotating synodic frame.
 * All quantities are in normalized units (distance: Sun–Jupiter separation; time: Jupiter orbital period / 2π).
 */
public record StateVector(double x, double y, double xDot, double yDot) {

    public double[] toArray() {
        return new double[]{x, y, xDot, yDot};
    }

    public static StateVector fromArray(double[] state) {
        return new StateVector(state[0], state[1], state[2], state[3]);
    }
}