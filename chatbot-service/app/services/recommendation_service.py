import logging
from typing import List, Optional
from app.models.schemas import (
    RecommendationRequestDTO, RecommendationResponseDTO,
    RecommendationResultDTO, ClientProfile, ScoringResult, PackDTO
)
from app.services.spring_boot_client import SpringBootClient

logger = logging.getLogger(__name__)

# Pondération du score de compatibilité — ajustable selon retours d'usage.
# Les 3 critères doivent totaliser 1.0.
# (Les critères "éligibilité" et "type de client" ont été retirés avec Pack.criteresEligibilite,
# qui portait les seules données permettant de les évaluer — voir architecture/MODELE_METIER.md.)
POIDS_BESOINS = 0.42       # domaines médicaux souhaités vs couverts par le pack
POIDS_BUDGET = 0.33
POIDS_SEXE = 0.25

# Domaines médicaux dont la pertinence dépend du sexe du client (cf. DomaineMedical côté backend).
DOMAINES_FEMININS = {
    "MATERNITE", "GYNECOLOGIE", "OBSTETRIQUE", "FERTILITE_PMA", "SUIVI_GROSSESSE"
}


class RecommendationService:
    """Service for generating recommendations based on client profile."""

    def __init__(self, spring_boot_client: SpringBootClient):
        self.spring_boot_client = spring_boot_client

    def generate_recommendations(self, request: RecommendationRequestDTO, jwt_token: Optional[str] = None) -> RecommendationResponseDTO:
        """Generate recommendations for a client profile."""
        logger.info(f"Génération de recommandations pour le profil: {request.session_id}")

        response = RecommendationResponseDTO(
            session_id=request.session_id,
            success=False,
            message="",
            explanation=""
        )

        try:
            profile = self._convert_to_client_profile(request)

            all_packs = self.spring_boot_client.get_all_packs_sync(jwt_token)
            active_packs = [p for p in all_packs if p.statut_workflow and p.statut_workflow.value == "PUBLIE"]

            recommended_packs = []
            for pack in active_packs:
                domaines_medicaux = self._get_pack_domaines_medicaux(pack, jwt_token)
                scoring_result = self._calculate_pack_score(profile, pack, domaines_medicaux)

                if scoring_result.is_recommended:
                    detailed_explanation = self._generate_detailed_explanation(profile, pack, scoring_result, domaines_medicaux)

                    result_dto = RecommendationResultDTO(
                        id=pack.id_pack or "",
                        nom=pack.nom_pack or "",
                        description=pack.description,
                        compatibility_score=scoring_result.global_score,
                        scoring_result=scoring_result.detailed_scores,
                        why_recommended=scoring_result.justification,
                        monthly_price=pack.prix_mensuel,
                        coverage_level=pack.niveau_couverture.value if pack.niveau_couverture else "BASIC",
                        detailed_explanation=detailed_explanation
                    )
                    recommended_packs.append(result_dto)

            recommended_packs.sort(key=lambda x: x.compatibility_score, reverse=True)
            top_packs = recommended_packs[:3]

            response.recommended_packs = top_packs
            response.recommended_products = []
            response.success = True

            if not top_packs:
                response.explanation = ("Aucun pack ne correspond à votre profil. "
                                      "Essayez d'ajuster votre budget ou vos critères.")
                response.message = "Aucune recommandation disponible"
            else:
                medical_condition = ("avec conditions médicales" if profile.chronic_diseases
                                    else "sans conditions médicales")
                budget_info = f"budget mensuel de {profile.monthly_budget} TND" if profile.monthly_budget else "budget non spécifié"

                response.explanation = (
                    f"Basé sur votre profil de {profile.age} ans, {medical_condition}, "
                    f"{budget_info}, voici les packs les plus adaptés. "
                    f"Le score de compatibilité prend en compte : vos besoins médicaux "
                    f"({int(POIDS_BESOINS * 100)}%), le budget ({int(POIDS_BUDGET * 100)}%) "
                    f"et votre profil ({int(POIDS_SEXE * 100)}%)."
                )
                response.message = f"{len(top_packs)} recommandation(s) générée(s) avec succès"

        except Exception as e:
            logger.error(f"Erreur lors de la génération des recommandations", exc_info=True)
            response.success = False
            response.message = f"Erreur lors de la génération des recommandations: {str(e)}"
            response.explanation = "Une erreur est survenue lors du traitement de votre demande."

        return response

    def _convert_to_client_profile(self, request: RecommendationRequestDTO) -> ClientProfile:
        """Convert recommendation request to client profile."""
        profile = ClientProfile(
            age=request.age,
            gender=request.gender,
            marital_status=request.marital_status,
            number_of_beneficiaries=request.number_of_children or 1,
            monthly_budget=request.monthly_budget,
            monthly_income=request.monthly_income,
            smoker=request.smoker,
            chronic_diseases=request.chronic_diseases,
            medical_needs=request.medical_needs,
            geographical_zone=request.geographical_zone,
            profession=request.profession
        )

        if request.monthly_budget:
            profile.coverage_level = "PREMIUM" if request.monthly_budget >= 100 else "BASIC"

        return profile

    def _get_pack_domaines_medicaux(self, pack: PackDTO, jwt_token: Optional[str]) -> List[str]:
        """Domaines médicaux couverts par le pack — dérivés côté serveur via /packs/{id}/detail."""
        try:
            detail = self.spring_boot_client.get_pack_detail_sync(pack.id_pack, jwt_token)
            return [str(d).upper() for d in (detail.get("domainesMedicaux") or [])]
        except Exception:
            logger.warning(f"Impossible de récupérer les domaines médicaux du pack {pack.id_pack}")
            return []

    # ------------------------------------------------------------------
    # Scoring
    # ------------------------------------------------------------------

    def _calculate_pack_score(self, profile: ClientProfile, pack: PackDTO, domaines_medicaux: List[str]) -> ScoringResult:
        """Calculate compatibility score for a pack based on client profile."""
        detailed_scores = {}
        total_score = 0.0
        justification_parts = []

        besoins_score = self._calculate_needs_score(profile, domaines_medicaux)
        detailed_scores["besoins"] = besoins_score
        total_score += besoins_score * POIDS_BESOINS
        if besoins_score >= 0.8:
            justification_parts.append("couvre vos besoins médicaux")

        budget_score = self._calculate_budget_score(profile, pack)
        detailed_scores["budget"] = budget_score
        total_score += budget_score * POIDS_BUDGET
        if budget_score >= 0.8:
            justification_parts.append("budget adapté")
        elif budget_score < 0.4:
            justification_parts.append("budget serré")

        gender_score = self._calculate_gender_score(profile, domaines_medicaux)
        detailed_scores["profil"] = gender_score
        total_score += gender_score * POIDS_SEXE

        justification = ", ".join(justification_parts) if justification_parts else "Pack standard"
        is_recommended = total_score >= 0.5

        return ScoringResult(
            global_score=round(total_score, 2),
            is_recommended=is_recommended,
            justification=justification,
            detailed_scores={k: round(v, 2) for k, v in detailed_scores.items()}
        )

    def _calculate_needs_score(self, profile: ClientProfile, domaines_medicaux: List[str]) -> float:
        """Compare les besoins exprimés (medical_needs + chronic_diseases) aux domaines couverts par le pack."""
        besoins = [b.upper() for b in (profile.medical_needs or [])] + [c.upper() for c in (profile.chronic_diseases or [])]
        if not besoins:
            return 0.6  # neutre-positif si aucun besoin exprimé

        if not domaines_medicaux:
            return 0.3  # pack sans domaines identifiés — score bas mais pas éliminatoire

        couverts = 0
        for besoin in besoins:
            if any(besoin in d or d in besoin for d in domaines_medicaux):
                couverts += 1

        return couverts / len(besoins)

    def _calculate_budget_score(self, profile: ClientProfile, pack: PackDTO) -> float:
        """Calculate budget compatibility score."""
        if not profile.monthly_budget or pack.prix_mensuel is None:
            return 0.5

        ratio = pack.prix_mensuel / profile.monthly_budget
        if ratio <= 0.7:
            return 1.0
        elif ratio <= 0.9:
            return 0.9
        elif ratio <= 1.0:
            return 0.8
        elif ratio <= 1.1:
            return 0.6
        elif ratio <= 1.2:
            return 0.3
        else:
            return 0.0

    def _calculate_gender_score(self, profile: ClientProfile, domaines_medicaux: List[str]) -> float:
        """Bonifie les packs dont les domaines couverts sont pertinents pour le sexe du client.
        Reste neutre si le pack ne couvre aucun domaine genré (ex: dentaire, hospitalisation générale)."""
        domaines_genres = [d for d in domaines_medicaux if d in DOMAINES_FEMININS]
        if not domaines_genres:
            return 0.7  # neutre : le pack ne cible pas de domaine genré

        gender = (profile.gender or "").upper()
        if gender in ("F", "FEMME", "FEMININ", "FÉMININ"):
            return 1.0
        elif gender in ("M", "HOMME", "MASCULIN"):
            return 0.5  # domaines féminins moins pertinents, sans exclure le pack (souvent bundlé)
        return 0.7

    def _generate_detailed_explanation(self, profile: ClientProfile, pack: PackDTO,
                                        scoring_result: ScoringResult, domaines_medicaux: List[str]) -> str:
        """Generate detailed explanation for why a pack is recommended."""
        explanations = []

        besoins_score = scoring_result.detailed_scores.get("besoins", 0)
        if besoins_score >= 0.8 and domaines_medicaux:
            explanations.append(f"Couvre vos besoins médicaux ({', '.join(domaines_medicaux[:3])})")

        budget_score = scoring_result.detailed_scores.get("budget", 0)
        if profile.monthly_budget and pack.prix_mensuel:
            if budget_score >= 0.8:
                explanations.append(f"Le prix de {pack.prix_mensuel} TND/mois est bien dans votre budget de {profile.monthly_budget} TND")
            elif budget_score >= 0.5:
                explanations.append(f"Le prix de {pack.prix_mensuel} TND/mois est acceptable pour votre budget")
            else:
                explanations.append(f"Le prix de {pack.prix_mensuel} TND/mois dépasse votre budget")

        if pack.niveau_couverture:
            explanations.append(f"Niveau de couverture {pack.niveau_couverture.value}")

        if profile.chronic_diseases:
            explanations.append("Prise en compte de vos conditions médicales particulières")

        return ". ".join(explanations) + "." if explanations else "Correspond à votre profil."
