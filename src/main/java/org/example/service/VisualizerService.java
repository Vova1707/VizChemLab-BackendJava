package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.CompoundInfoDto;
import org.example.dto.VisualizeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

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
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Compound query is required");
        }

        List<String> candidates = buildCandidates(compound);
        for (String candidate : candidates) {
            for (String recordType : List.of("2d", "3d")) {
                String sdf = pubChemService.fetchSdfByName(candidate, recordType);
                if (sdf != null) {
                    Long cid = pubChemService.fetchCidByName(candidate);
                    return new VisualizeResponse(compound, "PubChem", "sdf", sdf, cid, null);
                }
            }
        }

        if (pubChemService.isFormula(compound)) {
            List<Long> cids = pubChemService.fetchCidsByFormula(compound);
            if (!cids.isEmpty()) {
                List<CompoundInfoDto> isomers = pubChemService.fetchCompoundNames(cids);
                long firstCid = cids.get(0);
                String sdf = pubChemService.fetchSdfByCid(firstCid, "3d");
                if (sdf == null) {
                    sdf = pubChemService.fetchSdfByCid(firstCid, "2d");
                }
                if (sdf != null) {
                    return new VisualizeResponse(compound, "PubChem", "sdf", sdf, firstCid, isomers);
                }
            }
        }

        boolean hasCyrillic = compound.chars().anyMatch(ch ->
            (ch >= 'а' && ch <= 'я') || (ch >= 'А' && ch <= 'Я') || ch == 'ё' || ch == 'Ё');
        if (hasCyrillic) {
            String translated = pubChemService.forceTranslateToEn(compound);
            if (translated != null && !translated.isBlank()) {
                for (String recordType : List.of("2d", "3d")) {
                    String sdf = pubChemService.fetchSdfByName(translated, recordType);
                    if (sdf != null) {
                        Long cid = pubChemService.fetchCidByName(translated);
                        return new VisualizeResponse(compound, "PubChem (translated: " + translated + ")", "sdf", sdf, cid, null);
                    }
                }
            }
        }

        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compound not found in PubChem database");
    }

    public VisualizeResponse visualizeByCid(long cid) {
        String sdf = pubChemService.fetchSdfByCid(cid, "3d");
        if (sdf == null) {
            sdf = pubChemService.fetchSdfByCid(cid, "2d");
        }
        if (sdf == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compound not found");
        }
        return new VisualizeResponse("CID:" + cid, "PubChem", "sdf", sdf, cid, null);
    }

    private List<String> buildCandidates(String compound) {
        Set<String> candidates = new LinkedHashSet<>();
        String translated = pubChemService.maybeTranslateToEn(compound);
        if (translated != null && !translated.isBlank()) {
            candidates.add(translated);
            if (translated.contains("g")) {
                candidates.add(translated.replace('g', 'h'));
            }
            if (translated.contains("ll")) {
                candidates.add(translated.replace("ll", "l"));
            }
            if (translated.contains("g") && translated.contains("ll")) {
                candidates.add(translated.replace('g', 'h').replace("ll", "l"));
            }
        }
        candidates.add(compound);
        return new ArrayList<>(candidates);
    }
}
