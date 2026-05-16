package org.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CompoundInfoDto {
    private long cid;
    private String name;
    @JsonProperty("iupac_name")
    private String iupacName;
    @JsonProperty("molecular_formula")
    private String molecularFormula;
    @JsonProperty("molecular_weight")
    private Double molecularWeight;

    public CompoundInfoDto() {
    }

    public CompoundInfoDto(long cid, String name, String iupacName, String molecularFormula, Double molecularWeight) {
        this.cid = cid;
        this.name = name;
        this.iupacName = iupacName;
        this.molecularFormula = molecularFormula;
        this.molecularWeight = molecularWeight;
    }

    public long getCid() {
        return cid;
    }

    public String getName() {
        return name;
    }

    public String getIupacName() {
        return iupacName;
    }

    public String getMolecularFormula() {
        return molecularFormula;
    }

    public Double getMolecularWeight() {
        return molecularWeight;
    }
}
