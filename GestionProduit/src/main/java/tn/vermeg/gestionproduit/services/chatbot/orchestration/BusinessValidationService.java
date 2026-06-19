package tn.vermeg.gestionproduit.services.chatbot.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.entities.Garantie;
import tn.vermeg.gestionproduit.entities.Pack;
import tn.vermeg.gestionproduit.entities.PackGarantie;
import tn.vermeg.gestionproduit.entities.Produit;
import tn.vermeg.gestionproduit.enums.CouvertureGeographique;
import tn.vermeg.gestionproduit.enums.DomaineMedical;
import tn.vermeg.gestionproduit.enums.NiveauCouverture;
import tn.vermeg.gestionproduit.enums.Statut;
import tn.vermeg.gestionproduit.enums.TypeClient;
import tn.vermeg.gestionproduit.enums.TypeMontant;
import tn.vermeg.gestionproduit.enums.TypePlafond;
import tn.vermeg.gestionproduit.enums.TypeProduit;

import java.util.*;
 //Service de validation métier avant calcul du score global.
 // Applique des règles métier pour réduire le score si des champs critiques manquent.
@Service
public class BusinessValidationService {

    private static final Logger logger = LoggerFactory.getLogger(BusinessValidationService.class);
// Résultat de la validation métier avec score ajusté
    public static class BusinessValidationResult {
        private final boolean isValid;
        private final double originalScore;
        private final double adjustedScore;
        private final List<String> validationErrors;
        private final List<String> validationWarnings;
        private final Map<String, String> missingCriticalFields;
        private final List<String> suggestions;

        public BusinessValidationResult(boolean isValid, double originalScore, double adjustedScore,
                                       List<String> validationErrors, List<String> validationWarnings,
                                       Map<String, String> missingCriticalFields, List<String> suggestions) {
            this.isValid = isValid;
            this.originalScore = originalScore;
            this.adjustedScore = adjustedScore;
            this.validationErrors = validationErrors;
            this.validationWarnings = validationWarnings;
            this.missingCriticalFields = missingCriticalFields;
            this.suggestions = suggestions;
        }

        public boolean isValid() { return isValid; }
        public double getOriginalScore() { return originalScore; }
        public double getAdjustedScore() { return adjustedScore; }
        public List<String> getValidationErrors() { return validationErrors; }
        public List<String> getValidationWarnings() { return validationWarnings; }
        public Map<String, String> getMissingCriticalFields() { return missingCriticalFields; }
        public List<String> getSuggestions() { return suggestions; }
    }
// Valide une entité Pack avant calcul du score
    public BusinessValidationResult validatePackForScoring(Pack pack, double originalScore) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, String> missingFields = new LinkedHashMap<>();
        List<String> suggestions = new ArrayList<>();
        double scorePenalty = 0.0;

        // Validation 1: Prix mensuel
        if (pack.getPrixMensuel() <= 0.0) {
            missingFields.put("prixMensuel", "Prix mensuel manquant ou invalide");
            scorePenalty += 0.3;
            suggestions.add("Spécifiez un prix mensuel valide (ex: 50 TND)");}

        // Validation 2: Âges
        if (pack.getAgeMinimum() == 0) {
            missingFields.put("ageMinimum", "Âge minimum manquant");
            scorePenalty += 0.15;
            suggestions.add("Indiquez l'âge minimum éligible (ex: 18 ans)");}

        if (pack.getAgeMaximum() == 0) {
            missingFields.put("ageMaximum", "Âge maximum manquant");
            scorePenalty += 0.1;
            suggestions.add("Indiquez l'âge maximum éligible (ex: 65 ans)");}

        // Validation 3: Cohérence des âges
        if (pack.getAgeMinimum() > 0 && pack.getAgeMaximum() > 0 &&
            pack.getAgeMinimum() >= pack.getAgeMaximum()) {
            errors.add("L'âge minimum doit être inférieur à l'âge maximum");
            scorePenalty += 0.2;}

        // Validation 4: Couverture géographique
        if (pack.getCouvertureGeographique() == null) {
            missingFields.put("couvertureGeographique", "Couverture géographique manquante");
            scorePenalty += 0.15;
            suggestions.add("Spécifiez la couverture géographique (NATIONAL, UE, INTERNATIONAL, etc.)");}

        // Validation 5: Niveau de couverture
        if (pack.getNiveauCouverture() == null) {
            missingFields.put("niveauCouverture", "Niveau de couverture manquant");
            scorePenalty += 0.1;
            suggestions.add("Indiquez le niveau de couverture (BASIC, PREMIUM, GOLD)");}

        // Validation 6: Produit associé (optionnel - peut être résolu par nom)
        // Le produitId n'est plus obligatoire, le nom du produit peut être utilisé
        // Cette validation est déplacée en avertissement plutôt qu'en erreur bloquante

        double adjustedScore = Math.max(0.0, originalScore - scorePenalty);
        boolean isValid = scorePenalty < 0.5; // Valide si pénalité < 50%

        logger.info("Pack validation - Original score: {}, Adjusted score: {}, Penalty: {}, Valid: {}",
                    originalScore, adjustedScore, scorePenalty, isValid);
        return new BusinessValidationResult(isValid, originalScore, adjustedScore, errors, warnings, missingFields, suggestions);
    }
//Valide une entité Garantie avant calcul du score
    public BusinessValidationResult validateGarantieForScoring(tn.vermeg.gestionproduit.entities.Garantie garantie, double originalScore) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, String> missingFields = new LinkedHashMap<>();
        List<String> suggestions = new ArrayList<>();
        double scorePenalty = 0.0;
        // Validation 1: Nom
        if (garantie.getNomGarantie() == null || garantie.getNomGarantie().isBlank()) {
            missingFields.put("nomGarantie", "Nom de la garantie manquant");
            scorePenalty += 0.4;
            suggestions.add("Spécifiez un nom pour la garantie");
        }
        // Validation 2: Taux de remboursement
        if (garantie.getTauxRemboursement() <= 0.0) {
            missingFields.put("tauxRemboursement", "Taux de remboursement manquant");
            scorePenalty += 0.3;
            suggestions.add("Indiquez le taux de remboursement (ex: 0.8 pour 80%)");
        } else if (garantie.getTauxRemboursement() > 1.0) {
            warnings.add("Le taux de remboursement semble être en pourcentage (ex: 80 au lieu de 0.8)");
            scorePenalty += 0.1;
        }
        // Validation 3: Domaine médical
        if (garantie.getDomaine() == null) {
            missingFields.put("domaine", "Domaine médical manquant");
            scorePenalty += 0.15;
            suggestions.add("Spécifiez le domaine médical (HOSPITALISATION, CONSULTATION, etc.)");
        }
        // Validation 4: Plafond
        boolean hasAnyPlafond = (garantie.getPlafondAnnuel() > 0.0) ||
                               (garantie.getPlafondMensuel() > 0.0) ||
                               (garantie.getPlafondParActe() > 0.0);
        if (!hasAnyPlafond) {
            missingFields.put("plafond", "Aucun plafond spécifié");
            scorePenalty += 0.15;
            suggestions.add("Spécifiez au moins un plafond (annuel, mensuel ou par acte)");
        }
        double adjustedScore = Math.max(0.0, originalScore - scorePenalty);
        boolean isValid = scorePenalty < 0.5;
        logger.info("Garantie validation - Original score: {}, Adjusted score: {}, Penalty: {}, Valid: {}",
                    originalScore, adjustedScore, scorePenalty, isValid);
        return new BusinessValidationResult(isValid, originalScore, adjustedScore, errors, warnings, missingFields, suggestions);
    }
// Valide une entité Produit avant calcul du score
    public BusinessValidationResult validateProduitForScoring(Produit produit, double originalScore) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, String> missingFields = new LinkedHashMap<>();
        List<String> suggestions = new ArrayList<>();
        double scorePenalty = 0.0;
        // Validation 1: Nom
        if (produit.getNomProduit() == null || produit.getNomProduit().isBlank()) {
            missingFields.put("nomProduit", "Nom du produit manquant");
            scorePenalty += 0.5;
            suggestions.add("Spécifiez un nom pour le produit");
        }
        // Validation 2: Type de produit
        if (produit.getTypeProduit() == null) {
            missingFields.put("typeProduit", "Type de produit manquant");
            scorePenalty += 0.3;
            suggestions.add("Indiquez le type de produit (SANTE, AUTO, HABITATION, VIE, EPARGNE)");
        }
        // Validation 3: Description
        if (produit.getDescription() == null || produit.getDescription().isBlank()) {
            missingFields.put("description", "Description manquante");
            scorePenalty += 0.2;
            suggestions.add("Ajoutez une description du produit");
        }
        double adjustedScore = Math.max(0.0, originalScore - scorePenalty);
        boolean isValid = scorePenalty < 0.5;
        logger.info("Produit validation - Original score: {}, Adjusted score: {}, Penalty: {}, Valid: {}",
                    originalScore, adjustedScore, scorePenalty, isValid);
        return new BusinessValidationResult(isValid, originalScore, adjustedScore, errors, warnings, missingFields, suggestions);
    }
//Valide une configuration de pack avec garanties
    public BusinessValidationResult validatePackConfigurationForScoring(PackGarantie packGarantie, double originalScore) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, String> missingFields = new LinkedHashMap<>();
        List<String> suggestions = new ArrayList<>();
        double scorePenalty = 0.0;
        // Validation 1: Taux de remboursement
        if (packGarantie.getTauxRemboursement() <= 0.0) {
            missingFields.put("tauxRemboursement", "Taux de remboursement manquant");
            scorePenalty += 0.3;
            suggestions.add("Spécifiez le taux de remboursement pour cette garantie dans le pack");
        }
        // Validation 2: Plafond
        if (packGarantie.getPlafond() <= 0.0) {
            missingFields.put("plafond", "Plafond manquant");
            scorePenalty += 0.25;
            suggestions.add("Spécifiez le plafond pour cette garantie dans le pack");
        }
        // Validation 3: Franchise
        if (packGarantie.getFranchise() <= 0.0) {
            warnings.add("Franchise non spécifiée, sera définie à 0 par défaut");
            scorePenalty += 0.05;
        }
        double adjustedScore = Math.max(0.0, originalScore - scorePenalty);
        boolean isValid = scorePenalty < 0.5;
        logger.info("Pack configuration validation - Original score: {}, Adjusted score: {}, Penalty: {}, Valid: {}",
                    originalScore, adjustedScore, scorePenalty, isValid);
        return new BusinessValidationResult(isValid, originalScore, adjustedScore, errors, warnings, missingFields, suggestions);
    }
// Valide une liste de garanties avant calcul du score
    public BusinessValidationResult validateGarantiesListForScoring(List<tn.vermeg.gestionproduit.entities.Garantie> garanties, double originalScore) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        Map<String, String> missingFields = new LinkedHashMap<>();
        List<String> suggestions = new ArrayList<>();
        double scorePenalty = 0.0;
        int invalidCount = 0;

        if (garanties == null || garanties.isEmpty()) {
            missingFields.put("garanties", "Aucune garantie spécifiée");
            scorePenalty += 0.4;
            suggestions.add("Ajoutez au moins une garantie au pack");
        } else {
            // Validation individuelle des garanties
            for (tn.vermeg.gestionproduit.entities.Garantie garantie : garanties) {
                BusinessValidationResult result = validateGarantieForScoring(garantie, 1.0);
                if (!result.isValid()) {
                    invalidCount++;
                    missingFields.putAll(result.getMissingCriticalFields());
                }
            }
            if (invalidCount > 0) {
                scorePenalty += 0.2 * (invalidCount / (double) garanties.size());
                suggestions.add(invalidCount + " garantie(s) invalide(s) détectée(s)");
            }
        }
        double adjustedScore = Math.max(0.0, originalScore - scorePenalty);
        boolean isValid = scorePenalty < 0.5;

        logger.info("Garanties list validation - Count: {}, Invalid: {}, Original score: {}, Adjusted score: {}, Valid: {}",
                    garanties != null ? garanties.size() : 0, invalidCount, originalScore, adjustedScore, isValid);

        return new BusinessValidationResult(isValid, originalScore, adjustedScore, errors, warnings, missingFields, suggestions);
    }
// Génère un message de validation structuré
    public String generateValidationMessage(BusinessValidationResult result) {
        StringBuilder message = new StringBuilder();
        if (!result.isValid()) {message.append("⚠️ Validation métier échouée\n\n");
        } else {message.append("✅ Validation métier réussie\n\n");}
        message.append(String.format("Score original: %.2f\n", result.getOriginalScore()));
        message.append(String.format("Score ajusté: %.2f\n\n", result.getAdjustedScore()));
        if (!result.getMissingCriticalFields().isEmpty()) {
            message.append("Champs critiques manquants:\n");
            for (Map.Entry<String, String> entry : result.getMissingCriticalFields().entrySet()) {
                message.append(String.format("  • %s: %s\n", entry.getKey(), entry.getValue()));
            }
            message.append("\n");
        }
        if (!result.getValidationErrors().isEmpty()) {
            message.append("Erreurs de validation:\n");
            for (String error : result.getValidationErrors()) {
                message.append(String.format("  • %s\n", error));
            }
            message.append("\n");
        }
        if (!result.getValidationWarnings().isEmpty()) {
            message.append("Avertissements:\n");
            for (String warning : result.getValidationWarnings()) {
                message.append(String.format("  • %s\n", warning));
            }
            message.append("\n");
        }
        if (!result.getSuggestions().isEmpty()) {
            message.append("Suggestions:\n");
            for (String suggestion : result.getSuggestions()) {
                message.append(String.format("  • %s\n", suggestion));
            }
        }
        return message.toString();
    }
}
