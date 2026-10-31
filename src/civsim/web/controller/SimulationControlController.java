package civsim.web.controller;

import civsim.dto.CommandStatusDto;
import civsim.dto.SimulationStatusDto;
import civsim.dto.SpeedChangeRequestDto;
import civsim.runner.CommandType;
import civsim.runner.SimulationCommand;
import civsim.runner.SimulationRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller exposing simulation playback, step, speed, and lifecycle controls.
 */
@RestController
@RequestMapping("/api/simulation")
public class SimulationControlController {

    private final SimulationRunner runner;

    public SimulationControlController(SimulationRunner runner) {
        this.runner = runner;
    }

    @GetMapping("/status")
    public ResponseEntity<SimulationStatusDto> getStatus() {
        return ResponseEntity.ok(runner.getStatus());
    }

    @PostMapping("/pause")
    public ResponseEntity<CommandStatusDto> pause() {
        SimulationCommand cmd = new SimulationCommand(CommandType.PAUSE, null);
        runner.submitCommand(cmd);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(cmd.toDto());
    }

    @PostMapping("/resume")
    public ResponseEntity<CommandStatusDto> resume() {
        SimulationCommand cmd = new SimulationCommand(CommandType.RESUME, null);
        runner.submitCommand(cmd);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(cmd.toDto());
    }

    @PostMapping({"/step/month", "/step-month"})
    public ResponseEntity<CommandStatusDto> stepMonth() {
        SimulationCommand cmd = new SimulationCommand(CommandType.STEP_MONTH, null);
        runner.submitCommand(cmd);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(cmd.toDto());
    }

    @PostMapping({"/step/year", "/step-year"})
    public ResponseEntity<CommandStatusDto> stepYear() {
        SimulationCommand cmd = new SimulationCommand(CommandType.STEP_YEAR, null);
        runner.submitCommand(cmd);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(cmd.toDto());
    }

    @PostMapping({"/speed", "/set-speed"})
    public ResponseEntity<CommandStatusDto> setSpeed(@RequestBody SpeedChangeRequestDto request) {
        String speedStr = (request != null && request.speed() != null) ? request.speed() : "NORMAL";
        SimulationCommand cmd = new SimulationCommand(CommandType.SET_SPEED, speedStr);
        runner.submitCommand(cmd);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(cmd.toDto());
    }

    @GetMapping("/commands/{commandId}")
    public ResponseEntity<CommandStatusDto> getCommandStatus(@PathVariable String commandId) {
        SimulationCommand cmd = runner.getCommand(commandId);
        if (cmd == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(cmd.toDto());
    }
}

