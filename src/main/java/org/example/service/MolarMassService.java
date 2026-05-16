package org.example.service;

import org.example.dto.ElementContributionDto;
import org.example.dto.MolarMassResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MolarMassService {

    private static final Pattern TOKEN = Pattern.compile("([A-Z][a-z]?)(\\d*)");
    private static final Map<String, Double> ATOMIC_MASSES = Map.ofEntries(

            Map.entry("H", 1.008),
            Map.entry("He", 4.003),

            Map.entry("Li", 6.94),
            Map.entry("Be", 9.012),
            Map.entry("B", 10.81),
            Map.entry("C", 12.011),
            Map.entry("N", 14.007),
            Map.entry("O", 15.999),
            Map.entry("F", 18.998),
            Map.entry("Ne", 20.180),
            Map.entry("Na", 22.990),
            Map.entry("Mg", 24.305),
            Map.entry("Al", 26.982),
            Map.entry("Si", 28.085),
            Map.entry("P", 30.974),
            Map.entry("S", 32.06),
            Map.entry("Cl", 35.45),
            Map.entry("Ar", 39.948),
            Map.entry("K", 39.098),
            Map.entry("Ca", 40.078),
            Map.entry("Sc", 44.956),
            Map.entry("Ti", 47.867),
            Map.entry("V", 50.942),
            Map.entry("Cr", 51.996),
            Map.entry("Mn", 54.938),
            Map.entry("Fe", 55.845),
            Map.entry("Co", 58.933),
            Map.entry("Ni", 58.693),
            Map.entry("Cu", 63.546),
            Map.entry("Zn", 65.38),
            Map.entry("Ga", 69.723),
            Map.entry("Ge", 72.63),
            Map.entry("As", 74.922),
            Map.entry("Se", 78.96),
            Map.entry("Br", 79.904),
            Map.entry("Kr", 83.798),
            Map.entry("Rb", 85.468),
            Map.entry("Sr", 87.62),
            Map.entry("Ag", 107.87),
            Map.entry("Cd", 112.41),
            Map.entry("Sn", 118.71),
            Map.entry("Sb", 121.76),
            Map.entry("I", 126.90),
            Map.entry("Xe", 131.29),
            Map.entry("Cs", 132.91),
            Map.entry("Ba", 137.33),
            Map.entry("W", 183.84),
            Map.entry("Pt", 195.08),
            Map.entry("Au", 196.97),
            Map.entry("Hg", 200.59),
            Map.entry("Pb", 207.2),
            Map.entry("Bi", 208.98),
            Map.entry("La", 138.91),
            Map.entry("Ce", 140.12),
            Map.entry("U", 238.03)
    );

    public MolarMassResponse calculate(String rawFormula) {
        if (rawFormula == null || rawFormula.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Formula is required");
        }
        String formula = rawFormula.replaceAll("\\s+", "");
        Map<String, Integer> counts = parseFormula(formula);

        List<ElementContributionDto> elements = new ArrayList<>();
        double total = 0.0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            Double mass = ATOMIC_MASSES.get(entry.getKey());
            if (mass == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown element: " + entry.getKey());
            }
            double part = mass * entry.getValue();
            total += part;
            elements.add(new ElementContributionDto(
                    entry.getKey(),
                    entry.getValue(),
                    round(mass),
                    round(part)
            ));
        }

        return new MolarMassResponse(formula, round(total), "g/mol", elements);
    }

    private Map<String, Integer> parseFormula(String formula) {
        Matcher matcher = TOKEN.matcher(formula);
        Map<String, Integer> counts = new LinkedHashMap<>();
        int pos = 0;
        while (matcher.find()) {
            if (matcher.start() != pos) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid formula near position " + pos);
            }
            String symbol = matcher.group(1);
            String countStr = matcher.group(2);
            int count = countStr.isEmpty() ? 1 : Integer.parseInt(countStr);
            counts.merge(symbol, count, Integer::sum);
            pos = matcher.end();
        }
        if (pos != formula.length() || counts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid chemical formula");
        }
        return counts;
    }

    private double round(double value) {
        return Math.round(value * 1000.0) / 1000.0;
    }
}
