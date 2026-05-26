package org.example.dto;

public class ChemicalElementDto {
    private String symbol;
    private String name;
    private String nameRu;
    private Integer atomicNumber;
    private Double atomicMass;
    private String color;
    private Double radius;
    private Integer groupNumber;
    private Integer period;
    private String category;

    public ChemicalElementDto(String symbol, String name, String nameRu, Integer atomicNumber,
                               Double atomicMass, String color, Double radius,
                               Integer groupNumber, Integer period, String category) {
        this.symbol = symbol;
        this.name = name;
        this.nameRu = nameRu;
        this.atomicNumber = atomicNumber;
        this.atomicMass = atomicMass;
        this.color = color;
        this.radius = radius;
        this.groupNumber = groupNumber;
        this.period = period;
        this.category = category;
    }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getNameRu() { return nameRu; }
    public void setNameRu(String nameRu) { this.nameRu = nameRu; }
    
    public Integer getAtomicNumber() { return atomicNumber; }
    public void setAtomicNumber(Integer atomicNumber) { this.atomicNumber = atomicNumber; }
    
    public Double getAtomicMass() { return atomicMass; }
    public void setAtomicMass(Double atomicMass) { this.atomicMass = atomicMass; }
    
    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
    
    public Double getRadius() { return radius; }
    public void setRadius(Double radius) { this.radius = radius; }
    
    public Integer getGroupNumber() { return groupNumber; }
    public void setGroupNumber(Integer groupNumber) { this.groupNumber = groupNumber; }
    
    public Integer getPeriod() { return period; }
    public void setPeriod(Integer period) { this.period = period; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
