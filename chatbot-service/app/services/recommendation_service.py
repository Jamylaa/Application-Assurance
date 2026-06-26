import logging
from typing import List, Optional
from app.models.schemas import (
    RecommendationRequestDTO, RecommendationResponseDTO,
    RecommendationResultDTO, ClientProfile, ScoringResult, PackDTO
)
from app.services.spring_boot_client import SpringBootClient

logger = logging.getLogger(__name__)


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
            # Convert request to client profile
            profile = self._convert_to_client_profile(request)
            
            # Fetch all active packs from Spring Boot with JWT token
            all_packs = self.spring_boot_client.get_all_packs_sync(jwt_token)
            active_packs = [p for p in all_packs if p.statut and p.statut.value == "ACTIF"]
            
            # Calculate scores for each pack
            recommended_packs = []
            for pack in active_packs:
                scoring_result = self._calculate_pack_score(profile, pack)
                
                if scoring_result.is_recommended:
                    # Enhanced recommendation with detailed scoring breakdown
                    detailed_explanation = self._generate_detailed_explanation(profile, pack, scoring_result)
                    
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
            
            # Sort by score descending
            recommended_packs.sort(key=lambda x: x.compatibility_score, reverse=True)
            
            # Limit to top 3
            top_packs = recommended_packs[:3]
            
            response.recommended_packs = top_packs
            response.recommended_products = []
            response.success = True
            
            if not top_packs:
                response.explanation = ("Aucun pack ne correspond à votre profil. "
                                      "Essayez d'ajuster votre budget ou vos critères.")
                response.message = "Aucune recommandation disponible"
            else:
                # Enhanced explanation with detailed breakdown
                medical_condition = ("avec conditions médicales" if profile.chronic_diseases 
                                    else "sans conditions médicales")
                budget_info = f"budget mensuel de {profile.monthly_budget}€" if profile.monthly_budget else "budget non spécifié"
                
                response.explanation = (
                    f"Basé sur votre profil de {profile.age} ans, {medical_condition}, "
                    f"{budget_info}, voici les packs les plus adaptés. "
                    f"Le score de compatibilité prend en compte: l'âge (30%), le budget (25%), "
                    f"le niveau de couverture (20%), la zone géographique (15%) et le type de client (10%)."
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
            chronic_diseases=request.chronic_diseases,
            geographical_zone=request.geographical_zone,
            profession=request.profession
        )
        
        # Set default coverage level based on budget
        if request.monthly_budget:
            if request.monthly_budget < 50:
                profile.coverage_level = "BASIC"
            elif request.monthly_budget < 100:
                profile.coverage_level = "BASIC"
            else:
                profile.coverage_level = "PREMIUM"
        
        return profile
    
    def _calculate_pack_score(self, profile: ClientProfile, pack: PackDTO) -> ScoringResult:
        """Calculate compatibility score for a pack based on client profile."""
        detailed_scores = {}
        total_score = 0.0
        justification_parts = []
        
        # Score 1: Age compatibility (30%)
        age_score = self._calculate_age_score(profile, pack)
        detailed_scores["age_compatibility"] = age_score
        total_score += age_score * 0.3
        
        if age_score >= 0.8:
            justification_parts.append("âge compatible")
        elif age_score >= 0.5:
            justification_parts.append("âge moyennement compatible")
        else:
            justification_parts.append("âge limite")
        
        # Score 2: Budget compatibility (25%)
        budget_score = self._calculate_budget_score(profile, pack)
        detailed_scores["budget_compatibility"] = budget_score
        total_score += budget_score * 0.25
        
        if budget_score >= 0.8:
            justification_parts.append("budget adapté")
        elif budget_score >= 0.5:
            justification_parts.append("budget acceptable")
        else:
            justification_parts.append("budget élevé")
        
        # Score 3: Coverage level (20%)
        coverage_score = self._calculate_coverage_score(profile, pack)
        detailed_scores["coverage_level"] = coverage_score
        total_score += coverage_score * 0.2
        
        if coverage_score >= 0.8:
            justification_parts.append("couverture excellente")
        
        # Score 4: Geographic coverage (15%)
        geo_score = self._calculate_geographic_score(profile, pack)
        detailed_scores["geographic_coverage"] = geo_score
        total_score += geo_score * 0.15
        
        # Score 5: Client type (10%)
        client_type_score = self._calculate_client_type_score(profile, pack)
        detailed_scores["client_type"] = client_type_score
        total_score += client_type_score * 0.1
        
        # Generate justification
        justification = ", ".join(justification_parts)
        if not justification:
            justification = "Pack standard"
        
        is_recommended = total_score >= 0.5
        
        return ScoringResult(
            global_score=round(total_score, 2),
            is_recommended=is_recommended,
            justification=justification,
            detailed_scores={k: round(v, 2) for k, v in detailed_scores.items()}
        )
    
    def _calculate_age_score(self, profile: ClientProfile, pack: PackDTO) -> float:
        """Calculate age compatibility score."""
        if pack.age_minimum is None or pack.age_maximum is None:
            return 0.5  # Neutral score if no age constraints
        
        if pack.age_minimum <= profile.age <= pack.age_maximum:
            # Perfect match if in middle of range
            range_size = pack.age_maximum - pack.age_minimum
            if range_size > 0:
                position = (profile.age - pack.age_minimum) / range_size
                if 0.3 <= position <= 0.7:
                    return 1.0
                else:
                    return 0.9
            return 1.0
        
        # Partial score if close to range
        if profile.age < pack.age_minimum:
            diff = pack.age_minimum - profile.age
            if diff <= 2:
                return 0.7
            elif diff <= 5:
                return 0.4
        else:
            diff = profile.age - pack.age_maximum
            if diff <= 2:
                return 0.7
            elif diff <= 5:
                return 0.4
        
        return 0.0
    
    def _calculate_budget_score(self, profile: ClientProfile, pack: PackDTO) -> float:
        """Calculate budget compatibility score."""
        if not profile.monthly_budget or pack.prix_mensuel is None:
            return 0.5  # Neutral score if no budget info
        
        if pack.prix_mensuel <= profile.monthly_budget:
            # Perfect if well within budget
            ratio = pack.prix_mensuel / profile.monthly_budget
            if ratio <= 0.7:
                return 1.0
            elif ratio <= 0.9:
                return 0.9
            else:
                return 0.8
        else:
            # Partial score if slightly over budget
            ratio = pack.prix_mensuel / profile.monthly_budget
            if ratio <= 1.1:
                return 0.6
            elif ratio <= 1.2:
                return 0.3
            else:
                return 0.0
    
    def _calculate_coverage_score(self, profile: ClientProfile, pack: PackDTO) -> float:
        """Calculate coverage level score."""
        if not pack.niveau_couverture:
            return 0.5
        
        profile_level = profile.coverage_level.upper()
        pack_level = pack.niveau_couverture.value
        
        level_hierarchy = {"BASIC": 1, "PREMIUM": 2, "GOLD": 3}
        
        profile_rank = level_hierarchy.get(profile_level, 1)
        pack_rank = level_hierarchy.get(pack_level, 1)
        
        if pack_rank >= profile_rank:
            return 1.0
        elif pack_rank == profile_rank - 1:
            return 0.7
        else:
            return 0.4
    
    def _calculate_geographic_score(self, profile: ClientProfile, pack: PackDTO) -> float:
        """Calculate geographic coverage score."""
        if not profile.geographical_zone or not pack.couverture_geographique:
            return 0.5  # Neutral score
        
        profile_zone = profile.geographical_zone.upper()
        pack_zone = pack.couverture_geographique.value
        
        if pack_zone == "INTERNATIONAL":
            return 1.0
        elif pack_zone == profile_zone:
            return 1.0
        elif pack_zone == "NATIONAL":
            return 0.8
        elif pack_zone == "UE" and profile_zone in ["UE", "MAGHREB"]:
            return 0.7
        else:
            return 0.5
    
    def _calculate_client_type_score(self, profile: ClientProfile, pack: PackDTO) -> float:
        """Calculate client type compatibility score."""
        if not pack.type_clients:
            return 0.5  # Neutral score if no client type specified
        
        # Determine client type from profile
        if profile.marital_status.lower() in ["marié", "married"]:
            client_type = "FAMILLE"
        elif profile.age >= 60:
            client_type = "SENIOR"
        elif profile.number_of_beneficiaries > 1:
            client_type = "FAMILLE"
        else:
            client_type = "INDIVIDUEL"
        
        pack_types = [ct.value for ct in pack.type_clients]
        
        if client_type in pack_types:
            return 1.0
        elif "INDIVIDUEL" in pack_types:
            return 0.7
        else:
            return 0.5
    
    def _generate_detailed_explanation(self, profile: ClientProfile, pack: PackDTO, scoring_result: ScoringResult) -> str:
        """Generate detailed explanation for why a pack is recommended."""
        explanations = []
        
        # Age explanation
        age_score = scoring_result.detailed_scores.get("age_compatibility", 0)
        if age_score >= 0.8:
            explanations.append(f"Votre âge de {profile.age} ans correspond parfaitement aux critères du pack")
        elif age_score >= 0.5:
            explanations.append(f"Votre âge de {profile.age} ans est acceptable pour ce pack")
        else:
            explanations.append(f"Votre âge de {profile.age} ans est aux limites du pack")
        
        # Budget explanation
        budget_score = scoring_result.detailed_scores.get("budget_compatibility", 0)
        if profile.monthly_budget and pack.prix_mensuel:
            if budget_score >= 0.8:
                explanations.append(f"Le prix de {pack.prix_mensuel}€/mois est bien dans votre budget de {profile.monthly_budget}€")
            elif budget_score >= 0.5:
                explanations.append(f"Le prix de {pack.prix_mensuel}€/mois est acceptable pour votre budget")
            else:
                explanations.append(f"Le prix de {pack.prix_mensuel}€/mois dépasse légèrement votre budget")
        
        # Coverage explanation
        coverage_score = scoring_result.detailed_scores.get("coverage_level", 0)
        if coverage_score >= 0.8:
            explanations.append(f"Niveau de couverture {pack.niveau_couverture.value if pack.niveau_couverture else 'BASIC'} adapté à vos besoins")
        
        # Geographic explanation
        geo_score = scoring_result.detailed_scores.get("geographic_coverage", 0)
        if geo_score >= 0.8:
            explanations.append(f"Couverture géographique {pack.couverture_geographique.value if pack.couverture_geographique else 'NATIONALE'} conforme")
        
        # Medical conditions
        if profile.chronic_diseases:
            explanations.append("Prise en compte de vos conditions médicales particulières")
        
        return ". ".join(explanations) + "."
