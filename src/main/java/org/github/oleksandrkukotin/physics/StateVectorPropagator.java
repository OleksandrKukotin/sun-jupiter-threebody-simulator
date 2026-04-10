package org.github.oleksandrkukotin.physics;

import org.github.oleksandrkukotin.model.SimulationRequest;
import org.github.oleksandrkukotin.model.TrajectoryResult;
import org.springframework.stereotype.Component;

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
        // TODO (#3): Set up DormandPrince853Integrator with request tolerances,
        // attach a StepHandler that records TrajectoryPoints (state + jacobiConstant),
        // integrate from t=0 to t=request.duration(), and return a TrajectoryResult.
        throw new UnsupportedOperationException("Not yet implemented — see issue #3");
    }
}