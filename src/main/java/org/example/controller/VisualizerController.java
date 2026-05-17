package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.VisualizeRequest;
import org.example.dto.VisualizeResponse;
import org.example.service.VisualizerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("api")
public class VisualizerController {

    private final VisualizerService visualizerService;

    @PostMapping("/visualize")
    public ResponseEntity<?> visualize(@RequestBody VisualizeRequest request) {
        try {
            return ResponseEntity.ok(visualizerService.visualize(request.query()));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка сервера");
        }
    }

    @GetMapping("/visualize/cid/{cid}")
    public ResponseEntity<?> visualizeByCid(@PathVariable long cid) {
        try {
            return ResponseEntity.ok(visualizerService.visualizeByCid(cid));
        } catch (ResponseStatusException e) {
            return ResponseEntity.status(e.getStatusCode()).body(e.getReason());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка сервера");
        }
    }
}
