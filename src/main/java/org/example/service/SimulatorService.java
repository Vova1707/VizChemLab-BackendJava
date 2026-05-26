package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.dto.SimulateResponse;
import org.example.dto.SimulateVisualizeResponse;
import org.example.entity.ChemicalElement;
import org.example.repository.ChemicalElementRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SimulatorService {

    static {
        try {
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[]{
                new javax.net.ssl.X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {}
                }
            };
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            javax.net.ssl.HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            System.err.println("SSL bypass failed: " + e.getMessage());
        }
    }

    private final PubChemService pubChemService;
    private final ChemicalElementRepository elementRepository;

    private Map<String, ChemicalElement> elementCache;

    private Map<String, ChemicalElement> getElementCache() {
        if (elementCache == null) {
            elementCache = new HashMap<>();
            for (ChemicalElement e : elementRepository.findAll()) {
                elementCache.put(e.getSymbol(), e);
            }
        }
        return elementCache;
    }

    private String getElementColor(String symbol) {
        ChemicalElement e = getElementCache().get(symbol);
        if (e != null && e.getColor() != null) return e.getColor();
        Map<String, String> cpk = Map.of(
            "H","#FFFFFF","C","#909090","N","#3050F8","O","#FF0D0D",
            "S","#FFFF30","P","#FF8000","Cl","#1FF01F","Fe","#E06633","Na","#AB5CF2"
        );
        return cpk.getOrDefault(symbol, "#CCCCCC");
    }

    private double getElementRadius(String symbol) {
        ChemicalElement e = getElementCache().get(symbol);
        if (e != null && e.getRadiusAngstrom() != null && e.getRadiusAngstrom() > 0)
            return e.getRadiusAngstrom();
        // Fallback
        Map<String, Double> r = Map.of(
            "H",0.25,"C",0.50,"N",0.45,"O",0.40,
            "S",0.60,"P",0.55,"Cl",0.70,"Na",0.75,"Mg",0.65
        );
        return r.getOrDefault(symbol, 0.5);
    }
    
    @Value("${gigachat.auth-key}")
    private String gigachatAuthKey;
    
    @Value("${gigachat.scope}")
    private String gigachatScope;
    
    private String gigachatToken;
    private long tokenExpiry;
    
    private static final String GIGACHAT_AUTH_URL = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth";
    private static final String GIGACHAT_COMPLETIONS_URL = "https://gigachat.devices.sberbank.ru/api/v1/chat/completions";
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SimulateResponse simulate(String reactants) {
        String normalized = normalizeReactants(reactants);
        String equation = generateReactionWithGigaChat(normalized);
        
        if (equation == null || equation.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                "Не удалось сгенерировать реакцию для данных реагентов");
        }

        String[] parts = equation.split("→|->|=>");
        if (parts.length != 2) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Некорректное уравнение реакции");
        }

        List<String> left = Arrays.stream(parts[0].split("\\+"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
        
        List<String> right = Arrays.stream(parts[1].split("\\+"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());

        SimulateResponse.ReactionInfo info = new SimulateResponse.ReactionInfo(
            left, right, 0
        );

        return new SimulateResponse(reactants, equation, equation, info);
    }

    public SimulateVisualizeResponse simulateVisualize(String reactants) {
        SimulateResponse simple = simulate(reactants);
        
        // Generate 3D frames for animation
        List<SimulateVisualizeResponse.Frame> frames = generateFrames(
            simple.getInfo().getReactants(),
            simple.getInfo().getProducts()
        );

        List<SimulateVisualizeResponse.MoleculeModel> models = new ArrayList<>();

        for (String compound : simple.getInfo().getReactants()) {
            models.add(createMoleculeModel(stripCoefficient(compound), "reactant"));
        }
        for (String compound : simple.getInfo().getProducts()) {
            models.add(createMoleculeModel(stripCoefficient(compound), "product"));
        }

        SimulateVisualizeResponse response = new SimulateVisualizeResponse();
        response.setReactants(simple.getReactants());
        response.setEquation(simple.getEquation());
        response.setRawEquation(simple.getRawEquation());
        
        SimulateVisualizeResponse.ReactionInfo info = new SimulateVisualizeResponse.ReactionInfo();
        info.setReactants(simple.getInfo().getReactants());
        info.setProducts(simple.getInfo().getProducts());
        info.setElements(simple.getInfo().getElements());
        response.setInfo(info);
        
        response.setFrames(frames);
        response.setModels(models);
        response.setModelError(models.isEmpty() ? "Не удалось загрузить 3D модели" : null);

        return response;
    }

    private String normalizeReactants(String reactants) {
        String normalized = reactants.toUpperCase().trim();
        normalized = normalized.replaceAll("\\bCL\\b", "CL2");
        normalized = normalized.replaceAll("\\bBR\\b", "BR2");
        normalized = normalized.replaceAll("\\bI\\b", "I2");
        normalized = normalized.replaceAll("\\bF\\b", "F2");
        normalized = normalized.replaceAll("\\bH\\b(?!2)", "H2");
        normalized = normalized.replaceAll("\\bO\\b(?!2)", "O2");
        normalized = normalized.replaceAll("\\bN\\b(?!2)", "N2");
        
        return normalized;
    }

    private synchronized String getGigaChatToken() {
        long now = System.currentTimeMillis() / 1000;
        if (gigachatToken != null && now < tokenExpiry - 300) {
            return gigachatToken;
        }
        
        if (gigachatAuthKey == null || gigachatAuthKey.isBlank()) {
            System.err.println("GIGACHAT_AUTH_KEY not set");
            return null;
        }
        
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(GIGACHAT_AUTH_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Basic " + gigachatAuthKey);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("RqUID", "99c62694-10a5-4f6f-8d2d-5759431d8f22");
            conn.setDoOutput(true);
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(("scope=" + gigachatScope).getBytes(StandardCharsets.UTF_8));
            }
            
            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            
            if (code == 200) {
                JsonNode root = objectMapper.readTree(body);
                gigachatToken = root.path("access_token").asText();
                int expiresIn = root.path("expires_in").asInt(1800);
                tokenExpiry = now + expiresIn - 300;
                return gigachatToken;
            }
        } catch (Exception e) {
            System.err.println("GigaChat auth error: " + e.getMessage());
        }
        return null;
    }
    
    private String generateReactionWithGigaChat(String reactants) {
        String token = getGigaChatToken();
        if (token == null) {
            System.err.println("[SIM] No GigaChat token available");
            return null;
        }
        
        System.out.println("[SIM] Calling GigaChat API for: " + reactants);
        
        String prompt = "You are an expert organic and inorganic chemist.\n" +
            "Request: " + reactants + "\n" +
            "Instructions:\n" +
            "1. Analyze the reactants and determine the most likely chemical reaction.\n" +
            "2. Use standard IUPAC products.\n" +
            "3. Ensure all diatomic molecules are in their natural state (O2, H2, Cl2, etc.).\n" +
            "4. If no reaction is possible, output ONLY 'NO_REACTION'.\n" +
            "5. Output ONLY the balanced chemical equation. No explanations, no markdown.\n" +
            "Examples:\n" +
            "- C2H5OH -> C2H4 + H2O\n" +
            "- CH4 + Cl2 -> CH3Cl + HCl\n" +
            "- горение метана -> CH4 + 2O2 → CO2 + 2H2O\n" +
            "Balanced Equation:";
        
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(GIGACHAT_COMPLETIONS_URL).openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + token);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setConnectTimeout(10000); // 10 seconds
            conn.setReadTimeout(20000);    // 20 seconds
            conn.setDoOutput(true);
            
            String payload = objectMapper.writeValueAsString(Map.of(
                "model", "GigaChat",
                "messages", List.of(
                    Map.of("role", "system", "content", "You are an expert organic and inorganic chemist."),
                    Map.of("role", "user", "content", prompt)
                ),
                "temperature", 0.1,
                "top_p", 0.1,
                "max_tokens", 100,
                "stream", false
            ));
            
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            
            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            
            if (code == 200) {
                JsonNode root = objectMapper.readTree(body);
                String content = root.path("choices").get(0).path("message").path("content").asText();
                System.out.println("[SIM] GigaChat response: " + content);

                if (content.contains("NO_REACTION")) {
                    System.out.println("[SIM] GigaChat returned NO_REACTION");
                    return null;
                }
                
                String equation = cleanEquation(content);
                System.out.println("[SIM] Cleaned equation: " + equation);
                return equation;
            } else {
                System.err.println("[SIM] GigaChat API error code: " + code + ", body: " + body);
            }
            
            if (code == 401) {
                gigachatToken = null;
            }
        } catch (Exception e) {
            System.err.println("[SIM] GigaChat API error: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    private String cleanEquation(String text) {
        if (text == null || text.isBlank()) return null;

        String clean = text.replaceAll("\\$\\$|\\$|\\[|\\]", "");
        clean = clean.replaceAll("→|->|=>", "→");
        clean = clean.replaceAll("\n", " ").trim();
        

        int arrowIdx = clean.indexOf('→');
        if (arrowIdx < 0) {
            arrowIdx = clean.indexOf("->");
        }
        if (arrowIdx < 0) {
            arrowIdx = clean.indexOf("=>");
        }
        if (arrowIdx < 0) return clean;
        
        return clean;
    }

    private String createLookupKey(String reactants) {

        String[] compounds = reactants.split("\\+");
        List<String> normalized = new ArrayList<>();
        
        for (String compound : compounds) {
            String clean = compound.trim().toUpperCase()
                .replaceAll("\\s+", "")
                .replaceAll("\\d+", "");
            normalized.add(clean);
        }
        
        Collections.sort(normalized);
        return String.join("+", normalized);
    }

    private String stripCoefficient(String compound) {
        return compound.replaceAll("^\\d+", "").trim();
    }
    private int extractCoefficient(String compound) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d+)").matcher(compound.trim());
        return m.find() ? Integer.parseInt(m.group(1)) : 1;
    }
    private List<MolData> expandCompound(String compound, String side) {
        int coeff = Math.min(extractCoefficient(compound), 6);
        String formula = stripCoefficient(compound);
        MolData template = fetchMolData(formula, side);
        if (coeff == 1) return List.of(template);

        double maxExtent = 0;
        for (AtomData a : template.atoms) {
            maxExtent = Math.max(maxExtent, Math.abs(a.x));
            maxExtent = Math.max(maxExtent, Math.abs(a.y));
            maxExtent = Math.max(maxExtent, Math.abs(a.z));
        }
        double spread = Math.max(maxExtent * 2.4, 3.5);

        List<MolData> result = new ArrayList<>();

        for (int i = 0; i < coeff; i++) {
            double offsetX = (i - (coeff - 1) / 2.0) * spread;
            List<AtomData> shiftedAtoms = new ArrayList<>();
            for (AtomData a : template.atoms)
                shiftedAtoms.add(new AtomData(a.element, a.x + offsetX, a.y, a.z));
            result.add(new MolData(formula, side, shiftedAtoms, template.bonds));
        }
        return result;
    }

    private Set<String> parseElements(String formula) {
        Set<String> elements = new HashSet<>();
        Pattern pattern = Pattern.compile("([A-Z][a-z]?)");
        Matcher matcher = pattern.matcher(formula);
        while (matcher.find()) {
            elements.add(matcher.group(1));
        }
        return elements;
    }

    private static class AtomData {
        String element;
        double x, y, z;
        AtomData(String e, double x, double y, double z) { this.element = e; this.x = x; this.y = y; this.z = z; }
    }

    private static class BondData {
        int start, end, order;
        BondData(int s, int e, int o) { this.start = s; this.end = e; this.order = o; }
    }

    private static class MolData {
        String compound, side;
        List<AtomData> atoms;
        List<BondData> bonds;
        MolData(String compound, String side, List<AtomData> atoms, List<BondData> bonds) {
            this.compound = compound; this.side = side; this.atoms = atoms; this.bonds = bonds;
        }
    }

    private static class ParsedMol {
        List<AtomData> atoms;
        List<BondData> bonds;
        ParsedMol(List<AtomData> a, List<BondData> b) { this.atoms = a; this.bonds = b; }
    }

    private ParsedMol parseSdf(String sdf) {

        String[] lines = sdf.replace("\r\n", "\n").replace("\r", "\n").strip().split("\n");
        if (lines.length < 4) return null;
        try {

            int countsIdx = -1;
            for (int i = 3; i < Math.min(6, lines.length); i++) {
                String l = lines[i];
                if (l.length() >= 6 &&
                    l.substring(0, 3).trim().matches("\\d+") &&
                    l.substring(3, 6).trim().matches("\\d+")) {
                    countsIdx = i;
                    break;
                }
            }
            if (countsIdx < 0) return null;
            String countsLine = lines[countsIdx];
            int atomCount = Integer.parseInt(countsLine.substring(0, 3).trim());
            int bondCount = Integer.parseInt(countsLine.substring(3, 6).trim());
            if (atomCount <= 0) return null;

            List<AtomData> atoms = new ArrayList<>();
            for (int i = countsIdx + 1; i < countsIdx + 1 + atomCount && i < lines.length; i++) {
                String[] p = lines[i].trim().split("\\s+");
                if (p.length < 4) continue;
                try {
                    double x = Double.parseDouble(p[0]);
                    double y = Double.parseDouble(p[1]);
                    double z = Double.parseDouble(p[2]);
                    String elem = p[3].replaceAll("[^A-Za-z]", "");
                    if (!elem.isEmpty()) atoms.add(new AtomData(elem, x, y, z));
                } catch (NumberFormatException ignored) {}
            }
            List<BondData> bonds = new ArrayList<>();
            int bondStart = countsIdx + 1 + atoms.size();
            for (int i = bondStart; i < bondStart + bondCount && i < lines.length; i++) {
                String line = lines[i];
                if (line.length() < 6) continue;
                try {
                    int a = Integer.parseInt(line.substring(0, 3).trim()) - 1;
                    int b = Integer.parseInt(line.substring(3, 6).trim()) - 1;
                    int order = line.length() >= 9 ? Integer.parseInt(line.substring(6, 9).trim()) : 1;
                    if (a >= 0 && a < atoms.size() && b >= 0 && b < atoms.size())
                        bonds.add(new BondData(a, b, order));
                } catch (NumberFormatException ignored) {}
            }
            System.out.println("[SDF] atoms=" + atoms.size() + " bonds=" + bonds.size() +
                " | " + atoms.stream().map(a -> a.element).collect(Collectors.joining(",")) +
                " | bonds: " + bonds.stream().map(bd -> bd.start + "-" + bd.end).collect(Collectors.joining(",")));
            return new ParsedMol(atoms, bonds);
        } catch (Exception e) {
            System.err.println("[SDF] parse error: " + e.getMessage());
            return null;
        }
    }

    private List<AtomData> centerMolecule(List<AtomData> atoms) {
        if (atoms.isEmpty()) return atoms;
        double cx = atoms.stream().mapToDouble(a -> a.x).average().orElse(0);
        double cy = atoms.stream().mapToDouble(a -> a.y).average().orElse(0);
        double cz = atoms.stream().mapToDouble(a -> a.z).average().orElse(0);
        List<AtomData> out = new ArrayList<>();
        for (AtomData a : atoms) out.add(new AtomData(a.element, a.x - cx, a.y - cy, a.z - cz));
        return out;
    }

    private MolData fetchMolData(String compound, String side) {
        String sdf = null;

        if (pubChemService.isFormula(compound)) {
            List<Long> cids = pubChemService.fetchCidsByFormula(compound);
            if (!cids.isEmpty()) {
                sdf = pubChemService.fetchSdfByCid(cids.get(0), "3d");
                if (sdf == null) sdf = pubChemService.fetchSdfByCid(cids.get(0), "2d");
            }
        }
        if (sdf == null) {
            sdf = pubChemService.fetchSdfByName(compound, "3d");
            if (sdf == null) sdf = pubChemService.fetchSdfByName(compound, "2d");
        }
        if (sdf == null) {
            return generateFallbackMolData(compound, side);
        }
        ParsedMol parsed = parseSdf(sdf);
        if (parsed == null) return generateFallbackMolData(compound, side);
        return new MolData(compound, side, centerMolecule(parsed.atoms), parsed.bonds);
    }

    /** Fallback */
    private MolData generateFallbackMolData(String compound, String side) {
        Map<String, Integer> elems = parseFormula(compound);
        List<AtomData> atoms = new ArrayList<>();

        String central = null;
        for (String el : new String[]{"C","N","O","S","P","Si","B","Cl","Br","I","F"}) {
            if (elems.containsKey(el)) { central = el; break; }
        }
        if (central == null) central = elems.keySet().iterator().next();
        atoms.add(new AtomData(central, 0, 0, 0));
        int remaining = elems.get(central) - 1;
        double[][] tetra = {
            { 1, 1, 1}, { 1,-1,-1}, {-1, 1,-1}, {-1,-1, 1},
            { 1, 0,-1}, {-1, 0,-1}, { 0, 1, 1}, { 0,-1, 1}
        };
        int ligandIdx = 0;
        // Add remaining copies of central element
        for (int k = 0; k < remaining; k++) {
            double[] p = tetra[ligandIdx % tetra.length]; ligandIdx++;
            atoms.add(new AtomData(central, p[0]*1.5, p[1]*1.5, p[2]*1.5));
        }
        for (Map.Entry<String, Integer> e : elems.entrySet()) {
            if (e.getKey().equals(central)) continue;
            for (int k = 0; k < e.getValue(); k++) {
                double[] p = tetra[ligandIdx % tetra.length]; ligandIdx++;
                atoms.add(new AtomData(e.getKey(), p[0]*1.5, p[1]*1.5, p[2]*1.5));
            }
        }
        List<BondData> bonds = new ArrayList<>();
        for (int i = 1; i < atoms.size(); i++) bonds.add(new BondData(0, i, 1));
        return new MolData(compound, side, atoms, bonds);
    }

    private static class MatchResult {
        List<int[]> matched = new ArrayList<>();
        List<Map<String, Object>> rRem = new ArrayList<>();
        List<Map<String, Object>> pRem = new ArrayList<>();
    }

    private MatchResult matchAtoms(List<Map<String, Object>> rAtoms, List<Map<String, Object>> pAtoms) {
        MatchResult res = new MatchResult();
        List<Map<String, Object>> rRem = new ArrayList<>(rAtoms);
        List<Map<String, Object>> pRem = new ArrayList<>(pAtoms);
        for (Map<String, Object> ra : new ArrayList<>(rRem)) {
            for (Map<String, Object> pa : new ArrayList<>(pRem)) {
                if (ra.get("element").equals(pa.get("element"))) {
                    res.matched.add(new int[]{(int)ra.get("_orig_idx"), (int)pa.get("_orig_idx")});
                    rRem.remove(ra); pRem.remove(pa); break;
                }
            }
        }
        while (!rRem.isEmpty() && !pRem.isEmpty())
            res.matched.add(new int[]{(int)rRem.remove(0).get("_orig_idx"), (int)pRem.remove(0).get("_orig_idx")});
        res.rRem.addAll(rRem);
        res.pRem.addAll(pRem);
        return res;
    }

    private static class FlatResult {
        List<Map<String, Object>> atoms = new ArrayList<>();
        List<Map<String, Object>> bonds = new ArrayList<>();
    }

    private FlatResult flattenModels(List<MolData> models, double startX) {
        FlatResult fr = new FlatResult();
        int molIdx = 0;
        for (MolData m : models) {
            double shiftX = startX + molIdx * 8.0;
            int atomOffset = fr.atoms.size();
            int ai = 0;
            for (AtomData a : m.atoms) {
                int origIdx = atomOffset + ai;
                Map<String, Object> atom = new HashMap<>();
                atom.put("element", a.element);
                atom.put("x", a.x + shiftX);
                atom.put("y", a.y);
                atom.put("z", a.z);
                atom.put("opacity", 1.0);
                atom.put("mol_idx", molIdx);
                atom.put("compound", m.compound);
                atom.put("_orig_idx", origIdx);
                atom.put("color", getElementColor(a.element));
                atom.put("radius", getElementRadius(a.element));
                fr.atoms.add(atom);
                ai++;
            }
            for (BondData b : m.bonds) {
                Map<String, Object> bond = new HashMap<>();
                bond.put("start", atomOffset + b.start);
                bond.put("end", atomOffset + b.end);
                bond.put("order", b.order);
                fr.bonds.add(bond);
            }
            molIdx++;
        }
        return fr;
    }

    private List<SimulateVisualizeResponse.Frame> generateFrames(
            List<String> reactants, List<String> products) {
        int steps = 40;

        List<MolData> reactantModels = new ArrayList<>();
        for (String c : reactants) reactantModels.addAll(expandCompound(c, "reactant"));
        List<MolData> productModels = new ArrayList<>();
        for (String c : products) productModels.addAll(expandCompound(c, "product"));

        FlatResult rFlat = flattenModels(reactantModels, 0.0);
        FlatResult pFlat = flattenModels(productModels, 0.0);

        MatchResult match = matchAtoms(rFlat.atoms, pFlat.atoms);

        Map<Integer, Map<String, Object>> rByOrig = new HashMap<>();
        for (Map<String, Object> a : rFlat.atoms) rByOrig.put((int)a.get("_orig_idx"), a);
        Map<Integer, Map<String, Object>> pByOrig = new HashMap<>();
        for (Map<String, Object> a : pFlat.atoms) pByOrig.put((int)a.get("_orig_idx"), a);

        List<SimulateVisualizeResponse.Frame> frames = new ArrayList<>();

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double tSmooth = t * t * (3 - 2 * t);
            String title = t <= 0.1 ? "Реагенты" : (t >= 0.9 ? "Продукты" : "Химическая реакция");

            List<Map<String, Object>> frameAtoms = new ArrayList<>();

            Map<Integer, Integer> rToFrame = new HashMap<>();
            Map<Integer, Integer> pToFrame = new HashMap<>();

            // Matched atoms: interpolate
            for (int[] pair : match.matched) {
                int rOrig = pair[0], pOrig = pair[1];
                Map<String, Object> ra = rByOrig.get(rOrig);
                Map<String, Object> pa = pByOrig.get(pOrig);
                if (ra == null || pa == null) continue;
                int idx = frameAtoms.size();
                String elem = t < 0.5 ? (String)ra.get("element") : (String)pa.get("element");
                Map<String, Object> a = new HashMap<>();
                a.put("element", elem);
                a.put("x", (double)ra.get("x") * (1 - tSmooth) + (double)pa.get("x") * tSmooth);
                a.put("y", (double)ra.get("y") * (1 - tSmooth) + (double)pa.get("y") * tSmooth);
                a.put("z", (double)ra.get("z") * (1 - tSmooth) + (double)pa.get("z") * tSmooth);
                a.put("opacity", 1.0);
                a.put("mol_idx", t < 0.5 ? ra.get("mol_idx") : pa.get("mol_idx"));
                a.put("color", getElementColor(elem));
                a.put("radius", getElementRadius(elem));
                frameAtoms.add(a);
                rToFrame.put(rOrig, idx);
                pToFrame.put(pOrig, idx);
            }

            // Unmatched reactant atoms: fade out + sink
            for (Map<String, Object> ra : match.rRem) {
                int idx = frameAtoms.size();
                Map<String, Object> a = new HashMap<>();
                String elem = (String)ra.get("element");
                a.put("element", elem);
                a.put("x", ra.get("x"));
                a.put("y", ra.get("y"));
                a.put("z", (double)ra.get("z") - tSmooth * 3.0);
                a.put("opacity", 1.0 - tSmooth);
                a.put("mol_idx", ra.get("mol_idx"));
                a.put("color", getElementColor(elem));
                a.put("radius", getElementRadius(elem));
                frameAtoms.add(a);
                rToFrame.put((int)ra.get("_orig_idx"), idx);
            }

            // Unmatched product atoms: fade in + rise
            for (Map<String, Object> pa : match.pRem) {
                int idx = frameAtoms.size();
                Map<String, Object> a = new HashMap<>();
                String elem = (String)pa.get("element");
                a.put("element", elem);
                a.put("x", pa.get("x"));
                a.put("y", pa.get("y"));
                a.put("z", (double)pa.get("z") + (1.0 - tSmooth) * 3.0);
                a.put("opacity", tSmooth);
                a.put("mol_idx", pa.get("mol_idx"));
                a.put("color", getElementColor(elem));
                a.put("radius", getElementRadius(elem));
                frameAtoms.add(a);
                pToFrame.put((int)pa.get("_orig_idx"), idx);
            }

            // Bonds: r bonds fade out, p bonds fade in — key is _orig_idx
            List<Map<String, Object>> frameBonds = new ArrayList<>();
            for (Map<String, Object> rb : rFlat.bonds) {
                Integer si = rToFrame.get((int)rb.get("start"));
                Integer ei = rToFrame.get((int)rb.get("end"));
                if (si != null && ei != null) {
                    Map<String, Object> b = new HashMap<>();
                    b.put("start", si); b.put("end", ei); b.put("opacity", 1.0 - tSmooth);
                    frameBonds.add(b);
                }
            }
            for (Map<String, Object> pb : pFlat.bonds) {
                Integer si = pToFrame.get((int)pb.get("start"));
                Integer ei = pToFrame.get((int)pb.get("end"));
                if (si != null && ei != null) {
                    Map<String, Object> b = new HashMap<>();
                    b.put("start", si); b.put("end", ei); b.put("opacity", tSmooth);
                    frameBonds.add(b);
                }
            }

            SimulateVisualizeResponse.Frame frame = new SimulateVisualizeResponse.Frame();
            frame.setTitle(title);
            frame.setProgress(t);
            frame.setAtoms(frameAtoms);
            frame.setBonds(frameBonds);
            frames.add(frame);
        }

        return frames;
    }

    private SimulateVisualizeResponse.MoleculeModel createMoleculeModel(String compound, String side) {
        MolData md = fetchMolData(compound, side);
        SimulateVisualizeResponse.MoleculeModel model = new SimulateVisualizeResponse.MoleculeModel();
        model.setCompound(compound);
        model.setSide(side);
        List<SimulateVisualizeResponse.Atom> atoms = new ArrayList<>();
        for (AtomData a : md.atoms) {
            SimulateVisualizeResponse.Atom atom = new SimulateVisualizeResponse.Atom();
            atom.setElement(a.element); atom.setX(a.x); atom.setY(a.y); atom.setZ(a.z);
            atoms.add(atom);
        }
        model.setAtoms(atoms);
        List<SimulateVisualizeResponse.Bond> bonds = new ArrayList<>();
        for (BondData b : md.bonds) {
            SimulateVisualizeResponse.Bond bond = new SimulateVisualizeResponse.Bond();
            bond.setStart(b.start); bond.setEnd(b.end); bond.setOrder(b.order);
            bonds.add(bond);
        }
        model.setBonds(bonds);
        return model;
    }

    private Map<String, Integer> parseFormula(String formula) {
        Map<String, Integer> counts = new HashMap<>();
        Pattern pattern = Pattern.compile("([A-Z][a-z]?)(\\d*)");
        Matcher matcher = pattern.matcher(formula);
        
        while (matcher.find()) {
            String element = matcher.group(1);
            String countStr = matcher.group(2);
            int count = countStr.isEmpty() ? 1 : Integer.parseInt(countStr);
            counts.merge(element, count, Integer::sum);
        }
        
        return counts;
    }
}
