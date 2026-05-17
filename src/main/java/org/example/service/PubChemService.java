package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.dto.CompoundInfoDto;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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
    private static final String GOOGLE_TRANSLATE_URL = "https://translate.googleapis.com/translate_a/single";
    private static final Pattern FORMULA_PATTERN = Pattern.compile("^[A-Za-z0-9]+$");

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private String translateWithGoogle(String text) {
        try {
            String response = restClient.get()
                .uri(GOOGLE_TRANSLATE_URL, builder -> builder
                    .queryParam("client", "gtx")
                    .queryParam("sl", "ru")
                    .queryParam("tl", "en")
                    .queryParam("dt", "t")
                    .queryParam("q", text)
                    .build())
                .retrieve()
                .body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode firstBlock = root.get(0);
            if (firstBlock != null && firstBlock.isArray()) {
                StringBuilder sb = new StringBuilder();
                for (JsonNode segment : firstBlock) {
                    if (segment.isArray() && segment.size() > 0) {
                        String part = segment.get(0).asText("");
                        if (!part.isBlank()) sb.append(part);
                    }
                }
                String result = sb.toString().trim();
                if (!result.isBlank()) return result;
            }
        } catch (Exception e) {
            System.err.println("Google Translate failed for '" + text + "': " + e.getMessage());
        }
        return null;
    }

    public String maybeTranslateToEn(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        boolean hasCyrillic = text.chars().anyMatch(ch ->
                (ch >= 'а' && ch <= 'я') || (ch >= 'А' && ch <= 'Я') || ch == 'ё' || ch == 'Ё');
        if (!hasCyrillic) {
            return null;
        }
        String translated = translateWithGoogle(text);
        if (translated != null) {
            System.out.println("Translated '" + text + "' to '" + translated + "'");
            return translated;
        }
        return null;
    }

    public String forceTranslateToEn(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String translated = translateWithGoogle(text);
        if (translated != null) {
            System.out.println("Force translated '" + text + "' to '" + translated + "'");
            return translated;
        }
        return null;
    }

    public String fetchSdfByName(String query, String recordType) {
        String url = PUBCHEM_BASE + "/name/" + encode(query) + "/SDF?record_type=" + recordType;
        String body = httpGet(url);
        return isValidSdf(body) ? body : null;
    }

    public String fetchSdfByCid(long cid, String recordType) {
        String url = PUBCHEM_BASE + "/cid/" + cid + "/SDF?record_type=" + recordType;
        String body = httpGet(url);
        return isValidSdf(body) ? body : null;
    }

    public Long fetchCidByName(String name) {
        try {
            String url = PUBCHEM_BASE + "/name/" + encode(name) + "/cids/JSON";
            String body = httpGet(url);
            if (body == null) return null;
            JsonNode root = objectMapper.readTree(body);
            JsonNode cids = root.path("IdentifierList").path("CID");
            if (cids.isArray() && !cids.isEmpty()) {
                return cids.get(0).asLong();
            }
        } catch (Exception e) {
            System.err.println("[PubChem] fetchCidByName error: " + e.getMessage());
        }
        return null;
    }

    public Long fetchCidByAutocomplete(String query) {
        try {
            String url = "https://pubchem.ncbi.nlm.nih.gov/rest/autocomplete/compound/" + encode(query) + "/JSON?limit=1";
            String resp = httpGet(url);
            if (resp == null) return null;
            JsonNode root = objectMapper.readTree(resp);
            JsonNode items = root.path("dictionary_terms").path("compound");
            if (items.isArray() && !items.isEmpty()) {
                String bestMatch = items.get(0).asText();
                if (bestMatch != null && !bestMatch.isBlank()) {
                    return fetchCidByName(bestMatch);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public List<Long> fetchCidsByFormula(String formula) {
        List<Long> result = new ArrayList<>();
        try {
            String fastUrl = PUBCHEM_BASE + "/fastformula/" + encode(formula) + "/cids/JSON?MaxRecords=25";
            String fastBody = httpGet(fastUrl);
            if (fastBody != null) addCids(objectMapper.readTree(fastBody), result);
            if (!result.isEmpty()) {
                return result;
            }
            String formulaUrl = PUBCHEM_BASE + "/formula/" + encode(formula) + "/cids/JSON?MaxRecords=25";
            String formulaBody = httpGet(formulaUrl);
            if (formulaBody != null) addCids(objectMapper.readTree(formulaBody), result);
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
            String body = httpGet(url);
            if (body == null) return results;
            JsonNode root = objectMapper.readTree(body);
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

    private CompoundInfoDto defaultIsomer(long cid) {
        return new CompoundInfoDto(cid, "Compound " + cid, "Compound " + cid, "Unknown", 0.0);
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
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private String httpGet(String urlStr) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return null;
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}
