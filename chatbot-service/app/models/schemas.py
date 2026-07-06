from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime
from pydantic import ConfigDict
from app.models.enums import (
    DomaineMedical, TypeMontant,
    TypeProduit, NiveauCouverture, StatutWorkflow, TypePlafond,
    TypeFranchise, TypeRemboursement, CouvertureGeographique
)


def _camel(field_name: str) -> str:
    parts = field_name.split('_')
    return parts[0] + ''.join(w.capitalize() for w in parts[1:])


# Configuration pour utiliser camelCase dans la sérialisation JSON
class CamelCaseModel(BaseModel):
    model_config = ConfigDict(
        populate_by_name=True,
        alias_generator=_camel
    )


# ========== Request DTOs ==========
class ChatbotRequestDTO(BaseModel):
    """DTO reçu depuis Angular — accepte camelCase ET snake_case."""
    model_config = ConfigDict(populate_by_name=True, alias_generator=_camel)

    prompt: str
    session_id: Optional[str] = Field(None, alias='sessionId')
    jwt_token: Optional[str] = Field(None, alias='jwtToken')
    correlation_id: Optional[str] = Field(None, alias='correlationId')
    enable_debug: bool = Field(False, alias='enableDebug')
class RecommendationRequestDTO(BaseModel):
    """Accepte camelCase ET snake_case (aligné sur CamelCaseModel, cf. ChatbotRequestDTO)."""
    model_config = ConfigDict(populate_by_name=True, alias_generator=_camel)

    session_id: str
    age: int
    gender: str
    marital_status: str
    number_of_children: Optional[int] = None
    monthly_budget: Optional[float] = None
    monthly_income: Optional[float] = None
    smoker: Optional[bool] = None
    chronic_diseases: Optional[List[str]] = None
    medical_needs: Optional[List[str]] = None  # domaines/besoins exprimés par le client (ex: "dentaire", "maternité")
    geographical_zone: Optional[str] = None
    coverage_type: Optional[str] = None
    profession: Optional[str] = None
    correlation_id: Optional[str] = None
    enable_debug: bool = False
# ========== Entity DTOs ==========

class PlafondGarantieDTO(CamelCaseModel):
    """Value Object — remplace les anciens champs plats plafond_annuel/mensuel/par_acte."""
    plafond_par_acte: Optional[float] = None
    plafond_mensuel: Optional[float] = None
    plafond_annuel: Optional[float] = None
    plafond_global: Optional[float] = None
    plafond_par_soins: Optional[float] = None
    type_principal: Optional[TypePlafond] = None
    description: Optional[str] = None
    devise: Optional[str] = "TND"


class FranchiseGarantieDTO(CamelCaseModel):
    """Value Object — remplace l'ancien champ plat franchise (float)."""
    type: Optional[TypeFranchise] = None
    montant_fixe: Optional[float] = None
    pourcentage: Optional[float] = None
    montant_minimum: Optional[float] = None
    montant_maximum: Optional[float] = None
    description: Optional[str] = None
    devise: Optional[str] = "TND"


class RegleCalculDTO(CamelCaseModel):
    """Value Object — miroir de RegleCalcul (GestionProduit), formule de calcul personnalisée."""
    formule: Optional[str] = None
    description_formule: Optional[str] = None
    parametres_formule: Optional[List[str]] = None
    valeurs_defaut: Optional[Dict[str, float]] = None
    priorite_calcul: Optional[int] = None
    appliquer_plafond_apres_calcul: Optional[bool] = None
    deduire_franchise_avant_plafond: Optional[bool] = None
    base_conventionnee: Optional[bool] = None


class GarantieDTO(CamelCaseModel):
    id_garantie: Optional[str] = None
    nom_garantie: Optional[str] = None
    code_garantie: Optional[str] = None
    nom_court: Optional[str] = None
    description: Optional[str] = None
    description_technique: Optional[str] = None
    domaine: Optional[DomaineMedical] = None
    garantie_obligatoire_par_defaut: Optional[bool] = None
    statut_workflow: Optional[StatutWorkflow] = None
    evenements_couverts_par_defaut: Optional[List[str]] = None
    type_remboursement: Optional[TypeRemboursement] = None
    taux_remboursement_base: Optional[float] = None
    taux_remboursement_minimum: Optional[float] = None
    taux_remboursement_maximum: Optional[float] = None
    plafond: Optional[PlafondGarantieDTO] = None
    franchise: Optional[FranchiseGarantieDTO] = None
    regle_calcul: Optional[RegleCalculDTO] = None
    prerequis_garantie_ids: Optional[List[str]] = None
    parametres_dynamiques: Optional[Dict[str, Any]] = None
    prime_pure_base: Optional[float] = None
    cree_par: Optional[str] = None
    modifie_par: Optional[str] = None
    date_creation: Optional[datetime] = None
    date_modification: Optional[datetime] = None
    date_desactivation: Optional[datetime] = None


# DTOs simplifiés pour éviter les relations circulaires
class GarantieSimpleDTO(CamelCaseModel):
    id_garantie: Optional[str] = None
    nom_garantie: Optional[str] = None
    domaine: Optional[DomaineMedical] = None


class PackSimpleDTO(CamelCaseModel):
    id_pack: Optional[str] = None
    nom_pack: Optional[str] = None
    description: Optional[str] = None
    prix_mensuel: Optional[float] = None
    niveau_couverture: Optional[NiveauCouverture] = None


class PackDTO(CamelCaseModel):
    id_pack: Optional[str] = None
    nom_pack: Optional[str] = None
    code_pack: Optional[str] = None
    nom_commercial: Optional[str] = None
    description: Optional[str] = None
    description_courte: Optional[str] = None
    produit_id: Optional[str] = None
    # Champ transitoire (résolu en produit_id avant envoi), jamais persisté côté Java — exclu à la sérialisation.
    nom_produit: Optional[str] = None
    prix_mensuel: Optional[float] = None
    prix_annuel: Optional[float] = None
    taux_remise_annuelle: Optional[float] = None
    devise_prix: Optional[str] = "TND"
    version_pack: Optional[str] = None
    niveau_couverture: Optional[NiveauCouverture] = None
    pack_recommande: Optional[bool] = None
    color_theme: Optional[str] = None
    options_disponibles: Optional[bool] = None
    options_pack_ids: Optional[List[str]] = None
    packs_compatibles: Optional[List[str]] = None
    packs_incompatibles: Optional[List[str]] = None
    # Utilisé pour ne recommander que les packs publiés (cf. recommendation_service.py).
    statut_workflow: Optional[StatutWorkflow] = None
    date_effet: Optional[datetime] = None
    date_expiration: Optional[datetime] = None
    cree_par: Optional[str] = None
    modifie_par: Optional[str] = None
    date_creation: Optional[datetime] = None
    date_modification: Optional[datetime] = None
    garanties: Optional[List[GarantieSimpleDTO]] = None  # Liste des garanties associées


class ProduitDTO(CamelCaseModel):
    id_produit: Optional[str] = None
    nom_produit: Optional[str] = None
    code_produit: Optional[str] = None
    nom_commercial: Optional[str] = None
    description: Optional[str] = None
    type_produit: Optional[TypeProduit] = None
    statut_workflow: Optional[StatutWorkflow] = None
    valide_par: Optional[str] = None
    date_validation: Optional[datetime] = None
    prix_base: Optional[float] = None
    devise_prix: Optional[str] = "TND"
    couverture_geographique: Optional[CouvertureGeographique] = None
    territoires_exclus: Optional[List[str]] = None
    version: Optional[str] = None
    version_precedente_id: Optional[str] = None
    date_effet: Optional[datetime] = None
    date_expiration: Optional[datetime] = None
    cree_par: Optional[str] = None
    modifie_par: Optional[str] = None
    date_creation: Optional[datetime] = None
    date_modification: Optional[datetime] = None
    packs: Optional[List[PackSimpleDTO]] = None  # Liste des packs associés


class PackGarantieDTO(CamelCaseModel):
    id_pack_garantie: Optional[str] = None
    pack_id: Optional[str] = None
    garantie_id: Optional[str] = None
    nom_garantie: Optional[str] = None
    code_garantie: Optional[str] = None
    taux_remboursement_specifique: Optional[float] = None
    plafond_specifique: Optional[PlafondGarantieDTO] = None
    franchise_specifique: Optional[FranchiseGarantieDTO] = None
    type_montant: Optional[TypeMontant] = None
    optionnelle: Optional[bool] = None
    actif: Optional[bool] = None
    supplement_prix: Optional[float] = None
    configure_par: Optional[str] = None
    date_activation: Optional[datetime] = None
    date_desactivation: Optional[datetime] = None
    date_modification: Optional[datetime] = None


class ComplexGuaranteeDTO(CamelCaseModel):
    nom_garantie: str
    taux_remboursement: Optional[float] = None
    plafond: Optional[float] = None
    franchise: Optional[float] = None
    optionnelle: Optional[bool] = False


# ========== Response DTOs ==========

class ChatbotResponseDTO(BaseModel):
    success: bool
    intent: Optional[str] = None
    message: str
    data: Optional[Dict[str, Any]] = None
    created_entity: Optional[Dict[str, str]] = None
    refresh_targets: Optional[List[str]] = None
    errors: Optional[List[str]] = None
    warnings: Optional[List[str]] = None
    missing_fields: Optional[List[str]] = None
    confidence: Optional[float] = None
    fallback_used: bool = False
    action: Optional[str] = None
    result: Optional[Dict[str, Any]] = None
    entity_type: Optional[str] = None
    correlation_id: Optional[str] = None
    debug_trace: Optional[Dict[str, Any]] = None


class BusinessValidationResult(BaseModel):
    is_valid: bool
    original_score: float
    adjusted_score: float
    validation_errors: List[str] = []
    validation_warnings: List[str] = []
    missing_critical_fields: Dict[str, str] = {}
    suggestions: List[str] = []


class RecommendationResultDTO(BaseModel):
    id: str
    nom: str
    description: Optional[str] = None
    compatibility_score: float
    scoring_result: Optional[Dict[str, Any]] = None
    why_recommended: str
    monthly_price: Optional[float] = None
    coverage_level: str
    detailed_explanation: Optional[str] = None


class RecommendationResponseDTO(BaseModel):
    correlation_id: Optional[str] = None
    debug_trace: Optional[Dict[str, Any]] = None
    session_id: str
    success: bool
    message: str
    explanation: str
    recommended_packs: List[RecommendationResultDTO] = []
    recommended_products: List[RecommendationResultDTO] = []


class PackCreationResponseDTO(BaseModel):
    success: bool
    message: str
    pack_details: Optional[PackDTO] = None
    errors: List[str] = []
    warnings: List[str] = []


# ========== Internal DTOs ==========

class ClientProfile(BaseModel):
    age: int
    gender: str
    marital_status: str
    number_of_beneficiaries: int = 1
    monthly_budget: Optional[float] = None
    monthly_income: Optional[float] = None
    smoker: Optional[bool] = None
    chronic_diseases: Optional[List[str]] = None
    medical_needs: Optional[List[str]] = None
    coverage_level: str = "BASIC"
    geographical_zone: Optional[str] = None
    profession: Optional[str] = None


class ScoringResult(BaseModel):
    global_score: float
    is_recommended: bool
    justification: str
    detailed_scores: Dict[str, float] = {}
