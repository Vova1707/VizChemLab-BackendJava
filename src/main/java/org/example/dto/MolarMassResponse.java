package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

import java.util.List;

public class MolarMassResponse {
    @Getter private String formula;
    @JsonProperty("molar_mass")
    @Getter private double molarMass;
    @Getter private String unit;
    @Getter private List<ElementContributionDto> elements;

    public MolarMassResponse(String formula, double molarMass, String unit, List<ElementContributionDto> elements) {
        this.formula = formula;
        this.molarMass = molarMass;
        this.unit = unit;
        this.elements = elements;
    }
}
