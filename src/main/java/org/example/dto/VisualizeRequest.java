package org.example.dto;

public class VisualizeRequest {
    private String formula;
    private String compound;

    public VisualizeRequest() {}

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public void setCompound(String compound) {
        this.compound = compound;
    }

    public String query() {
        if (formula != null && !formula.isBlank()) {
            return formula.trim();
        }
        if (compound != null && !compound.isBlank()) {
            return compound.trim();
        }
        return "";
    }
}
