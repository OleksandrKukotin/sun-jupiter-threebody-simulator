package org.github.oleksandrkukotin.api;

import org.github.oleksandrkukotin.model.ManifoldRequest;
import org.github.oleksandrkukotin.model.ManifoldResult;
import org.github.oleksandrkukotin.physics.InvariantManifoldCalculator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manifolds")
public class ManifoldController {

    private final InvariantManifoldCalculator calculator;

    public ManifoldController(InvariantManifoldCalculator calculator) {
        this.calculator = calculator;
    }

    /** Computes the four invariant manifold branches of a collinear Lagrange point (L1–L3). */
    @GetMapping("/{lagrangePointId}")
    public ManifoldResult compute(
            @PathVariable String lagrangePointId,
            @RequestParam(defaultValue = "1e-5")  double perturbation,
            @RequestParam(defaultValue = "6.0")   double duration,
            @RequestParam(defaultValue = "1e-10") double absoluteTolerance,
            @RequestParam(defaultValue = "1e-10") double relativeTolerance,
            @RequestParam(defaultValue = "1e-10") double minStep,
            @RequestParam(defaultValue = "0.1")   double maxStep) {
        return calculator.compute(new ManifoldRequest(lagrangePointId, perturbation, duration,
                absoluteTolerance, relativeTolerance, minStep, maxStep));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleInvalidRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
