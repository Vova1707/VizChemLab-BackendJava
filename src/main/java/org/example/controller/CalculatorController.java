package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.MolarMassRequest;
import org.example.dto.MolarMassResponse;
import org.example.service.MolarMassService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/calculator")
public class CalculatorController {

    private final MolarMassService molarMassService;

    @PostMapping("/molar-mass")
    public ResponseEntity<MolarMassResponse> molarMass(@RequestBody MolarMassRequest request) {
        return ResponseEntity.ok(molarMassService.calculate(request.getFormula()));
    }
}
