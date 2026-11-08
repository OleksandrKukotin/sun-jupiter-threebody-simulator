package org.github.oleksandrkukotin.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.github.oleksandrkukotin.config.PhysicsConstants;
import org.github.oleksandrkukotin.model.SimulationRequest;
import org.github.oleksandrkukotin.model.StateVector;
import org.github.oleksandrkukotin.model.TrajectoryResult;
import org.github.oleksandrkukotin.physics.CR3BPEquations;
import org.github.oleksandrkukotin.physics.CR3BPUtils;
import org.github.oleksandrkukotin.physics.JacobiConstant;
import org.github.oleksandrkukotin.physics.StateVectorPropagator;
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

import java.util.Arrays;
import java.util.function.DoubleBinaryOperator;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.containsStringIgnoringCase;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP contract for trajectories that cannot run to their requested duration.
 *
 * <ul>
 *   <li>Impact on a primary: 200, partial trajectory, {@code terminationReason} names the body.</li>
 *   <li>Normal run: 200, {@code terminationReason} is {@code COMPLETED}.</li>
 *   <li>No trajectory can be produced: 422 with a {@code message} explaining why.</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class SimulationControllerCollisionTest {

    private static final double ABS_TOL = 1e-10;
    private static final double REL_TOL = 1e-10;
    private static final double MIN_STEP = 1e-6;
    private static final double MAX_STEP = 1.0;

    private static final double JUPITER_RADIUS = 8.98e-5;
    private static final double SUN_RADIUS = 8.94e-4;
    private static final double RADIUS_TOLERANCE = 0.1;

    // See StateVectorPropagatorCollisionTest for how these seeds were derived.
    private static final StateVector JUPITER_IMPACT =
            new StateVector(1.14819396326, 0.164613921996, -0.062164220388, -0.27417844989);
    private static final StateVector SUN_IMPACT =
            new StateVector(0.70571396203, 0.21856960347, -1.352426650561, -1.192176007811);

    /**
     * Starting exactly on a primary makes the first derivative 0/0. NaN then defeats the
     * integrator's step control and it never returns, so without this limit the test
     * would hang the build instead of failing. Each propagation gets its own counter.
     */
    @TestConfiguration
    static class EvaluationLimitConfig {

        @Bean
        @Primary
        StateVectorPropagator evaluationLimitedPropagator(JacobiConstant jacobiConstant) {
            return new StateVectorPropagator(new CR3BPEquations(), jacobiConstant) {
                @Override
                public TrajectoryResult propagate(SimulationRequest request) {
                    return new StateVectorPropagator(new EvaluationLimitedEquations(), jacobiConstant)
                            .propagate(request);
                }
            };
        }
    }

    private static final class EvaluationLimitedEquations extends CR3BPEquations {
        private static final long MAX_EVALUATIONS = 1_000_000;

        private long evaluations;

        @Override
        public void computeDerivatives(double t, double[] y, double[] yDot) {
            if (++evaluations > MAX_EVALUATIONS) {
                throw new AssertionError("integrator still running after "
                        + MAX_EVALUATIONS + " evaluations, state " + Arrays.toString(y));
            }
            super.computeDerivatives(t, y, yDot);
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

    private static StateVector orbitAtTwoRadii(double centreX, double mass, double radius) {
        double r = 2 * radius;
        return new StateVector(centreX + r, 0.0, 0.0, Math.sqrt(mass / r));
    }

    private void assertPartialTrajectoryEndingAtSurface(ResultActions actions, String reason,
                                                        DoubleBinaryOperator distance, double radius) throws Exception {
        actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.terminationReason").value(reason))
                .andExpect(jsonPath("$.points", hasSize(greaterThan(1))));

        JsonNode points = objectMapper.readTree(actions.andReturn().getResponse().getContentAsString()).get("points");
        JsonNode finalState = points.get(points.size() - 1).get("state");
        assertEquals(radius, distance.applyAsDouble(finalState.get("x").asDouble(), finalState.get("y").asDouble()),
                RADIUS_TOLERANCE * radius, "final sample should be on the surface of the impacted body");
    }

    private static void assertCompleted(ResultActions actions) throws Exception {
        actions.andExpect(status().isOk())
                .andExpect(jsonPath("$.terminationReason").value("COMPLETED"));
    }

    private static void assertUnprocessableExplaining(ResultActions actions, String expectedTopic) throws Exception {
        actions.andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsStringIgnoringCase(expectedTopic)))
                .andExpect(jsonPath("$.message", not(containsString("Exception"))));
    }

    @Test
    void propagate_onJupiterImpact_returnsPartialTrajectoryNamingJupiter() throws Exception {
        assertPartialTrajectoryEndingAtSurface(propagate(JUPITER_IMPACT, 5.0, MIN_STEP),
                "COLLISION_JUPITER", CR3BPUtils::distanceToJupiter, JUPITER_RADIUS);
    }

    @Test
    void propagate_onSunImpact_returnsPartialTrajectoryNamingSun() throws Exception {
        assertPartialTrajectoryEndingAtSurface(propagate(SUN_IMPACT, 5.0, MIN_STEP),
                "COLLISION_SUN", CR3BPUtils::distanceToSun, SUN_RADIUS);
    }

    @Test
    void propagate_normalRun_reportsCompleted() throws Exception {
        assertCompleted(propagate(new StateVector(0.5, 0.1, 0.0, 0.5), 5.0, MIN_STEP));
    }

    @Test
    void propagate_orbitCloseToJupiter_reportsCompleted() throws Exception {
        StateVector s0 = orbitAtTwoRadii(PhysicsConstants.ONE_MINUS_MU, PhysicsConstants.MU, JUPITER_RADIUS);

        assertCompleted(propagate(s0, 0.01, MIN_STEP));
    }

    @Test
    void propagate_orbitCloseToSun_reportsCompleted() throws Exception {
        StateVector s0 = orbitAtTwoRadii(-PhysicsConstants.MU, PhysicsConstants.ONE_MINUS_MU, SUN_RADIUS);

        assertCompleted(propagate(s0, 0.01, MIN_STEP));
    }

    @Test
    void propagate_whenStepSizeFloorIsHitWithoutCollision_returns422() throws Exception {
        // A circular orbit at r = 0.01 of Jupiter, well clear of the body, but with a minStep
        // of 0.5 that the integrator cannot honour. No collision event can explain this one.
        double r = 0.01;
        StateVector s0 = new StateVector(PhysicsConstants.ONE_MINUS_MU + r, 0.0, 0.0,
                Math.sqrt(PhysicsConstants.MU / r));

        assertUnprocessableExplaining(propagate(s0, 0.5, 0.5), "step");
    }

    @Test
    void propagate_startingExactlyAtJupiterCentre_returns422NamingJupiter() throws Exception {
        assertUnprocessableExplaining(
                propagate(new StateVector(PhysicsConstants.ONE_MINUS_MU, 0.0, 0.0, 0.0), 1.0, MIN_STEP), "Jupiter");
    }

    @Test
    void propagate_startingExactlyAtSunCentre_returns422NamingSun() throws Exception {
        assertUnprocessableExplaining(
                propagate(new StateVector(-PhysicsConstants.MU, 0.0, 0.0, 0.0), 1.0, MIN_STEP), "Sun");
    }
}
