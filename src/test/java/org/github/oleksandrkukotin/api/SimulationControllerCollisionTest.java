package org.github.oleksandrkukotin.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.SimulationRequest;
import org.github.oleksandrkukotin.model.StateVector;
import org.github.oleksandrkukotin.physics.CR3BPEquations;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract for trajectories that cannot run to their requested duration.
 *
 * <ul>
 *   <li>Impact on a primary: 200, partial trajectory, {@code terminationReason} names the body.</li>
 *   <li>Normal run: 200, {@code terminationReason} is {@code COMPLETED}.</li>
 *   <li>No trajectory can be produced: 422 with a readable {@code message}.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SimulationControllerCollisionTest {

    private static final double ABS_TOL = 1e-10;
    private static final double REL_TOL = 1e-10;
    private static final double MIN_STEP = 1e-6;
    private static final double MAX_STEP = 1.0;

    // See StateVectorPropagatorCollisionTest for how these seeds were derived.
    private static final StateVector JUPITER_IMPACT =
            new StateVector(1.14819396326, 0.164613921996, -0.062164220388, -0.27417844989);
    private static final StateVector SUN_IMPACT =
            new StateVector(0.70571396203, 0.21856960347, -1.352426650561, -1.192176007811);

    /**
     * Starting exactly on a primary makes the first derivative 0/0. NaN then defeats the
     * integrator's step control and it never returns, so without this limit the test
     * would hang the build instead of failing.
     */
    @TestConfiguration
    static class EvaluationLimitConfig {
        private static final long MAX_EVALUATIONS = 1_000_000;

        @Bean
        @Primary
        CR3BPEquations evaluationLimitedEquations() {
            return new CR3BPEquations() {
                private long evaluations;

                @Override
                public synchronized void computeDerivatives(double t, double[] y, double[] yDot) {
                    if (++evaluations > MAX_EVALUATIONS) {
                        evaluations = 0;
                        throw new AssertionError("integrator still running after "
                                + MAX_EVALUATIONS + " evaluations, state " + java.util.Arrays.toString(y));
                    }
                    super.computeDerivatives(t, y, yDot);
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private ResultActions propagate(StateVector s0, double duration, double minStep) throws Exception {
        SimulationRequest request = new SimulationRequest(s0, duration, ABS_TOL, REL_TOL, minStep, MAX_STEP);
        return mockMvc.perform(post("/api/simulation/propagate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)));
    }

    private static void assertUnprocessableWithMessage(ResultActions actions) throws Exception {
        actions.andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", not(blankOrNullString())))
                .andExpect(jsonPath("$.message", not(containsString("Exception"))));
    }

    @Test
    void propagate_onJupiterImpact_returnsPartialTrajectoryNamingJupiter() throws Exception {
        propagate(JUPITER_IMPACT, 5.0, MIN_STEP)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terminationReason").value("COLLISION_JUPITER"))
                .andExpect(jsonPath("$.points", hasSize(greaterThan(0))));
    }

    @Test
    void propagate_onSunImpact_returnsPartialTrajectoryNamingSun() throws Exception {
        propagate(SUN_IMPACT, 5.0, MIN_STEP)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terminationReason").value("COLLISION_SUN"))
                .andExpect(jsonPath("$.points", hasSize(greaterThan(0))));
    }

    @Test
    void propagate_normalRun_reportsCompleted() throws Exception {
        propagate(new StateVector(0.5, 0.1, 0.0, 0.5), 5.0, MIN_STEP)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.terminationReason").value("COMPLETED"));
    }

    @Test
    void propagate_whenStepSizeFloorIsHitWithoutCollision_returns422() throws Exception {
        // A circular orbit at r = 0.01 of Jupiter, well clear of the body, but with a minStep
        // of 0.5 that the integrator cannot honour. No collision event can explain this one.
        double r = 0.01;
        StateVector s0 = new StateVector(PhysicsConstants.ONE_MINUS_MU + r, 0.0, 0.0,
                Math.sqrt(PhysicsConstants.MU / r));

        assertUnprocessableWithMessage(propagate(s0, 0.5, 0.5));
    }

    @Test
    void propagate_startingExactlyAtJupiterCentre_returns422() throws Exception {
        assertUnprocessableWithMessage(
                propagate(new StateVector(PhysicsConstants.ONE_MINUS_MU, 0.0, 0.0, 0.0), 1.0, MIN_STEP));
    }

    @Test
    void propagate_startingExactlyAtSunCentre_returns422() throws Exception {
        assertUnprocessableWithMessage(
                propagate(new StateVector(-PhysicsConstants.MU, 0.0, 0.0, 0.0), 1.0, MIN_STEP));
    }
}
