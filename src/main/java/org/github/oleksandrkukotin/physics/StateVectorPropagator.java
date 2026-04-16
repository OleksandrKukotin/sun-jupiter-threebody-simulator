package org.github.oleksandrkukotin.physics;

import org.apache.commons.math3.exception.MaxCountExceededException;
import org.apache.commons.math3.ode.nonstiff.DormandPrince853Integrator;
import org.apache.commons.math3.ode.sampling.StepHandler;
import org.apache.commons.math3.ode.sampling.StepInterpolator;
import org.github.oleksandrkukotin.model.SimulationRequest;
import org.github.oleksandrkukotin.model.StateVector;
import org.github.oleksandrkukotin.model.TrajectoryPoint;
import org.github.oleksandrkukotin.model.TrajectoryResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Propagates a CR3BP state vector forward in time using an adaptive step-size ODE integrator.
 *
 * <p>Planned integrator: {@code DormandPrince853Integrator} from Apache Commons Math,
 * with a {@code StepHandler} to record trajectory points at each accepted step.
 *
 * @see <a href="https://github.com/OleksandrKukotin/sun-jupiter-threebody-simulator/issues/3">Issue #3</a>
 */
@Component
public class StateVectorPropagator {

    private final CR3BPEquations equations;
    private final JacobiConstant jacobiConstant;

    public StateVectorPropagator(CR3BPEquations equations, JacobiConstant jacobiConstant) {
        this.equations = equations;
        this.jacobiConstant = jacobiConstant;
    }

    /**
     * Integrates the trajectory and returns all recorded states with Jacobi constants.
     *
     * @param request simulation parameters including initial state, duration, and tolerances
     * @return full trajectory with per-step Jacobi constant values
     */
    public TrajectoryResult propagate(SimulationRequest request) {
        DormandPrince853Integrator integrator = new DormandPrince853Integrator(
                request.minStep(), request.maxStep(),
                request.absoluteTolerance(), request.relativeTolerance());

        List<TrajectoryPoint> points = new ArrayList<>();

        integrator.addStepHandler(new StepHandler() {
            @Override
            public void init(double t0, double[] y0, double t) {}
            @Override
            public void handleStep(StepInterpolator interpolator, boolean isLast) throws MaxCountExceededException {
                double t = interpolator.getCurrentTime();
                StateVector stateVector = StateVector.fromArray(interpolator.getInterpolatedState());
                points.add(new TrajectoryPoint(t, stateVector, jacobiConstant.compute(stateVector)));
            }
        });

        double[] y0 = request.initialState().toArray();
        double[] yOut = new double[4];
        integrator.integrate(equations, 0.0, y0, request.duration(), yOut);

        double cInitial = jacobiConstant.compute(request.initialState());
        double cFinal = points.get(points.size() - 1).jacobiConstant();
        return new TrajectoryResult(points, cInitial, cFinal);
    }
}