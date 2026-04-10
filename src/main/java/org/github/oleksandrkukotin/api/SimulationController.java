package org.github.oleksandrkukotin.api;

import org.github.oleksandrkukotin.model.LagrangePoint;
import org.github.oleksandrkukotin.model.SimulationRequest;
import org.github.oleksandrkukotin.model.TrajectoryResult;
import org.github.oleksandrkukotin.physics.LagrangePointCalculator;
import org.github.oleksandrkukotin.physics.StateVectorPropagator;
import org.github.oleksandrkukotin.physics.ZeroVelocityCurve;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final StateVectorPropagator propagator;
    private final LagrangePointCalculator lagrangePointCalculator;
    private final ZeroVelocityCurve zeroVelocityCurve;

    public SimulationController(StateVectorPropagator propagator,
                                LagrangePointCalculator lagrangePointCalculator,
                                ZeroVelocityCurve zeroVelocityCurve) {
        this.propagator = propagator;
        this.lagrangePointCalculator = lagrangePointCalculator;
        this.zeroVelocityCurve = zeroVelocityCurve;
    }

    /** Runs a trajectory from the given initial conditions. */
    @PostMapping("/propagate")
    public TrajectoryResult propagate(@RequestBody SimulationRequest request) {
        return propagator.propagate(request);
    }

    /** Returns L1–L5 positions in normalized synodic coordinates. */
    @GetMapping("/lagrange-points")
    public List<LagrangePoint> getLagrangePoints() {
        return lagrangePointCalculator.computeAll();
    }

    /** Returns the forbidden region grid for the given Jacobi constant. */
    @GetMapping("/zero-velocity-curve")
    public boolean[][] getZeroVelocityCurve(
            @RequestParam double jacobiConstant,
            @RequestParam(defaultValue = "-2.0") double xMin,
            @RequestParam(defaultValue = "2.0")  double xMax,
            @RequestParam(defaultValue = "-2.0") double yMin,
            @RequestParam(defaultValue = "2.0")  double yMax,
            @RequestParam(defaultValue = "200")  int resolution) {
        return zeroVelocityCurve.computeForbiddenRegion(jacobiConstant, xMin, xMax, yMin, yMax, resolution);
    }
}