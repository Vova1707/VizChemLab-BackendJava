package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.SimulateRequest;
import org.example.dto.SimulateResponse;
import org.example.dto.SimulateVisualizeResponse;
import org.example.service.SimulatorService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("api")
public class SimulatorController {

    private final SimulatorService simulatorService;

    @PostMapping("/simulate")
    public ResponseEntity<?> simulate(@RequestBody SimulateRequest request) {
        try {
            String reactants = request.getReactants();
            if (reactants.isBlank()) {
                return ResponseEntity.badRequest().body("Реагенты не указаны");
            }
            SimulateResponse response = simulatorService.simulate(reactants);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Ошибка при симуляции: " + e.getMessage());
        }
    }

    @PostMapping("/simulate-visualize")
    public ResponseEntity<?> simulateVisualize(@RequestBody SimulateRequest request) {
        try {
            String reactants = request.getReactants();
            if (reactants.isBlank()) {
                return ResponseEntity.badRequest().body("Реагенты не указаны");
            }
            SimulateVisualizeResponse response = simulatorService.simulateVisualize(reactants);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Ошибка при симуляции: " + e.getMessage());
        }
    }
}
