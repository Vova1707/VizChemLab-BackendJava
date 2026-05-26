package org.example.dto;

import java.util.List;
import java.util.Map;

public class SimulateVisualizeResponse {
    private String reactants;
    private String equation;
    private String rawEquation;
    private ReactionInfo info;
    private List<Frame> frames;
    private List<MoleculeModel> models;
    private String modelError;

    public SimulateVisualizeResponse() {}

    public String getReactants() { return reactants; }
    public void setReactants(String reactants) { this.reactants = reactants; }

    public String getEquation() { return equation; }
    public void setEquation(String equation) { this.equation = equation; }

    public String getRawEquation() { return rawEquation; }
    public void setRawEquation(String rawEquation) { this.rawEquation = rawEquation; }

    public ReactionInfo getInfo() { return info; }
    public void setInfo(ReactionInfo info) { this.info = info; }

    public List<Frame> getFrames() { return frames; }
    public void setFrames(List<Frame> frames) { this.frames = frames; }

    public List<MoleculeModel> getModels() { return models; }
    public void setModels(List<MoleculeModel> models) { this.models = models; }

    public String getModelError() { return modelError; }
    public void setModelError(String modelError) { this.modelError = modelError; }

    public static class ReactionInfo {
        private List<String> reactants;
        private List<String> products;
        private int elements;

        public ReactionInfo() {}

        public List<String> getReactants() { return reactants; }
        public void setReactants(List<String> reactants) { this.reactants = reactants; }

        public List<String> getProducts() { return products; }
        public void setProducts(List<String> products) { this.products = products; }

        public int getElements() { return elements; }
        public void setElements(int elements) { this.elements = elements; }
    }

    public static class Frame {
        private String title;
        private List<Map<String, Object>> atoms;
        private List<Map<String, Object>> bonds;
        private double progress;

        public Frame() {}

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public List<Map<String, Object>> getAtoms() { return atoms; }
        public void setAtoms(List<Map<String, Object>> atoms) { this.atoms = atoms; }

        public List<Map<String, Object>> getBonds() { return bonds; }
        public void setBonds(List<Map<String, Object>> bonds) { this.bonds = bonds; }

        public double getProgress() { return progress; }
        public void setProgress(double progress) { this.progress = progress; }
    }

    public static class MoleculeModel {
        private String compound;
        private String side;
        private List<Atom> atoms;
        private List<Bond> bonds;

        public MoleculeModel() {}

        public String getCompound() { return compound; }
        public void setCompound(String compound) { this.compound = compound; }

        public String getSide() { return side; }
        public void setSide(String side) { this.side = side; }

        public List<Atom> getAtoms() { return atoms; }
        public void setAtoms(List<Atom> atoms) { this.atoms = atoms; }

        public List<Bond> getBonds() { return bonds; }
        public void setBonds(List<Bond> bonds) { this.bonds = bonds; }
    }

    public static class Atom {
        private String element;
        private double x;
        private double y;
        private double z;

        public Atom() {}

        public String getElement() { return element; }
        public void setElement(String element) { this.element = element; }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }

        public double getY() { return y; }
        public void setY(double y) { this.y = y; }

        public double getZ() { return z; }
        public void setZ(double z) { this.z = z; }
    }

    public static class Bond {
        private int start;
        private int end;
        private int order;

        public Bond() {}

        public int getStart() { return start; }
        public void setStart(int start) { this.start = start; }

        public int getEnd() { return end; }
        public void setEnd(int end) { this.end = end; }

        public int getOrder() { return order; }
        public void setOrder(int order) { this.order = order; }
    }
}
