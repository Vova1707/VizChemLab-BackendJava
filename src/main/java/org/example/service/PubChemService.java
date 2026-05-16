package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.CompoundInfoDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class PubChemService {

    private static final String PUBCHEM_BASE = "https://pubchem.ncbi.nlm.nih.gov/rest/pug/compound";
    private static final String MYMEMORY_URL = "https://api.mymemory.translated.net/get";
    private static final Pattern FORMULA_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final java.util.Map<String, String> RUSSIAN_NAMES = java.util.Map.of(
        "вода", "water",
        "метан", "methane",
        "этан", "ethane",
        "пропан", "propane",
        "бутан", "butane",
        "этилен", "ethylene",
        "пропилен", "propylene",
        "бутилен", "butylene",
        "ацетилен", "acetylene",
        "бензол", "benzene",
        "толуол", "toluene",
        "фенол", "phenol",
        "метанол", "methanol",
        "этанол", "ethanol",
        "пропанол", "propanol",
        "бутанол", "butanol",
        "глицерин", "glycerol",
        "глюкоза", "glucose",
        "фруктоза", "fructose",
        "сахароза", "sucrose",
        "уксусная кислота", "acetic acid",
        "соляная кислота", "hydrochloric acid",
        "серная кислота", "sulfuric acid",
        "азотная кислота", "nitric acid",
        "муравьиная кислота", "formic acid",
        "мочевина", "urea",
        "ацетон", "acetone",
        "формальдегид", "formaldehyde",
        "аммиак", "ammonia",
        "сернистый газ", "sulfur dioxide",
        "углекислый газ", "carbon dioxide",
        "оксид углерода", "carbon monoxide",
        "метиловый спирт", "methanol",
        "этиловый спирт", "ethanol",
        "нашатырь", "ammonium chloride"
    );

    public String maybeTranslateToEn(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.toLowerCase().trim();
        boolean hasCyrillic = text.chars().anyMatch(ch ->
                (ch >= 'а' && ch <= 'я') || (ch >= 'А' && ch <= 'Я') || ch == 'ё' || ch == 'Ё');
        if (!hasCyrillic) {
            return null;
        }
        // First check local dictionary
        if (RUSSIAN_NAMES.containsKey(lower)) {
            return RUSSIAN_NAMES.get(lower);
        }
        // Try translation API with fallback
        try {
            String url = MYMEMORY_URL + "?q=" + encode(text) + "&langpair=ru|en";
            JsonNode root = objectMapper.readTree(restClient.get().uri(url).retrieve().body(String.class));
            String translated = root.path("responseData").path("translatedText").asText(null);
            if (translated != null && !translated.isBlank() && !translated.equalsIgnoreCase(text)) {
                System.out.println("Translated '" + text + "' to '" + translated + "'");
                return translated;
            }
        } catch (Exception e) {
            System.err.println("Translation failed for '" + text + "': " + e.getMessage());
        }
        // Return original if translation fails - PubChem might still recognize some names
        return text;
    }

    public String fetchSdfByName(String query, String recordType) {
        try {
            String url = PUBCHEM_BASE + "/name/" + encode(query) + "/SDF?record_type=" + recordType;
            String body = restClient.get().uri(url).retrieve().body(String.class);
            return isValidSdf(body) ? body : null;
        } catch (Exception e) {
            return null;
        }
    }

    public String fetchSdfByCid(long cid, String recordType) {
        try {
            String url = PUBCHEM_BASE + "/cid/" + cid + "/SDF?record_type=" + recordType;
            String body = restClient.get().uri(url).retrieve().body(String.class);
            return isValidSdf(body) ? body : null;
        } catch (Exception e) {
            return null;
        }
    }

    public Long fetchCidByName(String name) {
        try {
            String url = PUBCHEM_BASE + "/name/" + encode(name) + "/cids/JSON";
            JsonNode root = objectMapper.readTree(restClient.get().uri(url).retrieve().body(String.class));
            JsonNode cids = root.path("IdentifierList").path("CID");
            if (cids.isArray() && !cids.isEmpty()) {
                return cids.get(0).asLong();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public List<Long> fetchCidsByFormula(String formula) {
        List<Long> result = new ArrayList<>();
        try {
            String fastUrl = PUBCHEM_BASE + "/fastformula/" + encode(formula) + "/cids/JSON?MaxRecords=25";
            JsonNode root = objectMapper.readTree(restClient.get().uri(fastUrl).retrieve().body(String.class));
            addCids(root, result);
            if (!result.isEmpty()) {
                return result;
            }
            String formulaUrl = PUBCHEM_BASE + "/formula/" + encode(formula) + "/cids/JSON?MaxRecords=25";
            root = objectMapper.readTree(restClient.get().uri(formulaUrl).retrieve().body(String.class));
            addCids(root, result);
        } catch (Exception ignored) {
        }
        return result;
    }

    public List<CompoundInfoDto> fetchCompoundNames(List<Long> cids) {
        if (cids == null || cids.isEmpty()) {
            return List.of();
        }
        List<Long> subset = cids.size() > 10 ? cids.subList(0, 10) : cids;
        String cidList = subset.stream().map(String::valueOf).reduce((a, b) -> a + "," + b).orElse("");
        List<CompoundInfoDto> results = new ArrayList<>();
        Set<Long> processed = new LinkedHashSet<>();
        try {
            String url = PUBCHEM_BASE + "/cid/" + cidList
                    + "/property/IUPACName,MolecularFormula,MolecularWeight/JSON";
            JsonNode root = objectMapper.readTree(restClient.get().uri(url).retrieve().body(String.class));
            for (JsonNode prop : root.path("PropertyTable").path("Properties")) {
                long cid = prop.path("CID").asLong();
                processed.add(cid);
                results.add(new CompoundInfoDto(
                        cid,
                        prop.path("IUPACName").asText("Compound " + cid),
                        prop.path("IUPACName").asText("Isomer " + cid),
                        prop.path("MolecularFormula").asText("Unknown"),
                        prop.has("MolecularWeight") ? prop.path("MolecularWeight").asDouble() : 0.0
                ));
            }
        } catch (Exception ignored) {
        }
        for (Long cid : subset) {
            if (!processed.contains(cid)) {
                results.add(defaultIsomer(cid));
            }
        }
        return results;
    }

    public CompoundInfoDto defaultIsomer(long cid) {
        return new CompoundInfoDto(cid, "Isomer " + cid, "Isomer " + cid, "Unknown", 0.0);
    }

    public String createFallbackSdf(String compound) {
        String upper = compound == null ? "" : compound.toUpperCase();
        if (upper.contains("C4H8")) {
            return """
                  
                  Butene
                  
                  4  3  0  0  0  0  0  0  0  0999 V2000
                    0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
                    1.4000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
                    2.1000    1.2124    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
                    3.5000    1.2124    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
                  1  2  1  0  0  0  0
                  2  3  1  0  0  0  0
                  3  4  1  0  0  0  0
                M  END
                """;
        }
        if (upper.contains("H2O") || upper.contains("WATER") || compound.toLowerCase().contains("вода")) {
            return """
                  
                  Water
                  
                  3  2  0  0  0  0  0  0  0  0999 V2000
                    0.0000    0.0000    0.0000 O   0  0  0  0  0  0  0  0  0  0  0  0
                    0.0000    0.0000    1.0000 H   0  0  0  0  0  0  0  0  0  0  0  0
                    0.0000    1.0000    0.0000 H   0  0  0  0  0  0  0  0  0  0  0  0
                  1  2  1  0  0  0  0
                  1  3  1  0  0  0  0
                M  END
                """;
        }
        return """
              
              %s
              
              2  1  0  0  0  0  0  0  0  0999 V2000
                0.0000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
                1.4000    0.0000    0.0000 C   0  0  0  0  0  0  0  0  0  0  0  0
              1  2  1  0  0  0  0
            M  END
            """.formatted(compound);
    }

    public boolean isFormula(String text) {
        return text != null && FORMULA_PATTERN.matcher(text).matches();
    }

    private void addCids(JsonNode root, List<Long> target) {
        JsonNode cids = root.path("IdentifierList").path("CID");
        if (cids.isArray()) {
            for (JsonNode cid : cids) {
                target.add(cid.asLong());
            }
        } else if (cids.isNumber()) {
            target.add(cids.asLong());
        }
    }

    private boolean isValidSdf(String sdf) {
        return sdf != null && sdf.trim().length() > 50;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
