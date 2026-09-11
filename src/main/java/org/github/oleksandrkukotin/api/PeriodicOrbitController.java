package org.github.oleksandrkukotin.api;

import org.github.oleksandrkukotin.model.PeriodicOrbit;
import org.github.oleksandrkukotin.model.PeriodicOrbitResult;
import org.github.oleksandrkukotin.model.SimulationRequest;
import org.github.oleksandrkukotin.physics.LyapunovOrbitFinder;
import org.github.oleksandrkukotin.physics.StateVectorPropagator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @see <a href="https://github.com/OleksandrKukotin/sun-jupiter-threebody-simulator/issues/16">Issue #16</a>
 */
@RestController
@RequestMapping("/api/periodic-orbits")
public class PeriodicOrbitController {

    // Tight tolerances so the returned trajectory closes visually on the differentially
    // corrected initial state, matching the precision the corrector itself targets.
    private static final double PROPAGATION_ABS_TOLERANCE = 1e-12;
    private static final double PROPAGATION_REL_TOLERANCE = 1e-12;
    private static final double PROPAGATION_MIN_STEP = 1e-9;
    private static final double PROPAGATION_MAX_STEP = 0.5;

    private final LyapunovOrbitFinder lyapunovOrbitFinder;
    private final StateVectorPropagator propagator;

    public PeriodicOrbitController(LyapunovOrbitFinder lyapunovOrbitFinder, StateVectorPropagator propagator) {
        this.lyapunovOrbitFinder = lyapunovOrbitFinder;
        this.propagator = propagator;
    }

    /**
     * Finds a periodic orbit of the given family and x-amplitude around a collinear
     * Lagrange point, then propagates it through one full period for plotting.
     *
     * <p>Only the "lyapunov" family exists today; the parameter is exposed now so the
     * endpoint shape doesn't need to change when halo orbits (see issue #16) are added.
     */
    @GetMapping
    public PeriodicOrbitResult getPeriodicOrbit(
            @RequestParam String point,
            @RequestParam(defaultValue = "lyapunov") String family,
            @RequestParam double amplitude) {
        if (!"lyapunov".equalsIgnoreCase(family)) {
            throw new IllegalArgumentException("Unsupported periodic-orbit family: " + family);
        }

        PeriodicOrbit orbit = lyapunovOrbitFinder.findLyapunovOrbit(point, amplitude);

        SimulationRequest request = new SimulationRequest(
                orbit.initialState(), orbit.period(),
                PROPAGATION_ABS_TOLERANCE, PROPAGATION_REL_TOLERANCE,
                PROPAGATION_MIN_STEP, PROPAGATION_MAX_STEP);

        return new PeriodicOrbitResult(orbit, propagator.propagate(request).points());
    }
}
