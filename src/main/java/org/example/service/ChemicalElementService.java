package org.example.service;

import org.example.dto.ChemicalElementDto;
import org.example.entity.ChemicalElement;
import org.example.repository.ChemicalElementRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChemicalElementService {
    
    private final ChemicalElementRepository repository;
    
    public ChemicalElementService(ChemicalElementRepository repository) {
        this.repository = repository;
    }
    
    public List<ChemicalElementDto> getAllElements() {
        return repository.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }
    
    public ChemicalElementDto getElementBySymbol(String symbol) {
        return repository.findBySymbol(symbol)
            .map(this::toDto)
            .orElse(null);
    }
    
    private ChemicalElementDto toDto(ChemicalElement element) {
        return new ChemicalElementDto(
            element.getSymbol(),
            element.getName(),
            element.getNameRu() != null ? element.getNameRu() : element.getName(),
            element.getAtomicNumber() != null ? element.getAtomicNumber() : 0,
            element.getAtomicMass() != null ? element.getAtomicMass() : 0.0,
            element.getColor() != null ? element.getColor() : "#CCCCCC",
            element.getRadiusAngstrom() != null ? element.getRadiusAngstrom() : 0.5,
            element.getGroupNumber() != null ? element.getGroupNumber() : 0,
            element.getPeriod() != null ? element.getPeriod() : 0,
            element.getCategory() != null ? element.getCategory() : "unknown"
        );
    }
}
