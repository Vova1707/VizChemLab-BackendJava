package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.CompoundInfoDto;
import org.example.dto.VisualizeResponse;
import org.example.exception.CompoundNotFoundException;
import org.example.exception.InvalidInputException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VisualizerService {

    private final PubChemService pubChemService;

    public VisualizeResponse visualize(String compound) {

        if (compound == null || compound.isBlank()) {
            throw new InvalidInputException("Compound query is required");
        }

        // Normalize formula-like input: "c4h8" → "C4H8"
        String normalized = pubChemService.isFormula(compound) ? compound.toUpperCase() : compound;

        // If it looks like a formula, try formula search first to avoid wrong name matches
        if (pubChemService.isFormula(normalized)) {
            List<Long> cids = pubChemService.fetchCidsByFormula(normalized);
            if (!cids.isEmpty()) {
                List<CompoundInfoDto> isomers = pubChemService.fetchCompoundNames(cids);
                long firstCid = cids.get(0);
                for (String recordType : List.of("3d", "2d")) {
                    String sdf = pubChemService.fetchSdfByCid(firstCid, recordType);
                    if (sdf != null) {
                        return new VisualizeResponse(compound, "PubChem", "sdf", sdf, firstCid, isomers.size() > 1 ? isomers : null);
                    }
                }
            }
        }

        List<String> candidates = buildCandidates(normalized);

        System.out.println("[VIZ] candidates: " + candidates);

        for (String candidate : candidates) {
            for (String recordType : List.of("3d", "2d")) {
                String sdf = pubChemService.fetchSdfByName(candidate, recordType);
                System.out.println("[VIZ] fetchSdfByName('" + candidate + "', " + recordType + ") = " + (sdf != null ? "OK" : "null"));
                if (sdf != null) {
                    Long cid = pubChemService.fetchCidByName(candidate);
                    return new VisualizeResponse(compound, "PubChem", "sdf", sdf, cid, null);
                }
            }
            Long cid = pubChemService.fetchCidByName(candidate);
            System.out.println("[VIZ] fetchCidByName('" + candidate + "') = " + cid);
            if (cid != null) {
                for (String recordType : List.of("3d", "2d")) {
                    String sdf = pubChemService.fetchSdfByCid(cid, recordType);
                    System.out.println("[VIZ] fetchSdfByCid(" + cid + ", " + recordType + ") = " + (sdf != null ? "OK" : "null"));
                    if (sdf != null) {
                        return new VisualizeResponse(compound, "PubChem", "sdf", sdf, cid, null);
                    }
                }
            }
        }


        boolean hasCyrillic = compound.chars().anyMatch(ch ->
            (ch >= 'а' && ch <= 'я') || (ch >= 'А' && ch <= 'Я') || ch == 'ё' || ch == 'Ё');
        if (hasCyrillic) {
            String translated = pubChemService.forceTranslateToEn(compound);
            if (translated != null && !translated.isBlank()) {
                for (String recordType : List.of("3d", "2d")) {
                    String sdf = pubChemService.fetchSdfByName(translated, recordType);
                    if (sdf != null) {
                        Long cid = pubChemService.fetchCidByName(translated);
                        return new VisualizeResponse(compound, "PubChem (translated: " + translated + ")", "sdf", sdf, cid, null);
                    }
                }
                Long cid = pubChemService.fetchCidByName(translated);
                if (cid != null) {
                    for (String recordType : List.of("3d", "2d")) {
                        String sdf = pubChemService.fetchSdfByCid(cid, recordType);
                        if (sdf != null) {
                            return new VisualizeResponse(compound, "PubChem (translated: " + translated + ")", "sdf", sdf, cid, null);
                        }
                    }
                }
            }
        }

        Long cidFallback = pubChemService.fetchCidByName(compound);
        if (cidFallback != null) {
            for (String recordType : List.of("3d", "2d")) {
                String sdf = pubChemService.fetchSdfByCid(cidFallback, recordType);
                if (sdf != null) {
                    return new VisualizeResponse(compound, "PubChem", "sdf", sdf, cidFallback, null);
                }
            }
        }

        List<String> autocompleteQueries = new ArrayList<>(candidates);
        autocompleteQueries.add(compound);
        for (String query : autocompleteQueries) {
            Long cid = pubChemService.fetchCidByAutocomplete(query);
            if (cid != null) {
                for (String recordType : List.of("3d", "2d")) {
                    String sdf = pubChemService.fetchSdfByCid(cid, recordType);
                    if (sdf != null) {
                        return new VisualizeResponse(compound, "PubChem", "sdf", sdf, cid, null);
                    }
                }
            }
        }

        throw new CompoundNotFoundException("Compound not found in PubChem database");
    }

    public VisualizeResponse visualizeByCid(long cid) {
        String sdf = pubChemService.fetchSdfByCid(cid, "3d");
        if (sdf == null) {
            sdf = pubChemService.fetchSdfByCid(cid, "2d");
        }
        if (sdf == null) {
            throw new CompoundNotFoundException("Compound not found");
        }
        return new VisualizeResponse("CID:" + cid, "PubChem", "sdf", sdf, cid, null);
    }

    private List<String> buildCandidates(String compound) {
        Set<String> candidates = new LinkedHashSet<>();
        String translated = pubChemService.maybeTranslateToEn(compound);
        if (translated != null && !translated.isBlank()) {
            candidates.add(translated);
            String americanized = toAmericanSpelling(translated);
            if (!americanized.equals(translated)) {
                candidates.add(americanized);
            }
            if (translated.contains("g")) {
                String variant = translated.replace('g', 'h');
                candidates.add(variant);
                candidates.add(toAmericanSpelling(variant));
            }
            if (translated.contains("ll")) {
                String variant = translated.replace("ll", "l");
                candidates.add(variant);
                candidates.add(toAmericanSpelling(variant));
            }
        }
        candidates.add(compound);
        return new ArrayList<>(candidates);
    }

    private String toAmericanSpelling(String text) {
        return text
            .replace("sulph", "sulf")
            .replace("Sulph", "Sulf")
            .replace("aluminium", "aluminum")
            .replace("Aluminium", "Aluminum")
            .replace("centre", "center")
            .replace("colour", "color");
    }
}
