package tn.vermeg.gestionproduit.services.chatbot.analysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.services.chatbot.core.ChatbotAction;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
@Service
public class PromptAnalyzerService {
    private static final Logger logger = LoggerFactory.getLogger(PromptAnalyzerService.class);
    // ========== KEYWORDS AVEC PRIORISATION ==========
    // PRODUIT: Keywords spécifiques au contexte produit d'assurance
    private static final List<String> PRODUIT_KEYWORDS = Arrays.asList(
        "produit", "product", "assurance-produit", "formule principale"
    );
    // GARANTIE: Keywords spécifiques au contexte garantie/couverture
    private static final List<String> GARANTIE_KEYWORDS = Arrays.asList(
        "garantie", "warranty", "couverture", "assurance", "protection"
    );
    // PACK: Keywords spécifiques au contexte pack/offre
    private static final List<String> PACK_KEYWORDS = Arrays.asList(
        "pack", "package", "offre", "formule"
    );
    // RECOMMANDATION: Keywords spécifiques au contexte recommandation/conseil
    private static final List<String> RECOMMANDATION_KEYWORDS = Arrays.asList(
        "recommand", "recommend", "conseil", "adapter", "adapté", "correspond", "correspondant",
        "cherche", "recherche", "besoin", "besoins", "idéal", "ideal", "meilleur", "meilleure",
        "top", "top 3", "top3", "suggérer", "suggere", "proposer", "propose"
    );
    // Actions EXPLICITEES: Phrases exactes avec contexte élevé
    private static final Map<String, ChatbotAction> EXPLICIT_PHRASES = Map.ofEntries(
        // CRÉATION DE PRODUIT
        Map.entry("créer un produit", ChatbotAction.PRODUIT),
        Map.entry("create a product", ChatbotAction.PRODUIT),
        Map.entry("créer une produit", ChatbotAction.PRODUIT),
        Map.entry("nouveau produit", ChatbotAction.PRODUIT),
        Map.entry("ajouter un produit", ChatbotAction.PRODUIT),
        // CRÉATION DE GARANTIE
        Map.entry("créer une garantie", ChatbotAction.GARANTIE),
        Map.entry("create a warranty", ChatbotAction.GARANTIE),
        Map.entry("créer une couverture", ChatbotAction.GARANTIE),
        Map.entry("nouvelle garantie", ChatbotAction.GARANTIE),
        Map.entry("ajouter une garantie", ChatbotAction.GARANTIE),
        // CRÉATION DE PACK
        Map.entry("créer un pack", ChatbotAction.PACK),
        Map.entry("create a pack", ChatbotAction.PACK),
        Map.entry("nouveau pack", ChatbotAction.PACK),
        Map.entry("ajouter un pack", ChatbotAction.PACK),
        // CONFIGURATION
        Map.entry("configurer un pack", ChatbotAction.CONFIGURATION_PACK),
        Map.entry("configure a pack", ChatbotAction.CONFIGURATION_PACK),
        Map.entry("configurer le pack", ChatbotAction.CONFIGURATION_PACK)
    );
    private static final List<String> CREATION_KEYWORDS = Arrays.asList(
        "créer", "create", "ajouter", "add", "nouveau", "new", "génération", "générer"
    );
    private static final List<String> CONFIGURATION_KEYWORDS = Arrays.asList(
        "configurer", "configure", "configuration", "paramétrer", "setting", "modifier", "update"
    );
    private static final List<String> AJOUT_KEYWORDS = Arrays.asList(
        "ajout", "ajouter", "add", "intégrer", "integration"
    );
    public ChatbotAction analyzeAction(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            logger.warn("Prompt vide ou null");
            return null;
        }

        String lowerPrompt = prompt.toLowerCase();
        logger.info("ANALYSE ACTION - Prompt: {}", prompt);
        ChatbotAction explicitAction = detectExplicitPhrase(lowerPrompt);
        if (explicitAction != null) {
            logger.info("Action detectee NIVEAU 1 (Phrase Explicite): {} - Confiance: 100%", explicitAction);
            return explicitAction;
        }
        //COMBINAISONS ACTION + ENTITÉ (80% CONFIANCE)
        ChatbotAction combinedAction = detectCombinedAction(lowerPrompt);
        if (combinedAction != null) {
            logger.info("Action detectee NIVEAU 2 (Combinaison Explicite): {} - Confiance: 80%", combinedAction);
            return combinedAction;
        }
        //ENTITÉ SEULE AVEC PRIORISATION (60% CONFIANCE)
        ChatbotAction defaultAction = detectByEntity(lowerPrompt);
        if (defaultAction != null) {
            logger.info("Action detectee NIVEAU 3 (Entite Primaire): {} - Confiance: 60%", defaultAction);
            return defaultAction;
        }
        
        logger.warn("AUCUNE ACTION DETECTEE pour prompt: {}", prompt);
        return null;
    }
    // Détection par phrases exactes (priorité maximale)
    private ChatbotAction detectExplicitPhrase(String lowerPrompt) {
        for (Map.Entry<String, ChatbotAction> entry : EXPLICIT_PHRASES.entrySet()) {
            if (lowerPrompt.contains(entry.getKey())) {
                logger.debug("   → Phrase trouvée: '{}' → Action: {}", entry.getKey(), entry.getValue());
                return entry.getValue();
            }
        }
        return null;
    }
    // Détection par combinaisons (action + entité avec contexte)
     //Exemple: "créer" + "produit" (sans "garantie") = PRODUIT
    private ChatbotAction detectCombinedAction(String lowerPrompt) {
        boolean hasCreation = containsAny(lowerPrompt, CREATION_KEYWORDS);
        boolean hasConfiguration = containsAny(lowerPrompt, CONFIGURATION_KEYWORDS);
        boolean hasAjout = containsAny(lowerPrompt, AJOUT_KEYWORDS);
        
        boolean hasProduitKeyword = containsAny(lowerPrompt, PRODUIT_KEYWORDS);
        boolean hasGarantieKeyword = containsAny(lowerPrompt, GARANTIE_KEYWORDS);
        boolean hasPackKeyword = containsAny(lowerPrompt, PACK_KEYWORDS);
        boolean hasRecommandationKeyword = containsAny(lowerPrompt, RECOMMANDATION_KEYWORDS);
        // RÈGLE 1: Ajout de garantie à pack
        if (hasAjout && hasGarantieKeyword && hasPackKeyword) {
            logger.debug("   → Combinaison détectée: ajout + garantie + pack");
            return ChatbotAction.AJOUT_GARANTIE_PACK;
        }
        // RÈGLE 2: Configuration de pack
        if (hasConfiguration && hasPackKeyword) {
            logger.debug("   → Combinaison détectée: configure + pack");
            return ChatbotAction.CONFIGURATION_PACK;
        }
        // RÈGLE 3: Création avec priorités strictes
        if (hasCreation) {
            // 3a: PRODUIT = création + "produit" (sans "garantie" ou "pack")
            if (hasProduitKeyword && !hasGarantieKeyword && !hasPackKeyword) {
                logger.debug("   → Combinaison détectée: création + PRODUIT (exclusive)");
                return ChatbotAction.PRODUIT;
            }
            // 3b: GARANTIE = création + "garantie" (sans "produit" ou "pack")
            if (hasGarantieKeyword && !hasProduitKeyword && !hasPackKeyword) {
                logger.debug("   → Combinaison détectée: création + GARANTIE (exclusive)");
                return ChatbotAction.GARANTIE;
            }
            // 3c: PACK = création + "pack"
            if (hasPackKeyword && !hasProduitKeyword && !hasGarantieKeyword) {
                logger.debug("   → Combinaison détectée: création + PACK (exclusive)");
                return ChatbotAction.PACK;
            }
            // 3d: Ambiguïté avec création - PRIORISATION STRICTE
            if (hasProduitKeyword) {
                logger.debug("   → Création + AMBIGUÏTÉ → Priorisation: PRODUIT");
                return ChatbotAction.PRODUIT;
            }
            if (hasPackKeyword) {
                logger.debug("   → Création + AMBIGUÏTÉ → Priorisation: PACK");
                return ChatbotAction.PACK;
            }
            if (hasGarantieKeyword) {
                logger.debug("   → Création + AMBIGUÏTÉ → Priorisation: GARANTIE");
                return ChatbotAction.GARANTIE;
            }
        }
        
        // RÈGLE 4: Détection de recommandation (priorité haute)
        if (hasRecommandationKeyword && (hasPackKeyword || hasProduitKeyword)) {
            logger.debug("   → Combinaison détectée: recommandation + pack/produit");
            return ChatbotAction.RECOMMANDATION;
        }
        // RÈGLE 5: Détection de recommandation par contexte de besoin personnel
        if (hasRecommandationKeyword && 
            (lowerPrompt.contains("je suis") || lowerPrompt.contains("je cherche") || 
             lowerPrompt.contains("mes besoins") || lowerPrompt.contains("pour ma famille") ||
             lowerPrompt.contains("pour moi"))) {
            logger.debug("   → Combinaison détectée: recommandation par contexte personnel");
            return ChatbotAction.RECOMMANDATION;
        }
        return null;
    }
     // Détection par entité seule (fallback) Priorisation: PRODUIT > PACK > GARANTIE
    private ChatbotAction detectByEntity(String lowerPrompt) {
        if (containsAny(lowerPrompt, PRODUIT_KEYWORDS)) {
            logger.debug("   → Entité détectée: PRODUIT");
            return ChatbotAction.PRODUIT;
        }
        if (containsAny(lowerPrompt, PACK_KEYWORDS)) {
            logger.debug("  → Entité détectée: PACK");
            return ChatbotAction.PACK;
        }
        if (containsAny(lowerPrompt, GARANTIE_KEYWORDS)) {
            logger.debug("  → Entité détectée: GARANTIE");
            return ChatbotAction.GARANTIE;
        }
        return null;
    }
    // ========== HELPERS UTILITAIRES ==========
    private boolean containsExplicitPhrase(String prompt, String... phrases) {
        for (String phrase : phrases) {
            if (prompt.contains(phrase)) return true;
        }
        return false;
    }
    private boolean containsAny(String text, List<String> keywords) {
        return keywords.stream().anyMatch(text::contains);
    }
    public String extractNomGarantie(String prompt) {
        // Priorité 1: extraction explicite avec "nommée/nommé" ou "appelée"
        String explicitName = extractValueWithPattern(prompt, 
            "(?:garantie|warranty|coverage)\\s+(?:nommée?|appelée?|appelé|dénommée?)\\s+[\"']?([^\"',.;!?]+)[\"']?");
        if (explicitName != null) {
            // Nettoyer le résultat en supprimant "nommee" au début
            if (explicitName.toLowerCase().startsWith("nommee ")) {
                explicitName = explicitName.substring(7).trim();
            }
            return explicitName;
        }
        
        // Priorité 2: extraction implicite - nom directement après "garantie"
        Pattern implicitPattern = Pattern.compile(
            "garantie\\s+([^,.!?]+?)\\s+(?:active|inactif|avec|de\\s+type|statut|résiliable|plafond|taux|franchise|coût|durée|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );
        Matcher matcher = implicitPattern.matcher(prompt);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            // Nettoyer le résultat en supprimant "nommee" au début
            if (name.toLowerCase().startsWith("nommee ")) {
                name = name.substring(7).trim();
            }
            return name;
        }
        
        // Priorité 3: extraction avec guillemets
        String quotedName = extractValueWithPattern(prompt, 
            "[\"']([^\"',.;!?\\s]+)[\"']?\\s+(?:garantie|warranty|coverage)");
        if (quotedName != null) return quotedName;
        return null;
    }
    public String extractNomProduit(String prompt) {
        return extractValueWithPattern(prompt,
            "(?:produit|product)\\s+(?:d['']\\s*\\w+\\s+)*nommé\\s+[\"']?([^\"',.;!?]+)[\"']?|"
            + "(?:produit|product)\\s+(?:nommé|appelé|dénommé|intitulé)\\s+[\"']?([^\"',.;!?]+)[\"']?|"
            + "[\"']([^\"',.;!?]+)[\"']?\\s+(?:produit|product)");
    }
    public String extractNomPack(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return null;
        }
        String[] patterns = {
            // pack … nommé … (nom non greedy jusqu'à délimiteur)
            "(?is)(?:pack|package|offre|formule)\\b.+?nomm[eé]\\s+(?:l['']|la\\s+)?[\"']?([^\"'\\n]+?)[\"']?"
                + "(?=\\s*(?:,|\\n|prix\\s+mensuel|type\\s+de\\s+client|cible\\s+clients?|description|pour\\s+un\\s+produit|pour\\s+le\\s+produit|les\\s+garanties|avec\\s+les\\s+garanties|dur[eé]e|âge|age|$))",
            // « nommé X pack » / guillemets
            "(?is)[\"']([^\"'\\n]{2,100})[\"']\\s*(?:pack|package|offre|formule)\\b",
            "(?is)(?:pack|package|offre|formule)\\s+(?:nommé|appelé|dénommé|intitulé)\\s+[\"']?([^\"',.;!?\\n]+)[\"']?",
            "(?is)[\"']([^\"',.;!?\\n]+)[\"']?\\s+(?:pack|package|offre|formule)\\b"
        };
        for (String p : patterns) {
            String v = extractValueWithPattern(prompt, p);
            if (v != null) {
                String t = v.trim();
                if (t.length() >= 2) {
                    return t;
                }
            }
        }
        return null;
    }
    public String extractDescription(String prompt) {
        // Pattern amélioré pour capturer des descriptions plus longues et complètes
        // Capture jusqu'aux délimiteurs de fin de phrase ou de section
        Pattern pattern = Pattern.compile(
            "(?:description|détail|détails?)\\s*[:]?\\s*[\"']?([^\"'\\n]{10,2000}?)(?:[\"']|\\n\\n|\\n(?:nom|prix|type|niveau|couverture|âge|age|garanties|type client|durée|statut|un\\s+âge|une\\s+ancienneté|un\\s*prix|le\\s*type|la\\s*couverture|le\\s*niveau|$))|" +
            "avec (?:une?|la)?\\s+description\\s+([^\"'\\n]{10,2000}?)(?:\\s*,\\s*(?:un\\s+âge|une\\s+ancienneté|un\\s+prix|le\\s*type|la\\s*couverture|le\\s*niveau|nom|prix|type|niveau|couverture|âge|age|garanties|type client|durée|statut|$))|" +
            "[\"']([^\"'\\n]{10,2000}?)[\"']?\\s*(?:comme description|pour la description|description :)"
        );
        Matcher matcher = pattern.matcher(prompt);
        if (matcher.find()) {
            String desc = matcher.group(1) != null ? matcher.group(1) : 
                         matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            if (desc != null) {
                desc = desc.trim();
                // Nettoyer les suffixes parasites comme "et le"
                if (desc.endsWith(" et le")) {
                    desc = desc.substring(0, desc.length() - 6).trim();
                }
                return desc;
            }
        }
        
        //  extraire la description entre guillemets si présente
        Pattern quotedPattern = Pattern.compile("[\"']([^\"']{15,500})[\"']");
        Matcher quotedMatcher = quotedPattern.matcher(prompt);
        if (quotedMatcher.find()) {
            String possibleDesc = quotedMatcher.group(1).trim();
            // Vérifier que ce n'est probablement pas un nom ou une autre valeur
            if (possibleDesc.length() > 20 && 
                (possibleDesc.contains(" ") || possibleDesc.contains("pour") || possibleDesc.contains("avec"))) {
                return possibleDesc;
            }
        }
        
        return null;
    }
    public String extractTypeProduit(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("santé") || lowerPrompt.contains("sante")) return "SANTE";
        if (lowerPrompt.contains("auto") || lowerPrompt.contains("voiture") || lowerPrompt.contains("véhicule")) return "AUTO";
        if (lowerPrompt.contains("habitation") || lowerPrompt.contains("logement") || lowerPrompt.contains("maison")) return "HABITATION";
        if (lowerPrompt.contains("vie") || lowerPrompt.contains("décès") || lowerPrompt.contains("survie")) return "VIE";
        if (lowerPrompt.contains("prévoyance") || lowerPrompt.contains("prevoyance") || lowerPrompt.contains("prévision")) return "PREVOYANCE";
        if (lowerPrompt.contains("épargne") || lowerPrompt.contains("epargne") || lowerPrompt.contains("investissement")) return "EPARGNE";
        return null;
    }
    public String extractDomaineMedical(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        
        // Mapping vers les domaines médicaux précis de DomaineMedical
        if (lowerPrompt.contains("hospitalisation") || lowerPrompt.contains("hôpital")) return "HOSPITALISATION";
        if (lowerPrompt.contains("consultation") || lowerPrompt.contains("médecin") || lowerPrompt.contains("spécialiste")) return "CONSULTATION_GENERALE";
        if (lowerPrompt.contains("dentaire") || lowerPrompt.contains("dent") || lowerPrompt.contains("soin dentaire")) return "DENTAIRE";
        if (lowerPrompt.contains("optique") || lowerPrompt.contains("lunettes") || lowerPrompt.contains("vue") || lowerPrompt.contains("verre")) return "OPTIQUE";
        if (lowerPrompt.contains("médicament") || lowerPrompt.contains("pharmacie") || lowerPrompt.contains("médicaments")) return "PHARMACIE";
        
        // Domaines médicaux spécialisés
        if (lowerPrompt.contains("pédiatrie") || lowerPrompt.contains("enfant")) return "PEDIATRIE";
        if (lowerPrompt.contains("cardio") || lowerPrompt.contains("cœur")) return "CARDIOLOGIE";
        if (lowerPrompt.contains("dermato") || lowerPrompt.contains("peau")) return "DERMATOLOGIE";
        if (lowerPrompt.contains("obstetrique") || lowerPrompt.contains("obstétrique") || lowerPrompt.contains("maternité") || lowerPrompt.contains("femme")) return "OBSTETRIQUE";
        if (lowerPrompt.contains("gynéco") || lowerPrompt.contains("maternité") || lowerPrompt.contains("femme")) return "GYNECOLOGIE";
        if (lowerPrompt.contains("ophtalmo") || lowerPrompt.contains("œil")) return "OPHTALMOLOGIE";
        if (lowerPrompt.contains("neuro") || lowerPrompt.contains("cerveau")) return "NEUROLOGIE";
        if (lowerPrompt.contains("ortho") || lowerPrompt.contains("os")) return "CHIRURGIE_ORTHOPEDIQUE";
        if (lowerPrompt.contains("radio") || lowerPrompt.contains("imagerie")) return "RADIOLOGIE";
        if (lowerPrompt.contains("analyse") || lowerPrompt.contains("biologique")) return "ANALYSES_BIOLOGIQUES";
        if (lowerPrompt.contains("kinesi") || lowerPrompt.contains("physio")) return "KINESITHERAPIE";
        if (lowerPrompt.contains("psychiatrie") || lowerPrompt.contains("psychologie")) return "PSYCHIATRIE";
        if (lowerPrompt.contains("preventive") || lowerPrompt.contains("préventive") || lowerPrompt.contains("prevention") || lowerPrompt.contains("prévention")) return "MEDECINE_PREVENTIVE";
        
        return "AUTRE";
    }
    public String extractTypeMontant(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("tarif convention") || lowerPrompt.contains("tarif conventionne")) return "TARIF_CONVENTIONNE";
        if (lowerPrompt.contains("forfait")) return "FORFAIT";
        if (lowerPrompt.contains("frais reels") || lowerPrompt.contains("frais réels")) return "FRAIS_REELS";
        return "FRAIS_REELS"; // Valeur par défaut
    }
    public Double extractTauxRemboursement(String prompt) {
        Double result = extractDoubleWithPattern(prompt,
            "(?:taux|taux de|pourcentage)\\s+(?:de )?remboursement\\s*[:]?\\s*(\\d+(?:[.,]\\d+)?)%?|" +
            "remboursement\\s+(?:de|à)\\s*(\\d+(?:[.,]\\d+)?)%?|" +
            "(\\d+(?:[.,]\\d+)?)%?\\s*(?:de )?remboursement");
        
        if (result != null && result > 1.0) {
            result = result / 100.0;
        }
        return result != null ? result : 0.0;
    }
    public PlafondData extractPlafonds(String prompt) {
        PlafondData plafonds = new PlafondData();
        try {
            plafonds.annuel = extractDoubleWithPattern(prompt,
                "plafond\\s+annuel\\s+de\\s+(\\d+(?:[.,]\\d+)?)|" +
                "(?:plafond|limite)\\s+(?:annuel|annuelle)\\s*[:]?\\s*(\\d+(?:[\\s\\d.,]*)?)|" +
                "(\\d+(?:[.,]\\d+)?)\\s*(?:€|euros?)?\\s*(?:par )?an");
            if (plafonds.annuel == null) plafonds.annuel = 0.0;
            
            plafonds.mensuel = extractDoubleWithPattern(prompt,
                "plafond\\s+mensuel\\s+de\\s+(\\d+(?:[.,]\\d+)?)|" +
                "(?:plafond|limite)\\s+(?:mensuel|mensuelle)\\s*[:]?\\s*(\\d+(?:[\\s\\d.,]*)?)|" +
                "(\\d+(?:[.,]\\d+)?)\\s*(?:€|euros?)?\\s*(?:par )?mois");
            if (plafonds.mensuel == null) plafonds.mensuel = 0.0;
            
            plafonds.parActe = extractDoubleWithPattern(prompt,
                "plafond\\s+par\\s+acte\\s+de\\s+(\\d+(?:[.,]\\d+)?)|" +
                "(?:plafond|limite)\\s+(?:par acte|par actes?)\\s*[:]?\\s*(\\d+(?:[\\s\\d.,]*)?)|" +
                "(\\d+(?:[.,]\\d+)?)\\s*(?:€|euros?)?\\s*(?:par )?acte");
            if (plafonds.parActe == null) plafonds.parActe = 0.0;
        } catch (Exception e) {
            logger.warn("Erreur extraction plafonds: {}", e.getMessage());
            plafonds.annuel = 0.0;
            plafonds.mensuel = 0.0;
            plafonds.parActe = 0.0;
        }
        return plafonds;
    }
    public Double extractCoutMoyenParSinistre(String prompt) {
        Double result = extractDoubleWithPattern(prompt,
            "co[uû]t\\s+moyen\\s+de\\s+(\\d+(?:[.,]\\d+)?)|" +
            "co[uû]t\\s+moyen\\s+par\\s+sinistre\\s+de\\s+(\\d+(?:[.,]\\d+)?)|" +
            "coût\\s*[:]?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:€|euros?)?");
        return result != null ? result : 0.0;
    }
    public Integer extractDureeMinContrat(String prompt) {
        Integer result = extractIntegerWithPattern(prompt,
            "dur[ée]+\\s+(?:de\\s+)?contrat\\s+entre\\s+(\\d+)\\s+et|" +
            "(?:durée|duree)\\s+(?:minimum|min)\\s+(?:de )?contrat\\s*[:]?\\s*(\\d+)|" +
            "contrat\\s+d(?:e\\s+)?(\\d+)\\s+(?:mois|ans)|" +
            "dur[ée]+\\s+minimale\\s+(?:de\\s+)?(\\d+)|" +
            "(\\d+)\\s+à\\s+\\d+\\s+mois"); // Pattern pour "X à Y mois" - capture première valeur
        return result != null ? result : 0;
    }
    public Integer extractDureeMaxContrat(String prompt) {
        // D'abord chercher le pattern "X à Y mois" pour capturer la valeur max
        Pattern rangePattern = Pattern.compile("(\\d+)\\s+à\\s+(\\d+)\\s+mois");
        Matcher rangeMatcher = rangePattern.matcher(prompt);
        if (rangeMatcher.find()) {
            return Integer.parseInt(rangeMatcher.group(2));
        }
        
        // Sinon utiliser les autres patterns
        Integer result = extractIntegerWithPattern(prompt,
            "dur[ée]+\\s+(?:de\\s+)?contrat\\s+entre\\s+\\d+\\s+et\\s+(\\d+)|" +
            "(?:durée|duree)\\s+(?:maximum|max)\\s+(?:de )?contrat\\s*[:]?\\s*(\\d+)|" +
            "jusqu'à\\s+(\\d+)\\s+(?:mois|ans)\\s+de contrat|" +
            "dur[ée]+\\s+maximale\\s+(?:de\\s+)?(\\d+)");
        
        return result != null ? result : 0;
    }
    public Boolean extractResiliableAnnuellement(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("résiliable chaque année") || 
            lowerPrompt.contains("resiliable chaque annee") ||
            lowerPrompt.contains("résiliable annuellement") ||
            lowerPrompt.contains("resiliable annuellement")) {
            return true;
        }
        if (lowerPrompt.contains("irrésiliable") || 
            lowerPrompt.contains("irresiliable") ||
            lowerPrompt.contains("non résiliable") ||
            lowerPrompt.contains("non resiliable")) {
            return false;
        }
        return true;
    }
    public Double extractFranchise(String prompt) {
        if (prompt == null || prompt.isBlank()) return 0.0;
        try {
            List<Pattern> patterns = List.of(
                Pattern.compile("franchise\\s+de\\s+(\\d+[.,]?\\d*)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("franchise\\s*:\\s*(\\d+[.,]?\\d*)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("franchise\\s+(\\d+[.,]?\\d*)", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?:sans|aucune)\\s+franchise", Pattern.CASE_INSENSITIVE)
            );
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(prompt);
                if (matcher.find()) {
                    String value = matcher.group(1);
                    if (value != null) {
                        String normalizedValue = value.replace(" ", "").replace(",", ".");
                        return Double.parseDouble(normalizedValue);
                    } else {
                        return 0.0;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Erreur extraction franchise", e);
        }
        return 0.0;
    }
    public Double extractPrixMensuel(String prompt) {
        Double result = extractDoubleWithPattern(prompt,
            "(?:prix|co[uû]t|tarif)\\s+(?:mensuel|mensuelle)\\s*(?::|de|à|a|égale?|egale?)?\\s*(\\d+(?:[.,]\\d+)?)|" +
            "(?:prix|co[uû]t|tarif)\\s+(?:mensuel|mensuelle)\\s+(\\d+(?:[.,]\\d+)?)|" +
            "(\\d+(?:[.,]\\d+)?)\\s*(?:€|euros?)?\\s*(?:par )?mois");
        return result != null ? result : 0.0;
    }
    public Integer extractAgeMinimum(String prompt) {
        Integer result = extractIntegerWithPattern(prompt,
            "(?:âge|age)\\s+(?:minimum|min)\\s*(?:de|:)?\\s*(\\d+)\\s*ans?|" +
            "à partir de\\s+(\\d+)\\s+ans|" +
            "(?:âge|age)\\s*[:]?\\s*(\\d+)\\s*(?:ans|et plus)\\s*(?:minimum|min)?|" +
            "(?:pour|destiné aux?|ciblant)\\s+(?:les?\\s*)?(?:seniors|personnes âgées)\\s*(?:de\\s+)?(\\d+)\\s*ans?|" +
            "(?:de|d'|à)\\s*(\\d+)\\s*(?:à|a|-|–)\\s*\\d+\\s*ans?|" +
            "tranche\\s+d['ââ]ge\\s*:?\\s*(\\d+)\\s*(?:à|a|-|–)\\s*\\d+\\s*ans?|" +
            "pour\\s+(?:les?\\s*)?(?:personnes|clients?)\\s*(?:âgées?\\s*(?:de\\s+)?)?(\\d+)\\s*(?:à|a|-|–)\\s*\\d+\\s*ans?|" +
            "(\\d+)\\s*(?:à|a|-|–)\\s*\\d+\\s*ans?\\s*(?:d'âge|d'age)?");
        return result != null ? result : 0;
    }
    public Integer extractAgeMaximum(String prompt) {
        Integer result = extractIntegerWithPattern(prompt,
            "(?:âge|age)\\s+(?:maximum|max)\\s*(?:de|:)?\\s*(\\d+)\\s*ans?|" +
            "jusqu'à\\s+(\\d+)\\s+ans|" +
            "(?:âge|age)\\s*[:]?\\s*(\\d+)\\s*(?:ans|et moins)\\s*(?:maximum|max)?|" +
            "(?:de|d'|à)\\s*\\d+\\s*(?:à|a|-|–)\\s*(\\d+)\\s*ans?|" +
            "tranche\\s+d['ââ]ge\\s*:?\\s*\\d+\\s*(?:à|a|-|–)\\s*(\\d+)\\s*ans?|" +
            "pour\\s+(?:les?\\s*)?(?:personnes|clients?)\\s*(?:âgées?\\s*(?:de\\s+)?)?\\d+\\s*(?:à|a|-|–)\\s*(\\d+)\\s*ans?|" +
            "(\\d+)\\s*(?:à|a|-|–)\\s*(\\d+)\\s*ans?\\s*(?:d'âge|d'age)?");
        return result != null ? result : 120; // Valeur par défaut réaliste si non spécifié
    }
    public String extractCouvertureGeographique(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        // Priorité: NATIONAL avant UE pour éviter les faux positifs
        if (lowerPrompt.contains("national") || lowerPrompt.contains("france") || lowerPrompt.contains("tunisie")) return "NATIONAL";
        if (lowerPrompt.contains("internationale") || lowerPrompt.contains("international") || lowerPrompt.contains("monde")) return "INTERNATIONAL";
        if (lowerPrompt.contains("europe") || lowerPrompt.contains(" ue ") || lowerPrompt.contains("union européenne") || lowerPrompt.contains("union europeenne")) return "UE";
        if (lowerPrompt.contains("maghreb") || lowerPrompt.contains("afrique du nord")) return "MAGHREB";
        if (lowerPrompt.contains("régional") || lowerPrompt.contains("regional") || lowerPrompt.contains("local")) return "REGIONAL";
        return null; // Ne pas appliquer de valeur par défaut - laisser le système demander explicitement
    }
    public String extractNiveauCouverture(String prompt) {
        String lowerPrompt = prompt.toLowerCase();
        if (lowerPrompt.contains("gold")) {
            return "GOLD";
        }
        if (lowerPrompt.contains("premium") || lowerPrompt.contains("supérieur") || lowerPrompt.contains("superieur")) {
            return "PREMIUM";
        }
        if (lowerPrompt.contains("bronze") || lowerPrompt.contains("basic") || lowerPrompt.contains("silver") || lowerPrompt.contains("standard") || lowerPrompt.contains("moyen")) {
            return "BASIC";
        }
        return null; // Ne pas appliquer de valeur par défaut - laisser le système demander explicitement
    }
     // Types de clients explicitement mentionnés (liste vide si aucune occurrence).
     //Ne pas confondre avec la valeur par défaut métier : géré au niveau DTO / Pack.
    public List<String> extractTypeClientLabels(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return List.of();
        }
        List<String> found = new ArrayList<>();
        Pattern p1 = Pattern.compile(
            "type\\s+de\\s+clients?\\s*(?::|est|=)?\\s*(FAMILLE|FAMILIALE?|INDIVIDUEL(?:LE)?|ENTREPRISE|SENIOR)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m1 = p1.matcher(prompt);
        while (m1.find()) {
            mapClientToken(found, m1.group(1));
        }
        if (!found.isEmpty()) {
            return found;
        }
        Pattern p2 = Pattern.compile(
            "cible\\s+(?:clients?|clientèle)\\s*(?::|est|=)?\\s*(FAMILLE|FAMILIALE?|INDIVIDUEL(?:LE)?|ENTREPRISE|SENIOR)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m2 = p2.matcher(prompt);
        while (m2.find()) {
            mapClientToken(found, m2.group(1));
        }
        if (!found.isEmpty()) {
            return found;
        }
        Pattern p3 = Pattern.compile(
            "clients?\\s+(?:cible|type)\\s*(?::|est|=)?\\s*(FAMILLE|FAMILIALE?|INDIVIDUEL(?:LE)?|ENTREPRISE|SENIOR)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        Matcher m3 = p3.matcher(prompt);
        while (m3.find()) {
            mapClientToken(found, m3.group(1));
        }
        return found;
    }
    private static void mapClientToken(List<String> out, String raw) {
        if (raw == null) {
            return;
        }
        String u = raw.toUpperCase(Locale.FRENCH).trim();
        String label;
        if (u.startsWith("FAMIL")) {
            label = "FAMILLE";
        } else if (u.contains("INDIVID")) {
            label = "INDIVIDUEL";
        } else if (u.contains("ENTREPR")) {
            label = "ENTREPRISE";
        } else if (u.contains("SENIOR")) {
            label = "SENIOR";
        } else {
            label = null;
        }
        if (label != null && !out.contains(label)) {
            out.add(label);
        }
    }
    public Integer extractAncienneteContrat(String prompt) {
        Integer result = extractIntegerWithPattern(prompt,
            "(?:ancienneté|ancien)\\s*[:]?\\s*(\\d+)\\s*(?:mois|ans)|" +
            "(\\d+)\\s*(?:mois|ans)\\s+d'ancienneté");
        return result != null ? result : 0;
    }
   //Extrait l'âge d'une personne du prompt
     public Integer extractAge(String prompt) {
        Integer result = extractIntegerWithPattern(prompt,
            "(?:âge|age)\\s*(?:de|:)?\\s*(\\d+)\\s*ans?|" +
            "(\\d+)\\s*ans?\\s*(?:d'âge|d'age)|" +
            "j(?:e ai|'ai)\\s*(\\d+)\\s*ans?|" +
            "de\\s+(\\d+)\\s*ans?|" +
            "(?:ag[eé]e?\\s*(?:de\\s+)?)?(\\d+)\\s*ans?");
        return result != null ? result : null;
    }
    // Extrait le nombre d'enfants du prompt
    public Integer extractNumberOfChildren(String prompt) {
        Integer result = extractIntegerWithPattern(prompt,
            "(\\d+)\\s*(?:enfants?|fils?|fille?s?)|" +
            "avec\\s*(\\d+)\\s*enfants?|" +
            "(\\d+)\\s*enfants?\\s*à charge");
        return result != null ? result : null;
    }
    
    // Extrait les maladies chroniques du prompt
    public List<String> extractChronicDiseases(String prompt) {
        List<String> diseases = new ArrayList<>();
        String lowerPrompt = prompt.toLowerCase();
        
        // Liste de conditions médicales courantes
        String[] conditions = {
            "cholestérol", "cholesterol", "diabète", "diabete", "hypertension", "tension",
            "asthme", "arthrite", "cancer", "insuffisance", "cardiaque", "maladie cardiaque",
            "maladie respiratoire", "maladie rénale", "hépatite", "sida", "hiv",
            "maladie chronique", "trouble cardiaque", "problème cardiaque",
            "maladie de cœur", "problème de santé", "condition médicale"
        };
        
        for (String condition : conditions) {
            if (lowerPrompt.contains(condition)) {
                diseases.add(condition);
            }
        }
        
        return diseases.isEmpty() ? null : diseases;
    }
    
    /**
     * Extrait les garanties multiples du prompt
     */
    public List<String> extractGaranties(String prompt) {
        List<String> garanties = new ArrayList<>();
        if (prompt == null || prompt.isBlank()) {
            return null;
        }
        
        // Pattern pour capturer les noms de garanties
        // Capture les expressions comme "Garantie X", "inclut X", "avec X", etc.
        Pattern pattern = Pattern.compile(
            "(?:garantie|incluant?|avec|couvre|comprend|contient)\\s+([A-Z][A-Za-zÀ-ÿ\\s]+?)(?:,|\\.|et|;|$|pour|ainsi que)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
        );
        
        Matcher matcher = pattern.matcher(prompt);
        Set<String> uniqueGaranties = new LinkedHashSet<>();
        
        while (matcher.find()) {
            String garantie = matcher.group(1).trim();
            if (garantie.length() > 2 && !garantie.toLowerCase().matches("^(?:le|la|les|un|une|des|ce|cet|cette|ces)\\s.*")) {
                uniqueGaranties.add(garantie);
            }
        }
        
        // Aussi chercher des patterns de liste
        Pattern listPattern = Pattern.compile(
            "(?:garanties?\\s*(?::|suivantes?)?|inclut)\\s*:([^,.]+)(?:,|\\.|et|$)",
            Pattern.CASE_INSENSITIVE
        );
        Matcher listMatcher = listPattern.matcher(prompt);
        while (listMatcher.find()) {
            String[] items = listMatcher.group(1).split(",");
            for (String item : items) {
                String garantie = item.trim();
                if (garantie.length() > 2) {
                    uniqueGaranties.add(garantie);
                }
            }
        }
        
        if (!uniqueGaranties.isEmpty()) {
            garanties.addAll(uniqueGaranties);
            return garanties;
        }
        
        return null;
    }
    
    /**
     * Extrait le budget mensuel du prompt
     */
    public Double extractBudget(String prompt) {
        Double result = extractDoubleWithPattern(prompt,
            "budget\\s*(?:mensuel|mensuelle)?\\s*(?:de|:)?\\s*(\\d+(?:[.,]\\d+)?)|" +
            "(\\d+(?:[.,]\\d+)?)\\s*(?:€|euros?|tnd)?\\s*(?:par )?mois|" +
            "budget\\s*(?:de|:)?\\s*(\\d+(?:[.,]\\d+)?)\\s*(?:€|euros?|tnd)?");
        return result != null ? result : null;
    }
    // ========== HELPERS POUR REGEX ==========
    private String extractValueWithPattern(String prompt, String regexPattern) {
        Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(prompt);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String value = matcher.group(i);
                if (value != null && !value.trim().isEmpty()) {
                    return value.trim();
                }
            }
        }
        return null;
    }
    private Double extractDoubleWithPattern(String prompt, String regexPattern) {
        Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(prompt);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String value = matcher.group(i);
                if (value != null && !value.trim().isEmpty()) {
                    try {
                        String normalizedValue = value.replaceAll("\\s+", "").replace(",", ".");
                        return Double.parseDouble(normalizedValue);
                    } catch (NumberFormatException e) {
                        logger.warn("Impossible de parser le nombre: {}", value);
                    }
                }
            }
        }
        return null;
    }
    private Integer extractIntegerWithPattern(String prompt, String regexPattern) {
        Pattern pattern = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(prompt);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                String value = matcher.group(i);
                if (value != null && !value.trim().isEmpty()) {
                    try {
                        return Integer.parseInt(value);
                    } catch (NumberFormatException e) {
                        logger.warn("Impossible de parser l'entier: {}", value);
                    }
                }
            }
        }
        return null;
    }
    // ========== CLASSE INTERNE ==========
    public static class PlafondData {
        public Double annuel;
        public Double mensuel;
        public Double parActe;
        @Override
        public String toString() {
            return String.format("PlafondData{annuel=%s, mensuel=%s, parActe=%s}", 
                               annuel, mensuel, parActe);
        }
    }
}
/**
 * Service d'analyse UNIFIÉ et ROBUSTE pour la détection d'actions et l'extraction de données.
 * - Détection d'action ultra-robuste avec 3 niveaux de priorité
 * - Extraction de données via regex et patterns intelligents
 * - Fallback automatique en cas d'IA non disponible
 * - Logs détaillés pour débogage
 * - Gestion stricte des ambiguïtés
 *
 * PRIORITÉS DE DÉTECTION:
 * 1. Phrases explicites metier (100% confiance)
 * 2. Combinaisons action + entite distinctes (80% confiance)
 * 3. Detection par entite seule avec priorisation (60% confiance)
 */
