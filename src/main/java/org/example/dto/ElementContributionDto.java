package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ElementContributionDto {
    private String symbol;
    private int count;
    @JsonProperty("atomic_mass")
    private double atomicMass;
    @JsonProperty("total_mass")
    private double totalMass;

    public ElementContributionDto() {
    }

    public ElementContributionDto(String symbol, int count, double atomicMass, double totalMass) {
        this.symbol = symbol;
        this.count = count;
        this.atomicMass = atomicMass;
        this.totalMass = totalMass;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getCount() {
        return count;
    }

    public double getAtomicMass() {
        return atomicMass;
    }

    public double getTotalMass() {
        return totalMass;
    }
}
