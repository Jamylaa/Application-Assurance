package tn.vermeg.gestionproduit.services.chatbot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tn.vermeg.gestionproduit.entities.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ValidationService {
    private static final Logger logger = LoggerFactory.getLogger(ValidationService.class);
    // Patterns de validation
    private static final Pattern NOM_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_'àâäéèêëïîôöùûüÿç]{2,100}$");
    private static final Pattern DESCRIPTION_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s\\-_',.àâäéèêëïîôöùûüÿç]{10,500}$");
    private static final Pattern TAUX_PATTERN = Pattern.compile("^(0(\\.\\d+)?|1(\\.0+)?)$"); // entre 0 et 1

    // Résultat de validation
    public static class ValidationResult {
        private boolean valid;
        private List<String> errors;
        private List<String> warnings;

        public ValidationResult() {
            this.valid = true;
            this.errors = new ArrayList<>();
            this.warnings = new ArrayList<>();
        }
        public void addError(String error) {
            this.errors.add(error);
            this.valid = false;
        }
        public void addWarning(String warning) {
            this.warnings.add(warning);
        }
        // Getters et setters
        public boolean isValid() { return valid; }
        public void setValid(boolean valid) { this.valid = valid; }
        public List<String> getErrors() { return errors; }
        public void setErrors(List<String> errors) { this.errors = errors; }
        public List<String> getWarnings() { return warnings; }
        public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    }
     // Valide une garantie
    public ValidationResult validateGarantie(tn.vermeg.gestionproduit.entities.Garantie garantie) {
        ValidationResult result = new ValidationResult();
        // Validation du nom
        if (garantie.getNomGarantie() == null || garantie.getNomGarantie().trim().isEmpty()) {
            result.addError("Le nom de la garantie est obligatoire");
        } else if (!NOM_PATTERN.matcher(garantie.getNomGarantie()).matches()) {
            result.addError("Le nom de la garantie doit contenir entre 2 et 100 caractères alphanumériques");
        }
        // Validation du type
        if (garantie.getType() == null || garantie.getType().trim().isEmpty()) {
            result.addError("Le type de garantie est obligatoire");
        } else {
            boolean isKnownType = java.util.Arrays
                    .stream(tn.vermeg.gestionproduit.entities.TypeGarantie.values())
                    .anyMatch(t -> t.name().equalsIgnoreCase(garantie.getType().trim()));
            if (!isKnownType) {
                result.addWarning("Le type '" + garantie.getType()
                        + "' n'est pas un type de garantie sante standard. Types reconnus: "
                        + java.util.Arrays.stream(tn.vermeg.gestionproduit.entities.TypeGarantie.values())
                                .map(Enum::name)
                                .collect(java.util.stream.Collectors.joining(", ")));
            }
        }
        // Validation du taux de remboursement
        if (garantie.getTauxRemboursement() < 0.0 || garantie.getTauxRemboursement() > 1.0) {
            result.addError("Le taux de remboursement doit être entre 0 et 1 (0% à 100%)");
        }
        if (garantie.getTauxRemboursement() == 0.0) {
            result.addWarning("Le taux de remboursement est à 0, vérifiez si c'est correct");
        }
        // Validation des plafonds
        validatePlafonds(garantie.getPlafondAnnuel(), garantie.getPlafondMensuel(), garantie.getPlafondParActe(), result);

        // Validation de la franchise
        if (garantie.getFranchise() < 0.0) {
            result.addError("La franchise ne peut pas être négative");
        }
        // Validation des durées de contrat
        if (garantie.getDureeMinContrat() > garantie.getDureeMaxContrat()) {
            result.addError("La durée minimale ne peut pas être supérieure à la durée maximale");
        }
        // Validation du coût moyen par sinistre
        if (garantie.getCoutMoyenParSinistre() < 0.0) {
            result.addError("Le coût moyen par sinistre ne peut pas être négatif");
        }
        // Validation du statut
        if (garantie.getStatut() == null) {
            result.addWarning("Aucun statut spécifié, ACTIF sera appliqué par défaut");
        }
        return result;
    }
     // Valide un produit
    public ValidationResult validateProduit(tn.vermeg.gestionproduit.entities.Produit produit) {
        ValidationResult result = new ValidationResult();
        // Validation du nom
        if (produit.getNomProduit() == null || produit.getNomProduit().trim().isEmpty()) {
            result.addError("Le nom du produit est obligatoire");
        } else if (!NOM_PATTERN.matcher(produit.getNomProduit()).matches()) {
            result.addError("Le nom du produit doit contenir entre 2 et 100 caractères alphanumériques");
        }
        // Validation du type
        if (produit.getTypeProduit() == null) {
            result.addError("Le type de produit est obligatoire");
        }
        // Validation de la description
        if (produit.getDescription() != null && !produit.getDescription().trim().isEmpty()) {
            if (produit.getDescription().length() < 10) {
                result.addWarning("La description est très courte, il est recommandé d'ajouter plus de détails");
            }
            if (produit.getDescription().length() > 500) {
                result.addWarning("La description est très longue, elle pourrait être tronquée");
            }
        } else {
            result.addWarning("Aucune description fournie, il est recommandé d'en ajouter une");
        }
        // Validation du statut
        if (produit.getStatut() == null) {
            result.addWarning("Aucun statut spécifié, ACTIF sera appliqué par défaut");
        }
        return result;
    }
     //Valide un pack
    public ValidationResult validatePack(tn.vermeg.gestionproduit.entities.Pack pack) {
        ValidationResult result = new ValidationResult();
        // Validation du nom
        if (pack.getNomPack() == null || pack.getNomPack().trim().isEmpty()) {
            result.addError("Le nom du pack est obligatoire");
        } else if (!NOM_PATTERN.matcher(pack.getNomPack()).matches()) {
            result.addError("Le nom du pack doit contenir entre 2 et 100 caractères alphanumériques");
        }
        // Validation des âges
        if (pack.getAgeMinimum() != null && pack.getAgeMaximum() != null) {
            if (pack.getAgeMinimum() > pack.getAgeMaximum()) {
                result.addError("L'âge minimum ne peut pas être supérieur à l'âge maximum");
            }
            if (pack.getAgeMinimum() < 0 || pack.getAgeMaximum() > 120) {
                result.addError("Les âges doivent être compris entre 0 et 120 ans");
            }
        }
        // Validation du prix mensuel
        if (pack.getPrixMensuel() < 0.0) {
            result.addError("Le prix mensuel ne peut pas être négatif");
        }
        if (pack.getPrixMensuel() > 10000) {
            result.addWarning("Le prix mensuel semble très élevé, veuillez vérifier");
        }
        if (pack.getPrixMensuel() == 0.0) {
            result.addWarning("Le prix mensuel est à 0, vérifiez si c'est correct");
        }
        // Validation des types de clients
        if (pack.getTypeClients() == null || pack.getTypeClients().isEmpty()) {
            result.addWarning("Aucun type de client spécifié, INDIVIDUEL sera appliqué par défaut");
        }
        // Validation de la couverture géographique
        if (pack.getCouvertureGeographique() == null) {
            result.addWarning("Aucune couverture géographique spécifiée, NATIONAL sera appliqué par défaut");
        }
        // Validation des durées de contrat
        if (pack.getDureeMinContrat() > pack.getDureeMaxContrat()) {
            result.addError("La durée minimale ne peut pas être supérieure à la durée maximale");
        }
        // Validation de l'ancienneté
        if (pack.getAncienneteContratMois() < 0) {
            result.addError("L'ancienneté de contrat ne peut pas être négative");
        }
        // Validation du niveau de couverture
        if (pack.getNiveauCouverture() == null) {
            result.addWarning("Aucun niveau de couverture spécifié, STANDARD sera appliqué par défaut");
        }
        // Validation du statut
        if (pack.getStatut() == null) {
            result.addWarning("Aucun statut spécifié, ACTIF sera appliqué par défaut");
        }
        return result;
    }
     //Valide une configuration de pack
    public ValidationResult validatePackConfiguration(tn.vermeg.gestionproduit.entities.PackGarantie packGarantie) {
        ValidationResult result = new ValidationResult();
        // Validation du taux de remboursement
        if (packGarantie.getTauxRemboursement() < 0.0 || packGarantie.getTauxRemboursement() > 1.0) {
            result.addError("Le taux de remboursement doit être entre 0 et 1 (0% à 100%)");
        }
        if (packGarantie.getTauxRemboursement() == 0.0) {
            result.addWarning("Le taux de remboursement est à 0, vérifiez si c'est correct");
        }
        // Validation du plafond
        if (packGarantie.getPlafond() < 0.0) {
            result.addError("Le plafond ne peut pas être négatif");
        }
        // Validation de la franchise
        if (packGarantie.getFranchise() < 0.0) {
            result.addError("La franchise ne peut pas être négative");
        }
        // Validation du supplément de prix
        if (packGarantie.getSupplementPrix() < 0.0) {
            result.addError("Le supplément de prix ne peut pas être négatif");
        }
        // Validation du délai de carence
        if (packGarantie.getDelaiCarence() < 0) {
            result.addError("Le délai de carence ne peut pas être négatif");
        }
        // Validation de la priorité
        if (packGarantie.getPriorite() < 1) {
            result.addError("La priorité doit être supérieure ou égale à 1");
        }
        return result;
    }
     //Valide les plafonds (utilisé pour garantie et pack)
    private void validatePlafonds(Double plafondAnnuel, Double plafondMensuel, Double plafondParActe, ValidationResult result) {
        if (plafondAnnuel != null && plafondAnnuel < 0) {
            result.addError("Le plafond annuel ne peut pas être négatif");
        }
        if (plafondMensuel != null && plafondMensuel < 0) {
            result.addError("Le plafond mensuel ne peut pas être négatif");
        }
        if (plafondParActe != null && plafondParActe < 0) {
            result.addError("Le plafond par acte ne peut pas être négatif");
        }
        // Validation de la cohérence des plafonds
        if (plafondAnnuel != null && plafondMensuel != null) {
            if (plafondMensuel > plafondAnnuel) {
                result.addWarning("Le plafond mensuel est supérieur au plafond annuel, cela semble incohérent");
            }
        }
        if (plafondAnnuel != null && plafondParActe != null) {
            if (plafondParActe > plafondAnnuel) {
                result.addWarning("Le plafond par acte est supérieur au plafond annuel, cela semble incohérent");
            }
        }
    }
     //Valide un dto de requête de garantie
    public ValidationResult validateGarantieRequest(tn.vermeg.gestionproduit.dto.ChatbotGarantieRequestDTO dto) {
        ValidationResult result = new ValidationResult();
        if (dto.getNomGarantie() == null || dto.getNomGarantie().trim().isEmpty()) {
            result.addError("Le nom de la garantie est obligatoire");
        }
        if (dto.getType() == null || dto.getType().trim().isEmpty()) {
            result.addError("Le type de garantie est obligatoire");
        }
        if (dto.getTauxRemboursement() != null && (dto.getTauxRemboursement() < 0 || dto.getTauxRemboursement() > 1)) {
            result.addError("Le taux de remboursement doit être entre 0 et 1");
        }
        return result;
    }
     //Valide un dto de requête de produit
    public ValidationResult validateProduitRequest(tn.vermeg.gestionproduit.dto.ChatbotProduitRequestDTO dto) {
        ValidationResult result = new ValidationResult();

        if (dto.getNomProduit() == null || dto.getNomProduit().trim().isEmpty()) {
            result.addError("Le nom du produit est obligatoire");
        }
        if (dto.getTypeProduit() == null) {
            result.addError("Le type de produit est obligatoire");
        }
        return result;
    }
     // Valide un dto de requête de pack
    public ValidationResult validatePackRequest(tn.vermeg.gestionproduit.dto.ChatbotPackRequestDTO dto) {
        ValidationResult result = new ValidationResult();

        if (dto.getNomPack() == null || dto.getNomPack().trim().isEmpty()) {
            result.addError("Le nom du pack est obligatoire");
        }
        if (dto.getAgeMinimum() != null && dto.getAgeMaximum() != null && dto.getAgeMinimum() > dto.getAgeMaximum()) {
            result.addError("L'âge minimum ne peut pas être supérieur à l'âge maximum");
        }
        if (dto.getPrixMensuel() != null && dto.getPrixMensuel() < 0) {
            result.addError("Le prix mensuel ne peut pas être négatif");
        }
        return result;
    }
     // Vérifie si une action est supportée
    public boolean isSupportedAction(ChatbotAction action) {
        return action != null && Arrays.asList(
            ChatbotAction.GARANTIE,
            ChatbotAction.PRODUIT,
            ChatbotAction.PACK,
            ChatbotAction.CONFIGURATION_PACK,
            ChatbotAction.AJOUT_GARANTIE_PACK,
            ChatbotAction.RECOMMENDATION
        ).contains(action);
    }
     //Valide un prompt
    public ValidationResult validatePrompt(String prompt) {
        ValidationResult result = new ValidationResult();
        if (prompt == null || prompt.trim().isEmpty()) {
            result.addError("Le prompt ne peut pas être vide");
            return result;
        }
        if (prompt.length() < 10) {
            result.addWarning("Le prompt est très court, les résultats pourraient être imprécis");
        }

        if (prompt.length() > 2000) {
            result.addWarning("Le prompt est très long, il pourrait être tronqué");
        }
        return result;
    }
}
