package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.VisualizeRequest;
import org.example.dto.VisualizeResponse;
import org.example.service.VisualizerService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api")
public class VisualizerController {

    private final VisualizerService visualizerService;

    @PostMapping("/visualize")
    public ResponseEntity<VisualizeResponse> visualize(@RequestBody VisualizeRequest request) {
        return ResponseEntity.ok(visualizerService.visualize(request.query()));
    }

    @GetMapping("/visualize/cid/{cid}")
    public ResponseEntity<VisualizeResponse> visualizeByCid(@PathVariable long cid) {
        return ResponseEntity.ok(visualizerService.visualizeByCid(cid));
    }
}
