package org.example.dto;

import java.util.List;
import java.util.Map;

public class SimulateResponse {
    private String reactants;
    private String equation;
    private String rawEquation;
    private ReactionInfo info;

    public SimulateResponse() {}

    public SimulateResponse(String reactants, String equation, String rawEquation, ReactionInfo info) {
        this.reactants = reactants;
        this.equation = equation;
        this.rawEquation = rawEquation;
        this.info = info;
    }

    public String getReactants() { return reactants; }
    public void setReactants(String reactants) { this.reactants = reactants; }

    public String getEquation() { return equation; }
    public void setEquation(String equation) { this.equation = equation; }

    public String getRawEquation() { return rawEquation; }
    public void setRawEquation(String rawEquation) { this.rawEquation = rawEquation; }

    public ReactionInfo getInfo() { return info; }
    public void setInfo(ReactionInfo info) { this.info = info; }

    public static class ReactionInfo {
        private List<String> reactants;
        private List<String> products;
        private int elements;

        public ReactionInfo() {}

        public ReactionInfo(List<String> reactants, List<String> products, int elements) {
            this.reactants = reactants;
            this.products = products;
            this.elements = elements;
        }

        public List<String> getReactants() { return reactants; }
        public void setReactants(List<String> reactants) { this.reactants = reactants; }

        public List<String> getProducts() { return products; }
        public void setProducts(List<String> products) { this.products = products; }

        public int getElements() { return elements; }
        public void setElements(int elements) { this.elements = elements; }
    }
}
