package org.github.oleksandrkukotin.physics;

import org.apache.commons.math3.linear.EigenDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.ode.events.EventHandler;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.github.oleksandrkukotin.model.LagrangePoint;
import org.github.oleksandrkukotin.model.PeriodicOrbit;
import org.github.oleksandrkukotin.model.StateVector;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Finds planar Lyapunov orbits around a collinear Lagrange point (L1, L2, or L3)
 * via linearization + single-shooting differential correction, then walks the
 * family via natural-parameter continuation.
 *
 * <p>Only L1/L2/L3 are valid inputs — L4/L5 are the stable triangular points and
 * don't have this family; those are rejected with an {@link IllegalArgumentException}.
 *
 * <p>Algorithm (see Koon-Lo-Marsden-Ross, "Dynamical Systems, the Three-Body
 * Problem, and Space Mission Design"):
 *
 * <ol>
 *   <li>Linearize CR3BP at the L-point: build A (the same Jacobian used by
 *       {@link CR3BPVariationalEquations}), eigen-decompose it, and pull out the
 *       purely-imaginary eigenvalue pair {@code +-i*omega} and the eigenvector
 *       for {@code +i*omega}.</li>
 *   <li>Normalize the eigenvector so its x-component is real 1. Let
 *       {@code b = (Re(v_y), Im(v_y))} be the resulting complex y-component.
 *       The small-amplitude initial guess for amplitude Ax is:
 *       <pre>
 *         x0  = xL + Ax
 *         y0  = Ax * Re(b)
 *         vx0 = 0
 *         vy0 = -Ax * omega * Im(b)
 *       </pre>
 *   </li>
 *   <li>Single-shoot: integrate the augmented (state + STM) ODE from this guess
 *       until the first y=0 crossing after t=0. At that crossing (x_f, 0, vx_f,
 *       vy_f) with STM Phi, Newton-correct vy0 to drive vx_f -&gt; 0:
 *       <pre>
 *         dt_f/dvy0 = -Phi[1][3] / vy_f
 *         delta_vy0 = -vx_f / (Phi[2][3] - ax_f * Phi[1][3] / vy_f)
 *       </pre>
 *       where ax_f is xDotDot evaluated at the crossing state. Repeat until
 *       |vx_f| is below tolerance. Full period = 2 * t_f.</li>
 *   <li>Family continuation: for the next amplitude, seed the corrector with the
 *       *previous converged* (x0, vy0) instead of re-deriving from linear
 *       theory — much more robust once amplitude grows past where linearization
 *       is accurate.</li>
 * </ol>
 */
@Component
public class LyapunovOrbitFinder {

    private static final Set<String> COLLINEAR_POINTS = Set.of("L1", "L2", "L3");

    private static final int MAX_CORRECTION_ITERATIONS = 25;
    private static final double VX_TOLERANCE = 1e-10;
    private static final double MIN_IMAG_EIGENVALUE = 1e-9;

    private static final double INTEGRATOR_MIN_STEP = 1e-12;
    private static final double INTEGRATOR_MAX_STEP = 0.5;
    private static final double INTEGRATOR_ABS_TOLERANCE = 1e-13;
    private static final double INTEGRATOR_REL_TOLERANCE = 1e-13;
    private static final double EVENT_MAX_CHECK_INTERVAL = 1.0;
    private static final double EVENT_CONVERGENCE = 1e-13;
    private static final int EVENT_MAX_ITERATION_COUNT = 1000;
    private static final double HALF_PERIOD_SEARCH_MARGIN = 4.0;

    private final LagrangePointCalculator lagrangePointCalculator;
    private final CR3BPEquations equations;
    private final CR3BPVariationalEquations variationalEquations;
    private final JacobiConstant jacobiConstant;

    public LyapunovOrbitFinder(LagrangePointCalculator lagrangePointCalculator,
                                CR3BPEquations equations,
                                CR3BPVariationalEquations variationalEquations,
                                JacobiConstant jacobiConstant) {
        this.lagrangePointCalculator = lagrangePointCalculator;
        this.equations = equations;
        this.variationalEquations = variationalEquations;
        this.jacobiConstant = jacobiConstant;
    }

    /**
     * Finds a planar Lyapunov orbit of x-amplitude {@code amplitude} around the
     * named collinear Lagrange point, seeded from linear theory and refined by
     * single-shooting differential correction.
     */
    public PeriodicOrbit findLyapunovOrbit(String lagrangePointName, double amplitude) {
        if (!COLLINEAR_POINTS.contains(lagrangePointName)) {
            throw new IllegalArgumentException(
                    "Lyapunov orbits only exist around collinear points L1/L2/L3, got: " + lagrangePointName);
        }
        LagrangePoint lPoint = lagrangePointCalculator.computeAll().stream()
                .filter(p -> p.name().equals(lagrangePointName))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Lagrange point not found: " + lagrangePointName));

        double[][] a = CR3BPVariationalEquations.jacobianAt(lPoint.x(), lPoint.y());
        EigenDecomposition eigenDecomposition = new EigenDecomposition(MatrixUtils.createRealMatrix(a));

        int omegaIndex = -1;
        double omega = 0.0;
        for (int i = 0; i < a.length; i++) {
            double imaginaryPart = eigenDecomposition.getImagEigenvalue(i);
            if (imaginaryPart > MIN_IMAG_EIGENVALUE) {
                omega = imaginaryPart;
                omegaIndex = i;
                break;
            }
        }
        if (omegaIndex < 0 || omegaIndex + 1 >= a.length) {
            throw new IllegalStateException(
                    "No purely imaginary eigenvalue pair found at " + lagrangePointName
                            + " — cannot build a linear-theory Lyapunov seed.");
        }

        RealMatrix eigenvectors = eigenDecomposition.getV();
        double vxRe = eigenvectors.getEntry(0, omegaIndex);
        double vxIm = eigenvectors.getEntry(0, omegaIndex + 1);
        double vyRe = eigenvectors.getEntry(1, omegaIndex);
        double vyIm = eigenvectors.getEntry(1, omegaIndex + 1);

        // Normalize the complex eigenvector so its x-component is real 1: multiply
        // through by 1/(vxRe + i*vxIm), then read off the resulting y-component b.
        double denom = vxRe * vxRe + vxIm * vxIm;
        double bIm = (vxRe * vyIm - vxIm * vyRe) / denom;

        // Re(b) is 0 for any collinear point, not just approximately: Uxy = 3(1-mu)(x+mu)y/r1^5
        // + 3*mu*(x-(1-mu))*y/r2^5 vanishes exactly at y=0, which decouples the eigenvector
        // equations so that a real x-component forces a purely imaginary y-component. Using
        // the literal 0.0 here (instead of the amplitude*Re(b) that eigen-decomposition would
        // return, which is only zero up to floating-point noise) keeps the seed's y=0 crossing
        // detector in correct(seed, ...) free of spurious near-t0 events from that noise.
        StateVector seed = new StateVector(
                lPoint.x() + amplitude,
                0.0,
                0.0,
                -amplitude * omega * bIm);

        double estimatedHalfPeriod = Math.PI / omega;
        CorrectionResult result = correct(seed, estimatedHalfPeriod);

        return new PeriodicOrbit(lagrangePointName, amplitude, result.initialState(),
                2 * result.halfPeriod(), jacobiConstant.compute(result.initialState()));
    }

    /**
     * Single-shooting differential correction: adjusts vy0 of {@code seed} so that the
     * state, integrated forward to its first y=0 crossing, arrives with vx=0 (a
     * perpendicular crossing) — which makes the trajectory periodic with period
     * {@code 2 * halfPeriod}.
     *
     * @param seed                initial guess; only its yDot component is corrected
     *                            across iterations, x/y/xDot are held fixed
     * @param estimatedHalfPeriod rough estimate of the time to the first y=0 crossing,
     *                            used to bound the search window
     */
    private CorrectionResult correct(StateVector seed, double estimatedHalfPeriod) {
        StateVector current = seed;
        double maxTime = HALF_PERIOD_SEARCH_MARGIN * estimatedHalfPeriod;

        for (int iteration = 0; iteration < MAX_CORRECTION_ITERATIONS; iteration++) {
            double[] y0 = CR3BPVariationalEquations.initialAugmentedState(current);
            double[] yOut = new double[variationalEquations.getDimension()];

            DormandPrince853Integrator integrator = new DormandPrince853Integrator(
                    INTEGRATOR_MIN_STEP, INTEGRATOR_MAX_STEP, INTEGRATOR_ABS_TOLERANCE, INTEGRATOR_REL_TOLERANCE);
            YCrossingDetector eventHandler = new YCrossingDetector();
            integrator.addEventHandler(
                    eventHandler, EVENT_MAX_CHECK_INTERVAL, EVENT_CONVERGENCE, EVENT_MAX_ITERATION_COUNT);

            double tCrossing = integrator.integrate(variationalEquations, 0.0, y0, maxTime, yOut);

            if (!eventHandler.wasTriggered()) {
                throw new IllegalStateException(
                        "Differential correction failed: no y=0 crossing found within "
                                + maxTime + " time units for seed " + current);
            }

            double xf = yOut[0];
            double yf = yOut[1];
            double vxf = yOut[2];
            double vyf = yOut[3];

            if (Math.abs(vxf) < VX_TOLERANCE) {
                return new CorrectionResult(current, tCrossing);
            }

            double[] crossingDerivatives = new double[4];
            equations.computeDerivatives(tCrossing, new double[]{xf, yf, vxf, vyf}, crossingDerivatives);
            double axf = crossingDerivatives[2];

            double phi13 = yOut[4 + 1 * 4 + 3];
            double phi23 = yOut[4 + 2 * 4 + 3];
            double deltaVy0 = -vxf / (phi23 - axf * phi13 / vyf);

            current = new StateVector(current.x(), current.y(), current.xDot(), current.yDot() + deltaVy0);
        }

        throw new IllegalStateException(
                "Differential correction did not converge within " + MAX_CORRECTION_ITERATIONS
                        + " iterations for seed " + seed);
    }

    /**
     * Walks a family of Lyapunov orbits by natural-parameter continuation in amplitude:
     * the first orbit comes from linear theory ({@link #findLyapunovOrbit}); each
     * subsequent orbit is seeded from the previous converged initial state, with x
     * nudged by {@code stepAmplitude} and vy0 scaled by the amplitude ratio, rather than
     * re-deriving from the linear eigenvector.
     *
     * <p>vy0 is scaled, not just copied: L1's saddle eigenvalue means nearby trajectories
     * diverge from the true periodic solution roughly like e^(|lambda| * halfPeriod)
     * (order 10x for Sun-Jupiter L1) by the time they reach the y=0 crossing the corrector
     * targets, so reusing the exact previous vy0 unscaled can already push Newton's method
     * outside its basin of convergence even for modest steps — scaling keeps the seed on
     * the family's approximate slope instead of implicitly assuming it's flat.
     */
    public List<PeriodicOrbit> continueFamily(String lagrangePointName,
                                               double startAmplitude,
                                               double stepAmplitude,
                                               int numOrbits) {
        if (numOrbits < 1) {
            throw new IllegalArgumentException("numOrbits must be at least 1, got: " + numOrbits);
        }

        List<PeriodicOrbit> family = new ArrayList<>(numOrbits);
        PeriodicOrbit first = findLyapunovOrbit(lagrangePointName, startAmplitude);
        family.add(first);

        StateVector previousState = first.initialState();
        double previousHalfPeriod = first.period() / 2.0;
        double previousAmplitude = startAmplitude;

        for (int i = 1; i < numOrbits; i++) {
            double amplitude = previousAmplitude + stepAmplitude;
            double vy0Scale = amplitude / previousAmplitude;
            StateVector nextSeed = new StateVector(
                    previousState.x() + stepAmplitude,
                    previousState.y(),
                    previousState.xDot(),
                    previousState.yDot() * vy0Scale);

            CorrectionResult result = correct(nextSeed, previousHalfPeriod);
            StateVector converged = result.initialState();

            family.add(new PeriodicOrbit(lagrangePointName, amplitude, converged,
                    2 * result.halfPeriod(), jacobiConstant.compute(converged)));

            previousState = converged;
            previousAmplitude = amplitude;
            previousHalfPeriod = result.halfPeriod();
        }

        return family;
    }

    /** Fires on the first sign change of y(t) after t=0, i.e. the orbit's return to y=0. */
    private static final class YCrossingDetector implements EventHandler {
        private boolean triggered;

        @Override
        public void init(double t0, double[] y0, double t) {
            triggered = false;
        }

        @Override
        public double g(double t, double[] y) {
            return y[1];
        }

        @Override
        public Action eventOccurred(double t, double[] y, boolean increasing) {
            triggered = true;
            return Action.STOP;
        }

        @Override
        public void resetState(double t, double[] y) {
            // Action.STOP never invokes this; no state reset needed.
        }

        boolean wasTriggered() {
            return triggered;
        }
    }

    /** What {@link #correct} converges to: the initial state and time of its first y=0 crossing. */
    private record CorrectionResult(StateVector initialState, double halfPeriod) {}
}
