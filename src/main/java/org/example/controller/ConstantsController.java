package org.example.controller;

import org.example.dto.ChemicalElementDto;
import org.example.service.ChemicalElementService;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ConstantsController {
    
    private final ChemicalElementService elementService;
    
    public ConstantsController(ChemicalElementService elementService) {
        this.elementService = elementService;
    }
    
    @GetMapping("/constants")
    public Map<String, Object> getConstants() {
        Map<String, Object> response = new HashMap<>();
        response.put("periodic_table", elementService.getAllElements());
        return response;
    }
    
    @GetMapping("/elements")
    public List<ChemicalElementDto> getAllElements() {
        return elementService.getAllElements();
    }
    
    @GetMapping("/elements/{symbol}")
    public ChemicalElementDto getElementBySymbol(@PathVariable String symbol) {
        return elementService.getElementBySymbol(symbol);
    }
}
