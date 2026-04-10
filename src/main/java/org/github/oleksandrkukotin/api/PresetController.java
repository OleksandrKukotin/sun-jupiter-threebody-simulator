package org.github.oleksandrkukotin.api;

import org.github.oleksandrkukotin.model.OrbitPreset;
import org.github.oleksandrkukotin.model.SimulationRequest;
import org.github.oleksandrkukotin.model.TrajectoryResult;
import org.github.oleksandrkukotin.physics.StateVectorPropagator;
import org.github.oleksandrkukotin.presets.OrbitPresets;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/presets")
public class PresetController {

    private final OrbitPresets orbitPresets;
    private final StateVectorPropagator propagator;

    public PresetController(OrbitPresets orbitPresets, StateVectorPropagator propagator) {
        this.orbitPresets = orbitPresets;
        this.propagator = propagator;
    }

    /** Lists all available orbit presets. */
    @GetMapping
    public List<OrbitPreset> listPresets() {
        return orbitPresets.getAll();
    }

    /** Runs a named preset through the propagator and returns the trajectory. */
    @PostMapping("/{id}/run")
    public TrajectoryResult runPreset(@PathVariable String id) {
        OrbitPreset preset = orbitPresets.findById(id);
        SimulationRequest request = new SimulationRequest(
                preset.initialState(),
                preset.duration(),
                1e-10, 1e-10, 1e-6, 1.0
        );
        return propagator.propagate(request);
    }
}