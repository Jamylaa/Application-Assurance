package tn.vermeg.gestionproduit.services.chatbot.analysis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service d'extraction de listes complexes de garanties avec attributs détaillés.
 * Supporte l'extraction de garanties avec leurs taux, plafonds, franchises, etc.
 * 
 * @author PFE Ingénieur - GestionProduit
 * @version 2.0 - Enhanced AI Analysis Engine
 */
@Service
public class ComplexGuaranteeExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(ComplexGuaranteeExtractionService.class);

    /**
     * Représente une garantie extraite avec ses attributs détaillés
     */
    public static class ExtractedGuarantee {
        private String name;
        private Double tauxRemboursement;
        private Double plafond;
        private Double franchise;
        private String domaine;
        private boolean optionnelle;
        private Double supplementPrix;
        private int priority;
        private String rawMatch;

        public ExtractedGuarantee() {
            this.optionnelle = false;
            this.supplementPrix = 0.0;
            this.priority = 1;
        }

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public Double getTauxRemboursement() { return tauxRemboursement; }
        public void setTauxRemboursement(Double tauxRemboursement) { this.tauxRemboursement = tauxRemboursement; }
        public Double getPlafond() { return plafond; }
        public void setPlafond(Double plafond) { this.plafond = plafond; }
        public Double getFranchise() { return franchise; }
        public void setFranchise(Double franchise) { this.franchise = franchise; }
        public String getDomaine() { return domaine; }
        public void setDomaine(String domaine) { this.domaine = domaine; }
        public boolean isOptionnelle() { return optionnelle; }
        public void setOptionnelle(boolean optionnelle) { this.optionnelle = optionnelle; }
        public Double getSupplementPrix() { return supplementPrix; }
        public void setSupplementPrix(Double supplementPrix) { this.supplementPrix = supplementPrix; }
        public int getPriority() { return priority; }
        public void setPriority(int priority) { this.priority = priority; }
        public String getRawMatch() { return rawMatch; }
        public void setRawMatch(String rawMatch) { this.rawMatch = rawMatch; }

        @Override
        public String toString() {
            return String.format("ExtractedGuarantie{name='%s', taux=%.2f, plafond=%.2f, franchise=%.2f, optionnelle=%s}",
                               name, tauxRemboursement, plafond, franchise, optionnelle);
        }
    }

    /**
     * Extrait une liste complexe de garanties avec leurs attributs détaillés
     */
    public List<ExtractedGuarantee> extractComplexGuarantees(String text) {
        List<ExtractedGuarantee> guarantees = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return guarantees;
        }

        logger.debug("Extraction de garanties complexes depuis: {}", text);

        // Méthode 1: Pattern structuré avec parenthèses
        guarantees.addAll(extractStructuredGuarantees(text));

        // Méthode 2: Pattern avec délimiteurs (virgules, "et", "ainsi que")
        if (guarantees.isEmpty()) {
            guarantees.addAll(extractDelimitedGuarantees(text));
        }

        // Méthode 3: Pattern avec numérotation (1., 2., etc.)
        if (guarantees.isEmpty()) {
            guarantees.addAll(extractNumberedGuarantees(text));
        }

        // Méthode 4: Pattern simple (fallback)
        if (guarantees.isEmpty()) {
            guarantees.addAll(extractSimpleGuarantees(text));
        }

        // Déduplication par nom
        guarantees = deduplicateGuarantees(guarantees);

        logger.info("Extraction terminée: {} garanties trouvées", guarantees.size());
        for (ExtractedGuarantee g : guarantees) {
            logger.debug("  - {}", g);
        }

        return guarantees;
    }

    /**
     * Extrait les garanties avec un pattern structuré (avec parenthèses)
     * Ex: "Hospitalisation (80%, 10000€), Consultation (70%, 500€)"
     */
    private List<ExtractedGuarantee> extractStructuredGuarantees(String text) {
        List<ExtractedGuarantee> guarantees = new ArrayList<>();

        // Pattern: Nom (taux%, plafond€, franchise€)
        Pattern pattern = Pattern.compile(
            "([A-Z][A-Za-zÀ-ÿ\\s]+?)\\s*\\(([^)]+)\\)",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            String attributes = matcher.group(2).trim();

            ExtractedGuarantee guarantee = new ExtractedGuarantee();
            guarantee.setName(name);
            guarantee.setRawMatch(matcher.group(0));

            // Extraire les attributs
            extractAttributesFromText(guarantee, attributes);

            guarantees.add(guarantee);
        }

        return guarantees;
    }

    /**
     * Extrait les garanties avec des délimiteurs
     * Ex: "Hospitalisation à 80%, Consultation à 70%, et Dentaire à 60%"
     */
    private List<ExtractedGuarantee> extractDelimitedGuarantees(String text) {
        List<ExtractedGuarantee> guarantees = new ArrayList<>();

        // Découper par les délimiteurs
        String[] parts = text.split("(?i),|\\bet\\b|\\bainsi que\\b|\\;");

        for (String part : parts) {
            ExtractedGuarantee guarantee = extractSingleGuaranteeFromText(part.trim());
            if (guarantee != null && guarantee.getName() != null) {
                guarantees.add(guarantee);
            }
        }

        return guarantees;
    }

    /**
     * Extrait les garanties avec numérotation
     * Ex: "1. Hospitalisation, 2. Consultation, 3. Dentaire"
     */
    private List<ExtractedGuarantee> extractNumberedGuarantees(String text) {
        List<ExtractedGuarantee> guarantees = new ArrayList<>();

        Pattern pattern = Pattern.compile(
            "\\d+[.)]\\s*([A-Z][A-Za-zÀ-ÿ\\s]+?)(?:,|;|$)",
            Pattern.CASE_INSENSITIVE
        );

        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String name = matcher.group(1).trim();
            ExtractedGuarantee guarantee = new ExtractedGuarantee();
            guarantee.setName(name);
            guarantee.setRawMatch(matcher.group(0));

            // Chercher les attributs dans le contexte autour
            int start = Math.max(0, matcher.start() - 50);
            int end = Math.min(text.length(), matcher.end() + 50);
            String context = text.substring(start, end);
            extractAttributesFromText(guarantee, context);

            guarantees.add(guarantee);
        }

        return guarantees;
    }

    /**
     * Extrait les garanties avec un pattern simple (fallback)
     */
    private List<ExtractedGuarantee> extractSimpleGuarantees(String text) {
        List<ExtractedGuarantee> guarantees = new ArrayList<>();

        // Mots-clés de garantie
        String[] guaranteeKeywords = {
            "hospitalisation", "consultation", "dentaire", "optique", "médicament",
            "pharmacie", "soins", "examen", "analyse", "radio", "maternité",
            "chirurgie", "kinésithérapie", "hospital", "medic", "denta", "optiq"
        };

        for (String keyword : guaranteeKeywords) {
            Pattern pattern = Pattern.compile(
                "\\b(" + keyword + "[a-zàâäéèêëïîôöùûüÿç\\s]{0,30})\\b",
                Pattern.CASE_INSENSITIVE
            );

            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                String name = matcher.group(1).trim();
                if (name.length() > 3) {
                    ExtractedGuarantee guarantee = new ExtractedGuarantee();
                    guarantee.setName(name);
                    guarantee.setRawMatch(matcher.group(0));

                    // Chercher les attributs dans le contexte
                    int start = Math.max(0, matcher.start() - 100);
                    int end = Math.min(text.length(), matcher.end() + 100);
                    String context = text.substring(start, end);
                    extractAttributesFromText(guarantee, context);

                    guarantees.add(guarantee);
                }
            }
        }

        return guarantees;
    }

    /**
     * Extrait une garantie unique depuis un texte
     */
    private ExtractedGuarantee extractSingleGuaranteeFromText(String text) {
        // Extraire le nom (premier mot significatif)
        Pattern namePattern = Pattern.compile(
            "([A-Z][A-Za-zÀ-ÿ]+(?:\\s+[A-Z][A-Za-zÀ-ÿ]+)?)",
            Pattern.CASE_INSENSITIVE
        );

        Matcher nameMatcher = namePattern.matcher(text);
        if (!nameMatcher.find()) {
            return null;
        }

        ExtractedGuarantee guarantee = new ExtractedGuarantee();
        guarantee.setName(nameMatcher.group(1).trim());
        guarantee.setRawMatch(text);

        extractAttributesFromText(guarantee, text);

        return guarantee;
    }

    /**
     * Extrait les attributs (taux, plafond, franchise) depuis un texte
     */
    private void extractAttributesFromText(ExtractedGuarantee guarantee, String text) {
        // Extraire le taux de remboursement
        Pattern tauxPattern = Pattern.compile(
            "(\\d+(?:[.,]\\d+)?)\\s*%|taux\\s*(?:de)?\\s*(\\d+(?:[.,]\\d+)?)",
            Pattern.CASE_INSENSITIVE
        );

        Matcher tauxMatcher = tauxPattern.matcher(text);
        if (tauxMatcher.find()) {
            String tauxStr = tauxMatcher.group(1) != null ? tauxMatcher.group(1) : tauxMatcher.group(2);
            try {
                double taux = Double.parseDouble(tauxStr.replace(",", "."));
                guarantee.setTauxRemboursement(taux > 1 ? taux / 100.0 : taux);
            } catch (NumberFormatException e) {
                logger.warn("Erreur parsing taux: {}", tauxStr);
            }
        }

        // Extraire le plafond
        Pattern plafondPattern = Pattern.compile(
            "plafond\\s*(?:de|:)?\\s*(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?)?|" +
            "(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?)?\\s*(?:de )?plafond",
            Pattern.CASE_INSENSITIVE
        );

        Matcher plafondMatcher = plafondPattern.matcher(text);
        if (plafondMatcher.find()) {
            String plafondStr = plafondMatcher.group(1) != null ? plafondMatcher.group(1) : plafondMatcher.group(2);
            try {
                double plafond = Double.parseDouble(plafondStr.replace(",", ".").replaceAll("\\s", ""));
                guarantee.setPlafond(plafond);
            } catch (NumberFormatException e) {
                logger.warn("Erreur parsing plafond: {}", plafondStr);
            }
        }

        // Extraire la franchise
        Pattern franchisePattern = Pattern.compile(
            "franchise\\s*(?:de|:)?\\s*(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?)?",
            Pattern.CASE_INSENSITIVE
        );

        Matcher franchiseMatcher = franchisePattern.matcher(text);
        if (franchiseMatcher.find()) {
            try {
                double franchise = Double.parseDouble(franchiseMatcher.group(1).replace(",", "."));
                guarantee.setFranchise(franchise);
            } catch (NumberFormatException e) {
                logger.warn("Erreur parsing franchise: {}", franchiseMatcher.group(1));
            }
        }

        // Détecter si optionnelle
        guarantee.setOptionnelle(text.toLowerCase().contains("optionnelle") || 
                                 text.toLowerCase().contains("option") ||
                                 text.toLowerCase().contains("facultatif"));

        // Extraire le supplément de prix
        Pattern supplementPattern = Pattern.compile(
            "suppl[ée]ment\\s*(?:de|:)?\\s*(\\d+[.,]?\\d*)\\s*(?:€|euros?|eur|TND|tunisien|dinars?)?",
            Pattern.CASE_INSENSITIVE
        );

        Matcher supplementMatcher = supplementPattern.matcher(text);
        if (supplementMatcher.find()) {
            try {
                double supplement = Double.parseDouble(supplementMatcher.group(1).replace(",", "."));
                guarantee.setSupplementPrix(supplement);
            } catch (NumberFormatException e) {
                logger.warn("Erreur parsing supplément: {}", supplementMatcher.group(1));
            }
        }

        // Déduir le domaine
        guarantee.setDomaine(deduceGuaranteeDomaine(guarantee.getName()));
    }

    /**
     * Déduit le type de garantie à partir du nom
     */
    private String deduceGuaranteeType(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("hospital")) return "HOSPITALISATION";
        if (lower.contains("consult")) return "CONSULTATION";
        if (lower.contains("dent")) return "DENTAIRE";
        if (lower.contains("opti")) return "OPTIQUE";
        if (lower.contains("medic") || lower.contains("pharm")) return "MEDICAMENTS";
        if (lower.contains("examen") || lower.contains("analyse")) return "EXAMEN";
        return "AUTRE";
    }

    /**
     * Déduit le domaine médical à partir du nom
     */
    private String deduceGuaranteeDomaine(String name) {
        String lower = name.toLowerCase();
        if (lower.contains("hospital")) return "HOSPITALISATION";
        if (lower.contains("consult")) return "CONSULTATION_GENERALE";
        if (lower.contains("dent")) return "DENTAIRE";
        if (lower.contains("opti")) return "OPTIQUE";
        if (lower.contains("medic") || lower.contains("pharm")) return "MEDICAMENTS";
        if (lower.contains("pediatr") || lower.contains("enfant")) return "PEDIATRIE";
        if (lower.contains("cardio")) return "CARDIOLOGIE";
        if (lower.contains("gyneco")) return "GYNECOLOGIE";
        return "AUTRE";
    }
    private List<ExtractedGuarantee> deduplicateGuarantees(List<ExtractedGuarantee> guarantees) {
        Map<String, ExtractedGuarantee> uniqueMap = new LinkedHashMap<>();

        for (ExtractedGuarantee guarantee : guarantees) {
            String key = guarantee.getName().toLowerCase().replaceAll("\\s+", "");
            
            // Fusionner avec l'existante si plus d'attributs
            ExtractedGuarantee existing = uniqueMap.get(key);
            if (existing == null) {
                uniqueMap.put(key, guarantee);
            } else {
                // Conserver les attributs les plus complets
                if (guarantee.getTauxRemboursement() != null && existing.getTauxRemboursement() == null) {
                    existing.setTauxRemboursement(guarantee.getTauxRemboursement());
                }
                if (guarantee.getPlafond() != null && existing.getPlafond() == null) {
                    existing.setPlafond(guarantee.getPlafond());
                }
                if (guarantee.getFranchise() != null && existing.getFranchise() == null) {
                    existing.setFranchise(guarantee.getFranchise());
                }
            }
        }

        return new ArrayList<>(uniqueMap.values());
    }

    public List<Map<String, Object>> convertToMaps(List<ExtractedGuarantee> guarantees) {
        List<Map<String, Object>> maps = new ArrayList<>();

        for (ExtractedGuarantee guarantee : guarantees) {
            Map<String, Object> map = new HashMap<>();
            map.put("name", guarantee.getName());
            map.put("tauxRemboursement", guarantee.getTauxRemboursement());
            map.put("plafond", guarantee.getPlafond());
            map.put("franchise", guarantee.getFranchise());
            map.put("domaine", guarantee.getDomaine());
            map.put("optionnelle", guarantee.isOptionnelle());
            map.put("supplementPrix", guarantee.getSupplementPrix());
            map.put("priority", guarantee.getPriority());
            map.put("rawMatch", guarantee.getRawMatch());
            maps.add(map);
        }

        return maps;
    }
}
