package org.example.dto;

public class SimulateRequest {
    private String reactants;

    public SimulateRequest() {}

    public String getReactants() {
        return reactants != null ? reactants.trim() : "";
    }

    public void setReactants(String reactants) {
        this.reactants = reactants;
    }
}
