from pydantic import BaseModel, Field
from typing import Optional, List, Dict, Any
from datetime import datetime
from pydantic import ConfigDict
from app.models.enums import (
    CouvertureGeographique, DomaineMedical, TypeMontant,
    TypeProduit, NiveauCouverture, TypeClient, Statut, TypePlafond
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
    session_id: str
    age: int
    gender: str
    marital_status: str
    number_of_children: Optional[int] = None
    monthly_budget: Optional[float] = None
    chronic_diseases: Optional[List[str]] = None
    geographical_zone: Optional[str] = None
    coverage_type: Optional[str] = None
    profession: Optional[str] = None
    correlation_id: Optional[str] = None
    enable_debug: bool = False
# ========== Entity DTOs ==========

class GarantieDTO(CamelCaseModel):
    id_garantie: Optional[str] = None
    nom_garantie: Optional[str] = None
    description: Optional[str] = None
    statut: Optional[Statut] = None
    domaine: Optional[DomaineMedical] = None
    taux_remboursement: Optional[float] = None
    type_montant: Optional[TypeMontant] = None
    type_plafond: Optional[TypePlafond] = None
    plafond_annuel: Optional[float] = None
    plafond_mensuel: Optional[float] = None
    plafond_par_acte: Optional[float] = None
    franchise: Optional[float] = None
    cout_moyen_par_sinistre: Optional[float] = None
    duree_min_contrat: Optional[int] = None
    duree_max_contrat: Optional[int] = None
    resiliable_annuellement: Optional[bool] = None
    cree_par: Optional[str] = None
    date_creation: Optional[datetime] = None
    date_modification: Optional[datetime] = None
    date_desactivation: Optional[datetime] = None
    pack_id: Optional[str] = None  # Référence au pack parent


# DTOs simplifiés pour éviter les relations circulaires
class GarantieSimpleDTO(CamelCaseModel):
    id_garantie: Optional[str] = None
    nom_garantie: Optional[str] = None
    domaine: Optional[DomaineMedical] = None
    statut: Optional[Statut] = None


class PackSimpleDTO(CamelCaseModel):
    id_pack: Optional[str] = None
    nom_pack: Optional[str] = None
    description: Optional[str] = None
    prix_mensuel: Optional[float] = None
    niveau_couverture: Optional[NiveauCouverture] = None
    statut: Optional[Statut] = None


class PackDTO(CamelCaseModel):
    id_pack: Optional[str] = None
    nom_pack: Optional[str] = None
    description: Optional[str] = None
    produit_id: Optional[str] = None
    nom_produit: Optional[str] = None
    age_minimum: Optional[int] = None
    age_maximum: Optional[int] = None
    type_clients: Optional[List[TypeClient]] = None
    anciennete_contrat_mois: Optional[int] = None
    couverture_geographique: Optional[CouvertureGeographique] = None
    prix_mensuel: Optional[float] = None
    duree_min_contrat: Optional[int] = None
    duree_max_contrat: Optional[int] = None
    niveau_couverture: Optional[NiveauCouverture] = None
    statut: Optional[Statut] = None
    domaines_medicaux: Optional[List[str]] = None
    date_creation: Optional[datetime] = None
    date_modification: Optional[datetime] = None
    garanties: Optional[List[GarantieSimpleDTO]] = None  # Liste des garanties associées


class ProduitDTO(CamelCaseModel):
    id_produit: Optional[str] = None
    nom_produit: Optional[str] = None
    description: Optional[str] = None
    type_produit: Optional[TypeProduit] = None
    statut: Optional[Statut] = None
    date_creation: Optional[datetime] = None
    date_modification: Optional[datetime] = None
    packs: Optional[List[PackSimpleDTO]] = None  # Liste des packs associés


class PackGarantieDTO(CamelCaseModel):
    id: Optional[str] = None
    pack_id: Optional[str] = None
    garantie_id: Optional[str] = None
    taux_remboursement: Optional[float] = None
    plafond: Optional[float] = None
    franchise: Optional[float] = None
    delai_carence: Optional[int] = None
    type_montant: Optional[TypeMontant] = None
    optionnelle: Optional[bool] = None
    actif: Optional[bool] = None
    supplement_prix: Optional[float] = None
    priorite: Optional[int] = None


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
    chronic_diseases: Optional[List[str]] = None
    coverage_level: str = "BASIC"
    geographical_zone: Optional[str] = None
    profession: Optional[str] = None


class ScoringResult(BaseModel):
    global_score: float
    is_recommended: bool
    justification: str
    detailed_scores: Dict[str, float] = {}
