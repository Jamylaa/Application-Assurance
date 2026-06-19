package tn.vermeg.gestionproduit.services.chatbot.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service d'extraction numérique amélioré avec patterns regex avancés.
 * Gère l'extraction correcte des champs numériques: prix, plafonds, franchises, durées.
 * 
 * @author PFE Ingénieur - GestionProduit
 * @version 2.0 - Enhanced AI Analysis Engine
 */
@Service
public class EnhancedNumericExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(EnhancedNumericExtractionService.class);

    /**
     * Résultat d'extraction numérique avec métadonnées
     */
    public static class NumericExtractionResult {
        private final Double value;
        private final String rawMatch;
        private final String extractionMethod;
        private final double confidence;
        private final String unit;
        private final boolean isValid;

        public NumericExtractionResult(Double value, String rawMatch, String extractionMethod,
                                      double confidence, String unit, boolean isValid) {
            this.value = value;
            this.rawMatch = rawMatch;
            this.extractionMethod = extractionMethod;
            this.confidence = confidence;
            this.unit = unit;
            this.isValid = isValid;
        }

        public Double getValue() { return value; }
        public String getRawMatch() { return rawMatch; }
        public String getExtractionMethod() { return extractionMethod; }
        public double getConfidence() { return confidence; }
        public String getUnit() { return unit; }
        public boolean isValid() { return isValid; }
    }

    /**
     * Extrait le prix mensuel avec patterns améliorés
     * Priorise les patterns qui mentionnent explicitement "prix mensuel", "coût mensuel", "tarif mensuel"
     */
    public NumericExtractionResult extractPrixMensuel(String text) {
        if (text == null || text.isBlank()) {
            return new NumericExtractionResult(0.0, "", "none", 0.0, "", false);
        }

        String lowerText = text.toLowerCase();

        // Pattern 1: "prix mensuel de X" / "coût mensuel X" / "tarif mensuel X" - HIGHEST PRIORITY
        Pattern pattern1 = Pattern.compile(
            "(?:prix|co[uû]t|tarif)\\s+(?:mensuel|mensuelle)\\s*(?::|de|à|a|égale?|egale?|est)?\\s*(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?)?",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 2: "X par mois" / "X mensuel" - HIGH PRIORITY
        Pattern pattern2 = Pattern.compile(
            "(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?)?\\s*(?:par|pour)\\s*(?:chaque\\s+)?mois",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 3: "mensualité de X" / "mensualité X" - HIGH PRIORITY
        Pattern pattern3 = Pattern.compile(
            "mensualit[ée]\\s*(?:de|:)?\\s*(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?)?",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 4: "pack à X par mois" - VERY SPECIFIC
        Pattern pattern4 = Pattern.compile(
            "(?:pack|offre|formule)\\s+(?:à|co[uû]te|tarif)\\s*(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?)?\\s*(?:par|pour)\\s*mois",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 5: Simple price mention without context (last resort) - LOW PRIORITY
        Pattern pattern5 = Pattern.compile(
            "(?:prix|co[uû]t|tarif)\\s*[:]?\\s*(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?)?",
            Pattern.CASE_INSENSITIVE
        );

        // Pattern 6: Standalone number at beginning of sentence (very low confidence)
        Pattern pattern6 = Pattern.compile(
            "^(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?)?\\s*(?:par|pour)\\s*mois",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
        );

        List<Pattern> patterns = Arrays.asList(pattern1, pattern2, pattern3, pattern4, pattern5, pattern6);
        List<String> methods = Arrays.asList("explicit_monthly", "per_month", "mensualite", "pack_specific", "simple_price", "standalone_number");

        for (int i = 0; i < patterns.size(); i++) {
            Matcher matcher = patterns.get(i).matcher(text);
            if (matcher.find()) {
                try {
                    String valueStr = matcher.group(1).replace(",", ".");
                    double value = Double.parseDouble(valueStr);
                    
                    // Filtrer les valeurs qui sont probablement des plafonds ou franchises
                    // Les prix mensuels réalistes sont généralement entre 5 et 500
                    if (value < 0.5 || value > 5000) {
                        logger.debug("Valeur {} ignorée comme prix mensuel (hors plage réaliste)", value);
                        continue;
                    }
                    
                    // Déterminer l'unité
                    String unit = extractCurrencyUnit(text);
                    
                    // Calculer la confiance (patterns plus spécifiques = confiance plus élevée)
                    double confidence = 1.0 - (i * 0.15);
                    
                    logger.debug("Prix mensuel extrait: {} (méthode: {}, confiance: {})", value, methods.get(i), confidence);
                    return new NumericExtractionResult(value, matcher.group(0), methods.get(i), confidence, unit, true);
                } catch (NumberFormatException e) {
                    logger.warn("Erreur parsing prix: {}", matcher.group(1));
                }
            }
        }

        logger.debug("Prix mensuel non détecté");
        return new NumericExtractionResult(0.0, "", "not_found", 0.0, "", false);
    }

    /**
     * Extrait les plafonds (annuel, mensuel, par acte)
     */
    public Map<String, NumericExtractionResult> extractPlafonds(String text) {
        Map<String, NumericExtractionResult> plafonds = new HashMap<>();

        // Plafond annuel
        plafonds.put("plafondAnnuel", extractSinglePlafond(text, "annuel", 
            Arrays.asList("plafond\\s+annuel\\s+de\\s+(\\d+[.,]?\\d*)",
                          "(?:plafond|limite)\\s+(?:annuel|annuelle)\\s*[:]?\\s*(\\d+[\\s\\d.,]*)?",
                          "(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?)?\\s*(?:par )?an")));

        // Plafond mensuel
        plafonds.put("plafondMensuel", extractSinglePlafond(text, "mensuel",
            Arrays.asList("plafond\\s+mensuel\\s+de\\s+(\\d+[.,]?\\d*)",
                          "(?:plafond|limite)\\s+(?:mensuel|mensuelle)\\s*[:]?\\s*(\\d+[\\s\\d.,]*)?",
                          "(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?)?\\s*(?:par )?mois")));

        // Plafond par acte
        plafonds.put("plafondParActe", extractSinglePlafond(text, "parActe",
            Arrays.asList("plafond\\s+par\\s+acte\\s+de\\s+(\\d+[.,]?\\d*)",
                          "(?:plafond|limite)\\s+(?:par acte|par actes?)\\s*[:]?\\s*(\\d+[\\s\\d.,]*)?",
                          "(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?)?\\s*(?:par )?acte")));

        return plafonds;
    }

    /**
     * Extrait un plafond spécifique
     */
    private NumericExtractionResult extractSinglePlafond(String text, String type, List<String> patterns) {
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    String valueStr = matcher.group(1).replaceAll("\\s+", "").replace(",", ".");
                    double value = Double.parseDouble(valueStr);
                    String unit = extractCurrencyUnit(text);
                    return new NumericExtractionResult(value, matcher.group(0), "plafond_" + type, 0.9, unit, true);
                } catch (NumberFormatException e) {
                    logger.warn("Erreur parsing plafond {}: {}", type, matcher.group(1));
                }
            }
        }
        return new NumericExtractionResult(0.0, "", "not_found", 0.0, "", false);
    }

    /**
     * Extrait la franchise
     */
    public NumericExtractionResult extractFranchise(String text) {
        if (text == null || text.isBlank()) {
            return new NumericExtractionResult(0.0, "", "none", 0.0, "", false);
        }

        // Pattern pour "sans franchise" / "aucune franchise"
        Pattern noFranchisePattern = Pattern.compile(
            "(?:sans|aucune)\\s+franchise",
            Pattern.CASE_INSENSITIVE
        );

        Matcher noFranchiseMatcher = noFranchisePattern.matcher(text);
        if (noFranchiseMatcher.find()) {
            return new NumericExtractionResult(0.0, noFranchiseMatcher.group(0), "no_franchise", 1.0, "", true);
        }

        // Pattern pour "franchise de X"
        Pattern pattern = Pattern.compile(
            "franchise\\s+(?:de|:)?\\s*(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?)?",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                String valueStr = matcher.group(1).replace(",", ".");
                double value = Double.parseDouble(valueStr);
                String unit = extractCurrencyUnit(text);
                return new NumericExtractionResult(value, matcher.group(0), "explicit_franchise", 0.95, unit, true);
            } catch (NumberFormatException e) {
                logger.warn("Erreur parsing franchise: {}", matcher.group(1));
            }
        }

        return new NumericExtractionResult(0.0, "", "not_found", 0.0, "", false);
    }

    /**
     * Extrait les durées (min et max)
     */
    public Map<String, NumericExtractionResult> extractDurees(String text) {
        Map<String, NumericExtractionResult> durees = new HashMap<>();

        // Pattern pour "durée comprise entre X et Y mois"
        Pattern betweenPattern = Pattern.compile(
            "dur[ée]+\\s+(?:comprise\\s+)?entre\\s+(\\d+)\\s+et\\s+(\\d+)\\s+(mois|ans|years|months)",
            Pattern.CASE_INSENSITIVE
        );

        Matcher betweenMatcher = betweenPattern.matcher(text);
        if (betweenMatcher.find()) {
            try {
                int min = Integer.parseInt(betweenMatcher.group(1));
                int max = Integer.parseInt(betweenMatcher.group(2));
                String unit = betweenMatcher.group(3).toLowerCase();

                // Convertir en mois si nécessaire
                if (unit.contains("an")) {
                    min *= 12;
                    max *= 12;
                }

                durees.put("dureeMinContrat", new NumericExtractionResult((double) min, betweenMatcher.group(1), "duration_between", 0.95, "mois", true));
                durees.put("dureeMaxContrat", new NumericExtractionResult((double) max, betweenMatcher.group(2), "duration_between", 0.95, "mois", true));
            } catch (NumberFormatException e) {
                logger.warn("Erreur parsing durée entre: {}", betweenMatcher.group(0));
            }
        }

        // Durée minimum
        durees.put("dureeMinContrat", extractSingleDuree(text, "min",
            Arrays.asList("dur[ée]+\\s+(?:minimum|min)\\s+(?:de )?contrat\\s*[:]?\\s*(\\d+)",
                          "contrat\\s+d(?:e\\s+)?(\\d+)\\s+(?:mois|ans)",
                          "dur[ée]+\\s+minimale\\s+(?:de\\s+)?(\\d+)")));

        // Durée maximum
        durees.put("dureeMaxContrat", extractSingleDuree(text, "max",
            Arrays.asList("dur[ée]+\\s+(?:maximum|max)\\s+(?:de )?contrat\\s*[:]?\\s*(\\d+)",
                          "jusqu'à\\s+(\\d+)\\s+(?:mois|ans)\\s+de contrat",
                          "dur[ée]+\\s+maximale\\s+(?:de\\s+)?(\\d+)")));

        return durees;
    }

    /**
     * Extrait une durée spécifique
     */
    private NumericExtractionResult extractSingleDuree(String text, String type, List<String> patterns) {
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    int value = Integer.parseInt(matcher.group(1));
                    return new NumericExtractionResult((double) value, matcher.group(0), "duree_" + type, 0.9, "", true);
                } catch (NumberFormatException e) {
                    logger.warn("Erreur parsing durée {}: {}", type, matcher.group(1));
                }
            }
        }
        // Si déjà extrait par le pattern "entre", ne pas écraser
        return new NumericExtractionResult(0.0, "", "not_found", 0.0, "", false);
    }

    /**
     * Extrait l'âge minimum
     */
    public NumericExtractionResult extractAgeMinimum(String text) {
        List<String> patterns = Arrays.asList(
            "(?:âge|age)\\s+(?:minimum|min)\\s*(?:de|:)?\\s*(\\d+)\\s*ans?",
            "à partir de\\s+(\\d+)\\s+ans",
            "(?:âge|age)\\s*[:]?\\s*(\\d+)\\s*(?:ans|et plus)\\s*(?:minimum|min)?",
            "(?:pour|destiné aux?|ciblant)\\s+(?:les?\\s*)?(?:seniors|personnes âgées)\\s*(?:de\\s+)?(\\d+)\\s*ans?"
        );

        return extractSingleInteger(text, patterns, "age_minimum");
    }

    /**
     * Extrait l'âge maximum
     */
    public NumericExtractionResult extractAgeMaximum(String text) {
        List<String> patterns = Arrays.asList(
            "(?:âge|age)\\s+(?:maximum|max)\\s*(?:de|:)?\\s*(\\d+)\\s*ans?",
            "jusqu'à\\s+(\\d+)\\s+ans",
            "(?:âge|age)\\s*[:]?\\s*(\\d+)\\s*(?:ans|et moins)\\s*(?:maximum|max)?"
        );

        NumericExtractionResult result = extractSingleInteger(text, patterns, "age_maximum");
        // Valeur par défaut réaliste si non spécifié
        if (!result.isValid()) {
            return new NumericExtractionResult(120.0, "default", "default_age_max", 0.5, "", true);
        }
        return result;
    }

    /**
     * Extrait le taux de remboursement
     */
    public NumericExtractionResult extractTauxRemboursement(String text) {
        List<String> patterns = Arrays.asList(
            "(?:taux|taux de|pourcentage)\\s+(?:de )?remboursement\\s*[:]?\\s*(\\d+(?:[.,]\\d+)?)%?",
            "remboursement\\s+(?:de|à)\\s*(\\d+(?:[.,]\\d+)?)%?",
            "(\\d+(?:[.,]\\d+)?)%?\\s*(?:de )?remboursement"
        );

        NumericExtractionResult result = extractSingleDouble(text, patterns, "taux_remboursement");
        
        // Si > 1, convertir en pourcentage (0.8)
        if (result.isValid() && result.getValue() > 1.0) {
            double normalizedValue = result.getValue() / 100.0;
            return new NumericExtractionResult(normalizedValue, result.getRawMatch(), 
                result.getExtractionMethod() + "_normalized", result.getConfidence(), "%", true);
        }
        
        return result;
    }

    /**
     * Helper pour extraire un entier
     */
    private NumericExtractionResult extractSingleInteger(String text, List<String> patterns, String methodName) {
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                try {
                    int value = Integer.parseInt(matcher.group(1));
                    return new NumericExtractionResult((double) value, matcher.group(0), methodName, 0.9, "", true);
                } catch (NumberFormatException e) {
                    logger.warn("Erreur parsing entier: {}", matcher.group(1));
                }
            }
        }
        return new NumericExtractionResult(0.0, "", "not_found", 0.0, "", false);
    }

    /**
     * Helper pour extraire un double
     */
    private NumericExtractionResult extractSingleDouble(String text, List<String> patterns, String methodName) {
        for (String patternStr : patterns) {
            Pattern pattern = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    String valueStr = matcher.group(i);
                    if (valueStr != null && !valueStr.trim().isEmpty()) {
                        try {
                            String normalizedValue = valueStr.replaceAll("\\s+", "").replace(",", ".");
                            double value = Double.parseDouble(normalizedValue);
                            return new NumericExtractionResult(value, matcher.group(0), methodName, 0.9, "", true);
                        } catch (NumberFormatException e) {
                            logger.warn("Erreur parsing double: {}", valueStr);
                        }
                    }
                }
            }
        }
        return new NumericExtractionResult(0.0, "", "not_found", 0.0, "", false);
    }

    /**
     * Extrait l'unité de devise du texte
     */
    private String extractCurrencyUnit(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("tnd") || lower.contains("tunisien") || lower.contains("dinar")) {
            return "TND";
        }
        if (lower.contains("€") || lower.contains("euros") || lower.contains("eur")) {
            return "EUR";
        }
        if (lower.contains("$") || lower.contains("dollars") || lower.contains("usd")) {
            return "USD";
        }
        return "";
    }

    /**
     * Extrait toutes les valeurs numériques avec leur contexte
     */
    public List<Map<String, Object>> extractAllNumericValues(String text) {
        List<Map<String, Object>> results = new ArrayList<>();

        // Pattern générique pour les nombres
        Pattern genericPattern = Pattern.compile("(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?|%|mois|ans|ans?)?");
        Matcher matcher = genericPattern.matcher(text);

        while (matcher.find()) {
            Map<String, Object> match = new HashMap<>();
            match.put("rawMatch", matcher.group(0));
            match.put("value", matcher.group(1));
            match.put("position", matcher.start());
            match.put("context", extractContext(text, matcher.start(), 20));
            results.add(match);
        }

        return results;
    }

    /**
     * Extrait le contexte autour d'une correspondance
     */
    private String extractContext(String text, int position, int window) {
        int start = Math.max(0, position - window);
        int end = Math.min(text.length(), position + window);
        return text.substring(start, end).trim();
    }
}
