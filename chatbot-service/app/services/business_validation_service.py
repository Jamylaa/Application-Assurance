import logging
from typing import Dict, List, Any
from app.models.schemas import (
    GarantieDTO, PackDTO, ProduitDTO, PackGarantieDTO,
    BusinessValidationResult
)
from app.models.enums import TypeMontant, DomaineMedical

logger = logging.getLogger(__name__)


class BusinessValidationService:
    
    def validate_pack_for_scoring(self, pack: PackDTO, original_score: float) -> BusinessValidationResult:
        errors = []
        warnings = []
        missing_fields = {}
        suggestions = []
        score_penalty = 0.0
        
        # Light validation: Check for critical missing fields only
        if not pack.nom_pack or not pack.nom_pack.strip():
            missing_fields["nomPack"] = "Nom du pack manquant"
            score_penalty += 0.2
            suggestions.append("Spécifiez un nom pour le pack")
        
        if pack.prix_mensuel is None or pack.prix_mensuel <= 0.0:
            missing_fields["prixMensuel"] = "Prix mensuel manquant ou invalide"
            score_penalty += 0.1
            suggestions.append("Spécifiez un prix mensuel valide (ex: 50 TND)")

        adjusted_score = max(0.0, original_score - score_penalty)
        is_valid = score_penalty < 0.5  # More lenient since Java validates
        
        logger.info(f"Pack light validation - Original: {original_score}, Adjusted: {adjusted_score}, "
                   f"Penalty: {score_penalty}, Valid: {is_valid}")
        
        return BusinessValidationResult(
            is_valid=is_valid,
            original_score=original_score,
            adjusted_score=adjusted_score,
            validation_errors=errors,
            validation_warnings=warnings,
            missing_critical_fields=missing_fields,
            suggestions=suggestions
        )
    
    def validate_garantie_for_scoring(self, garantie: GarantieDTO, original_score: float) -> BusinessValidationResult:
        errors = []
        warnings = []
        missing_fields = {}
        suggestions = []
        score_penalty = 0.0
        
        # Light validation: Check for critical missing fields only
        if not garantie.nom_garantie or not garantie.nom_garantie.strip():
            missing_fields["nomGarantie"] = "Nom de la garantie manquant"
            score_penalty += 0.2
            suggestions.append("Spécifiez un nom pour la garantie")
        
        if garantie.taux_remboursement_base is None or garantie.taux_remboursement_base <= 0.0:
            missing_fields["tauxRemboursement"] = "Taux de remboursement manquant"
            score_penalty += 0.1
            suggestions.append("Indiquez le taux de remboursement (ex: 80 pour 80%)")
        elif garantie.taux_remboursement_base <= 1.0:
            warnings.append("Le taux de remboursement semble être une fraction (ex: 0.8 au lieu de 80)")
            score_penalty += 0.05

        if garantie.domaine is None:
            missing_fields["domaine"] = "Domaine médical manquant"
            score_penalty += 0.1
            suggestions.append("Spécifiez le domaine médical (HOSPITALISATION, CONSULTATION, etc.)")
        
        adjusted_score = max(0.0, original_score - score_penalty)
        is_valid = score_penalty < 0.5  # More lenient since Java validates
        
        logger.info(f"Garantie light validation - Original: {original_score}, Adjusted: {adjusted_score}, "
                   f"Penalty: {score_penalty}, Valid: {is_valid}")
        
        return BusinessValidationResult(
            is_valid=is_valid,
            original_score=original_score,
            adjusted_score=adjusted_score,
            validation_errors=errors,
            validation_warnings=warnings,
            missing_critical_fields=missing_fields,
            suggestions=suggestions
        )
    
    def validate_produit_for_scoring(self, produit: ProduitDTO, original_score: float) -> BusinessValidationResult:
        errors = []
        warnings = []
        missing_fields = {}
        suggestions = []
        score_penalty = 0.0
        
        # Light validation: Check for critical missing fields only
        if not produit.nom_produit or not produit.nom_produit.strip():
            missing_fields["nomProduit"] = "Nom du produit manquant"
            score_penalty += 0.2
            suggestions.append("Spécifiez un nom pour le produit")
        
        if produit.type_produit is None:
            missing_fields["typeProduit"] = "Type de produit manquant"
            score_penalty += 0.1
            suggestions.append("Indiquez le type de produit (SANTE, AUTO, HABITATION, VIE, EPARGNE)")
        
        adjusted_score = max(0.0, original_score - score_penalty)
        is_valid = score_penalty < 0.5  # More lenient since Java validates
        
        logger.info(f"Produit light validation - Original: {original_score}, Adjusted: {adjusted_score}, "
                   f"Penalty: {score_penalty}, Valid: {is_valid}")
        
        return BusinessValidationResult(
            is_valid=is_valid,
            original_score=original_score,
            adjusted_score=adjusted_score,
            validation_errors=errors,
            validation_warnings=warnings,
            missing_critical_fields=missing_fields,
            suggestions=suggestions
        )
    
    def validate_pack_configuration_for_scoring(self, pack_garantie: PackGarantieDTO, original_score: float) -> BusinessValidationResult:
        errors = []
        warnings = []
        missing_fields = {}
        suggestions = []
        score_penalty = 0.0
        
        # Light validation: ces champs sont optionnels dans le nouveau modèle (héritent de la
        # Garantie de base si non surchargés) — on se contente d'avertir, sans pénaliser fortement.
        if pack_garantie.taux_remboursement_specifique is not None and pack_garantie.taux_remboursement_specifique <= 0.0:
            errors.append("Le taux de remboursement spécifique ne peut pas être nul ou négatif")
            score_penalty += 0.1
        
        adjusted_score = max(0.0, original_score - score_penalty)
        is_valid = score_penalty < 0.5  # More lenient since Java validates
        
        logger.info(f"Pack configuration light validation - Original: {original_score}, Adjusted: {adjusted_score}, "
                   f"Penalty: {score_penalty}, Valid: {is_valid}")
        
        return BusinessValidationResult(
            is_valid=is_valid,
            original_score=original_score,
            adjusted_score=adjusted_score,
            validation_errors=errors,
            validation_warnings=warnings,
            missing_critical_fields=missing_fields,
            suggestions=suggestions
        )
    
    def generate_validation_message(self, result: BusinessValidationResult) -> str:
        """Generate a structured validation message."""
        message = []
        
        if not result.is_valid:
            message.append("Validation métier échouée\n\n")
        else:
            message.append("Validation métier réussie\n\n")
        
        message.append(f"Score original: {result.original_score:.2f}\n")
        message.append(f"Score ajusté: {result.adjusted_score:.2f}\n\n")
        
        if result.missing_critical_fields:
            message.append("Champs critiques manquants:\n")
            for key, value in result.missing_critical_fields.items():
                message.append(f"  • {key}: {value}\n")
            message.append("\n")
        
        if result.validation_errors:
            message.append("Erreurs de validation:\n")
            for error in result.validation_errors:
                message.append(f"  • {error}\n")
            message.append("\n")
        
        if result.validation_warnings:
            message.append("Avertissements:\n")
            for warning in result.validation_warnings:
                message.append(f"  • {warning}\n")
            message.append("\n")
        
        if result.suggestions:
            message.append("Suggestions:\n")
            for suggestion in result.suggestions:
                message.append(f"  • {suggestion}\n")
        
        return "".join(message)
