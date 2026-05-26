package org.example.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "periodic_table")
public class ChemicalElement {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String symbol;
    
    @Column(nullable = false)
    private String name;
    
    @Column(name = "name_ru")
    private String nameRu;
    
    @Column(name = "atomic_number")
    private Integer atomicNumber;
    
    @Column(name = "atomic_mass")
    private Double atomicMass;
    
    private String color;
    
    @Column(name = "radius_angstrom")
    private Double radiusAngstrom;
    
    @Column(name = "group_number")
    private Integer groupNumber;
    
    private Integer period;
    
    private String category;
    
    private Integer valence;

    public ChemicalElement() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
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
    
    public Double getRadiusAngstrom() { return radiusAngstrom; }
    public void setRadiusAngstrom(Double radiusAngstrom) { this.radiusAngstrom = radiusAngstrom; }
    
    public Integer getGroupNumber() { return groupNumber; }
    public void setGroupNumber(Integer groupNumber) { this.groupNumber = groupNumber; }
    
    public Integer getPeriod() { return period; }
    public void setPeriod(Integer period) { this.period = period; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public Integer getValence() { return valence; }
    public void setValence(Integer valence) { this.valence = valence; }
}
