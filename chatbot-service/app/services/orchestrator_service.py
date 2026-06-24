import logging
from typing import Dict, Any, Optional
from app.models.schemas import (
    ChatbotRequestDTO, ChatbotResponseDTO, GarantieDTO, PackDTO, ProduitDTO,
    BusinessValidationResult, PackCreationResponseDTO, RecommendationRequestDTO,
    RecommendationResponseDTO
)
from app.models.enums import ChatbotAction, Statut, TypeMontant, DomaineMedical, TypeProduit, NiveauCouverture, CouvertureGeographique, TypeClient
from app.services.ai_extraction_service import AIExtractionService
from app.services.business_validation_service import BusinessValidationService
from app.services.recommendation_service import RecommendationService
from app.services.spring_boot_client import SpringBootClient
from app.services.prompt_analyzer_service import PromptAnalyzerService

# Import analytics store for tracking metrics
from app.api.analytics import analytics_store

logger = logging.getLogger(__name__)


class ChatbotOrchestratorService:
    """Orchestrator service that coordinates all chatbot operations."""
    
    def __init__(self):
        self.ai_extraction_service = AIExtractionService()
        self.business_validation_service = BusinessValidationService()
        self.spring_boot_client = SpringBootClient()
        self.prompt_analyzer_service = PromptAnalyzerService()
        self.recommendation_service = RecommendationService(self.spring_boot_client)
    
    def process_prompt(self, request: ChatbotRequestDTO) -> ChatbotResponseDTO:
        """Process a chatbot prompt and return response."""
        logger.info(f"Processing prompt: {request.prompt}")
        
        # Track prompt processed
        analytics_store["prompts_processed"] = analytics_store.get("prompts_processed", 0) + 1
        
        try:
            # Step 1: Analyze action
            action = self.prompt_analyzer_service.analyze_action(request.prompt)
            
            if not action:
                return self._create_error_response(
                    "Action non détectée",
                    ["Impossible de déterminer l'action à partir du prompt"]
                )
            
            logger.info(f"Detected action: {action}")
            
            # Step 2: Execute action
            result = self._execute_action(action, request.prompt, request.session_id)
            
            # Step 3: Create response
            return self._create_success_response(action, result, request.prompt)
        
        except Exception as e:
            logger.error(f"Error processing prompt: {e}", exc_info=True)
            return self._create_error_response(
                "Erreur interne",
                [f"Une erreur est survenue: {str(e)}"]
            )
    
    def _execute_action(self, action: ChatbotAction, prompt: str, session_id: Optional[str]) -> Dict[str, Any]:
        """Execute the detected action."""
        logger.info(f"Executing action: {action}")
        
        if action == ChatbotAction.GARANTIE:
            return self._execute_create_garantie(prompt)
        elif action == ChatbotAction.PRODUIT:
            return self._execute_create_produit(prompt)
        elif action == ChatbotAction.PACK:
            return self._execute_create_pack(prompt)
        elif action == ChatbotAction.CONFIGURATION_PACK:
            return self._execute_configure_pack(prompt)
        elif action == ChatbotAction.AJOUT_GARANTIE_PACK:
            return self._execute_add_garantie_to_pack(prompt)
        elif action == ChatbotAction.RECOMMANDATION:
            return self._execute_recommendation(prompt, session_id)
        else:
            return {
                "success": False,
                "error": f"Action non implémentée: {action}"
            }
    
    def _execute_create_garantie(self, prompt: str) -> Dict[str, Any]:
        """Execute guarantee creation."""
        try:
            # Extract data using AI
            extracted_data = self.ai_extraction_service.extract_garantie_data(prompt)
            
            # Track AI call
            if extracted_data:
                analytics_store["ai_calls"] = analytics_store.get("ai_calls", 0) + 1
            else:
                analytics_store["ai_fallbacks"] = analytics_store.get("ai_fallbacks", 0) + 1
            
            # Create DTO
            garantie_dto = self._create_garantie_dto(extracted_data, prompt)
            
            # Business validation
            validation_result = self.business_validation_service.validate_garantie_for_scoring(garantie_dto, 1.0)
            
            if not validation_result.is_valid:
                return {
                    "success": False,
                    "error": "Validation échouée",
                    "details": validation_result.validation_errors,
                    "warnings": validation_result.validation_warnings,
                    "business_validation": self.business_validation_service.generate_validation_message(validation_result)
                }
            
            # Apply defaults
            self._apply_garantie_defaults(garantie_dto)
            
            # Create via Spring Boot
            created_garantie = self.spring_boot_client.create_garantie_sync(garantie_dto)
            
            # Track successful creation
            analytics_store["creations_successful"] = analytics_store.get("creations_successful", 0) + 1
            
            return {
                "success": True,
                "action": "CREATE_GARANTIE",
                "entity": created_garantie.model_dump(),
                "message": "Garantie créée avec succès",
                "id": created_garantie.id_garantie,
                "warnings": validation_result.validation_warnings,
                "business_validation": self.business_validation_service.generate_validation_message(validation_result)
            }
        
        except Exception as e:
            # Track failed creation
            analytics_store["creations_failed"] = analytics_store.get("creations_failed", 0) + 1
            logger.error(f"Error creating guarantee: {e}", exc_info=True)
            return {
                "success": False,
                "error": f"Erreur création garantie: {str(e)}"
            }
    
    def _execute_create_produit(self, prompt: str) -> Dict[str, Any]:
        """Execute product creation."""
        try:
            # Extract data using AI
            extracted_data = self.ai_extraction_service.extract_produit_data(prompt)
            
            # Track AI call
            if extracted_data:
                analytics_store["ai_calls"] = analytics_store.get("ai_calls", 0) + 1
            else:
                analytics_store["ai_fallbacks"] = analytics_store.get("ai_fallbacks", 0) + 1
            
            # Create DTO
            produit_dto = self._create_produit_dto(extracted_data, prompt)
            
            # Business validation
            validation_result = self.business_validation_service.validate_produit_for_scoring(produit_dto, 1.0)
            
            if not validation_result.is_valid:
                return {
                    "success": False,
                    "error": "Validation échouée",
                    "details": validation_result.validation_errors,
                    "warnings": validation_result.validation_warnings,
                    "business_validation": self.business_validation_service.generate_validation_message(validation_result)
                }
            
            # Apply defaults
            self._apply_produit_defaults(produit_dto)
            
            # Create via Spring Boot
            created_produit = self.spring_boot_client.create_produit_sync(produit_dto)
            
            # Track successful creation
            analytics_store["creations_successful"] = analytics_store.get("creations_successful", 0) + 1
            
            return {
                "success": True,
                "action": "CREATE_PRODUIT",
                "entity": created_produit.model_dump(),
                "message": "Produit créé avec succès",
                "id": created_produit.id_produit,
                "warnings": validation_result.validation_warnings,
                "business_validation": self.business_validation_service.generate_validation_message(validation_result)
            }
        
        except Exception as e:
            # Track failed creation
            analytics_store["creations_failed"] = analytics_store.get("creations_failed", 0) + 1
            logger.error(f"Error creating product: {e}", exc_info=True)
            return {
                "success": False,
                "error": f"Erreur création produit: {str(e)}"
            }
    
    def _execute_create_pack(self, prompt: str) -> Dict[str, Any]:
        """Execute pack creation."""
        try:
            # Extract data using AI
            extracted_data = self.ai_extraction_service.extract_pack_data(prompt)
            
            # Track AI call
            if extracted_data:
                analytics_store["ai_calls"] = analytics_store.get("ai_calls", 0) + 1
            else:
                analytics_store["ai_fallbacks"] = analytics_store.get("ai_fallbacks", 0) + 1
            
            # Create DTO
            pack_dto = self._create_pack_dto(extracted_data, prompt)
            
            # Resolve product name to ID if needed
            if pack_dto.nom_produit and not pack_dto.produit_id:
                produit = self.spring_boot_client.get_produit_by_name_sync(pack_dto.nom_produit)
                if produit:
                    pack_dto.produit_id = produit.id_produit
            
            # Business validation
            validation_result = self.business_validation_service.validate_pack_for_scoring(pack_dto, 1.0)
            
            if not validation_result.is_valid:
                return {
                    "success": False,
                    "error": "Validation échouée",
                    "details": validation_result.validation_errors,
                    "warnings": validation_result.validation_warnings,
                    "business_validation": self.business_validation_service.generate_validation_message(validation_result),
                    "pack_details": pack_dto.model_dump()
                }
            
            # Apply defaults
            self._apply_pack_defaults(pack_dto)
            
            # Create via Spring Boot
            created_pack = self.spring_boot_client.create_pack_sync(pack_dto)
            
            # Track successful creation
            analytics_store["creations_successful"] = analytics_store.get("creations_successful", 0) + 1
            
            return {
                "success": True,
                "action": "CREATE_PACK",
                "entity": created_pack.model_dump(),
                "message": "Pack créé avec succès",
                "id": created_pack.id_pack,
                "warnings": validation_result.validation_warnings,
                "business_validation": self.business_validation_service.generate_validation_message(validation_result)
            }
        
        except Exception as e:
            # Track failed creation
            analytics_store["creations_failed"] = analytics_store.get("creations_failed", 0) + 1
            logger.error(f"Error creating pack: {e}", exc_info=True)
            return {
                "success": False,
                "error": f"Erreur création pack: {str(e)}"
            }
    
    def _execute_configure_pack(self, prompt: str) -> Dict[str, Any]:
        """Execute pack configuration."""
        try:
            extracted_data = self.ai_extraction_service.extract_pack_configuration_data(prompt)
            
            # This would need to be implemented based on Spring Boot API
            return {
                "success": False,
                "error": "Configuration de pack non encore implémentée dans FastAPI"
            }
        
        except Exception as e:
            logger.error(f"Error configuring pack: {e}", exc_info=True)
            return {
                "success": False,
                "error": f"Erreur configuration pack: {str(e)}"
            }
    
    def _execute_add_garantie_to_pack(self, prompt: str) -> Dict[str, Any]:
        """Execute adding guarantee to pack."""
        try:
            extracted_data = self.ai_extraction_service.extract_add_garantie_to_pack_data(prompt)
            
            # Get pack and guarantee by name
            pack = self.spring_boot_client.get_pack_by_name_sync(extracted_data.get("nomPack", ""))
            garantie = self.spring_boot_client.get_garantie_by_name_sync(extracted_data.get("nomGarantie", ""))
            
            if not pack or not garantie:
                return {
                    "success": False,
                    "error": "Pack ou garantie non trouvé",
                    "details": {
                        "pack_found": pack is not None,
                        "garantie_found": garantie is not None
                    }
                }
            
            # Add guarantee to pack
            self.spring_boot_client.add_garantie_to_pack(
                pack.id_pack,
                garantie.id_garantie,
                extracted_data.get("tauxRemboursement", 0.8),
                extracted_data.get("plafond", 0),
                extracted_data.get("franchise", 0),
                extracted_data.get("optionnelle", False)
            )
            
            return {
                "success": True,
                "action": "ADD_GARANTIE_TO_PACK",
                "message": f"Garantie ajoutée au pack {pack.nom_pack}",
                "pack_id": pack.id_pack,
                "garantie_id": garantie.id_garantie
            }
        
        except Exception as e:
            logger.error(f"Error adding guarantee to pack: {e}", exc_info=True)
            return {
                "success": False,
                "error": f"Erreur ajout garantie au pack: {str(e)}"
            }
    
    def _execute_recommendation(self, prompt: str, session_id: Optional[str]) -> Dict[str, Any]:
        """Execute recommendation generation."""
        try:
            # Extract client profile from prompt using AI
            # For now, we'll use a simple extraction or require structured input
            
            # This would need AI extraction for client profile
            # For now, return an error asking for structured input
            return {
                "success": False,
                "error": "Veuillez utiliser l'endpoint /recommendations avec les données structurées du client"
            }
        
        except Exception as e:
            logger.error(f"Error generating recommendation: {e}", exc_info=True)
            return {
                "success": False,
                "error": f"Erreur génération recommandation: {str(e)}"
            }
    
    def _create_garantie_dto(self, extracted_data: Dict[str, Any], prompt: str) -> GarantieDTO:
        """Create guarantee DTO from extracted data."""
        return GarantieDTO(
            nom_garantie=extracted_data.get("nom"),
            description=extracted_data.get("description"),
            domaine=self._parse_enum(DomaineMedical, extracted_data.get("domaine")),
            statut=self._parse_enum(Statut, extracted_data.get("statut", "ACTIF")),
            taux_remboursement=extracted_data.get("tauxRemboursement"),
            type_montant=self._parse_enum(TypeMontant, extracted_data.get("typeMontant")),
            plafond_annuel=extracted_data.get("plafondAnnuel"),
            plafond_mensuel=extracted_data.get("plafondMensuel"),
            plafond_par_acte=extracted_data.get("plafondParActe"),
            franchise=extracted_data.get("franchise"),
            cout_moyen_par_sinistre=extracted_data.get("coutMoyenParSinistre"),
            duree_min_contrat=extracted_data.get("dureeMinContrat"),
            duree_max_contrat=extracted_data.get("dureeMaxContrat"),
            resiliable_annuellement=extracted_data.get("resiliableAnnuellement", True)
        )
    
    def _create_produit_dto(self, extracted_data: Dict[str, Any], prompt: str) -> ProduitDTO:
        """Create product DTO from extracted data."""
        return ProduitDTO(
            nom_produit=extracted_data.get("nom"),
            description=extracted_data.get("description"),
            type_produit=self._parse_enum(TypeProduit, extracted_data.get("typeProduit")),
            statut=self._parse_enum(Statut, extracted_data.get("statut", "ACTIF"))
        )
    
    def _create_pack_dto(self, extracted_data: Dict[str, Any], prompt: str) -> PackDTO:
        """Create pack DTO from extracted data."""
        type_clients = extracted_data.get("typeClients")
        if isinstance(type_clients, str):
            type_clients = [self._parse_enum(TypeClient, type_clients)]
        
        return PackDTO(
            nom_pack=extracted_data.get("nom"),
            description=extracted_data.get("description"),
            nom_produit=extracted_data.get("nomProduit"),
            age_minimum=extracted_data.get("ageMin"),
            age_maximum=extracted_data.get("ageMax"),
            type_clients=type_clients,
            couverture_geographique=self._parse_enum(CouvertureGeographique, extracted_data.get("couvertureGeographique")),
            prix_mensuel=extracted_data.get("prixMensuel"),
            duree_min_contrat=extracted_data.get("dureeMinContrat"),
            duree_max_contrat=extracted_data.get("dureeMaxContrat"),
            niveau_couverture=self._parse_enum(NiveauCouverture, extracted_data.get("niveauCouverture")),
            statut=self._parse_enum(Statut, extracted_data.get("statut", "ACTIF"))
        )
    
    def _parse_enum(self, enum_class, value: Any):
        """Parse string to enum value."""
        if value is None:
            return None
        if isinstance(value, enum_class):
            return value
        try:
            return enum_class(value.upper())
        except (ValueError, AttributeError):
            return None
    
    def _apply_garantie_defaults(self, garantie: GarantieDTO):
        """Apply default values to guarantee."""
        if not garantie.statut:
            garantie.statut = Statut.ACTIF
        if garantie.taux_remboursement is None:
            garantie.taux_remboursement = 0.8
        if garantie.duree_min_contrat is None:
            garantie.duree_min_contrat = 12
        if garantie.duree_max_contrat is None:
            garantie.duree_max_contrat = 36
        if garantie.resiliable_annuellement is None:
            garantie.resiliable_annuellement = True
    
    def _apply_produit_defaults(self, produit: ProduitDTO):
        """Apply default values to product."""
        if not produit.statut:
            produit.statut = Statut.ACTIF
    
    def _apply_pack_defaults(self, pack: PackDTO):
        """Apply default values to pack."""
        if not pack.statut:
            pack.statut = Statut.ACTIF
        if pack.age_minimum is None:
            pack.age_minimum = 18
        if pack.age_maximum is None:
            pack.age_maximum = 65
        if not pack.type_clients:
            pack.type_clients = [TypeClient.INDIVIDUEL]
        if pack.duree_min_contrat is None:
            pack.duree_min_contrat = 12
        if pack.duree_max_contrat is None:
            pack.duree_max_contrat = 36
        if pack.niveau_couverture is None:
            pack.niveau_couverture = NiveauCouverture.BASIC
    
    def _create_success_response(self, action: ChatbotAction, result: Dict[str, Any], prompt: str) -> ChatbotResponseDTO:
        """Create success response."""
        # Use the action from the execution result if available, otherwise use the detected action
        action_value = result.get("action", action.value)
        
        # Only populate errors field if there are actual errors (not on success)
        errors = result.get("errors") if not result.get("success") else None
        
        # Create specific success message based on action type
        success_message = result.get("message", self._get_default_success_message(action_value))
        
        return ChatbotResponseDTO(
            success=result.get("success", True),
            message=success_message,
            action=action_value,
            result=result,
            data={
                "prompt": prompt,
                "extraction_method": "AI" if self.ai_extraction_service.is_ai_available() else "FALLBACK"
            },
            entity_type=self._get_entity_type_from_action(action_value),
            errors=errors,
            warnings=result.get("warnings")
        )
    
    def _create_error_response(self, message: str, errors: list) -> ChatbotResponseDTO:
        """Create error response."""
        return ChatbotResponseDTO(
            success=False,
            message=message,
            action="ERROR",
            result={},
            data={},
            entity_type="UNKNOWN",
            errors=errors if errors else [message],
            warnings=None
        )
    
    def _get_default_success_message(self, action: str) -> str:
        """Get default success message based on action type."""
        action_messages = {
            "CREATE_GARANTIE": "Garantie créée avec succès",
            "CREATE_PRODUIT": "Produit créé avec succès",
            "CREATE_PACK": "Pack créé avec succès",
            "ADD_GARANTIE_TO_PACK": "Garantie ajoutée au pack avec succès",
            "CONFIGURATION_PACK": "Pack configuré avec succès",
            "RECOMMANDATION": "Recommandation générée avec succès"
        }
        return action_messages.get(action, "Opération réussie")
    
    def _get_entity_type_from_action(self, action: str) -> str:
        """Get entity type from action."""
        action_entity_map = {
            "CREATE_GARANTIE": "GARANTIE",
            "CREATE_PRODUIT": "PRODUIT",
            "CREATE_PACK": "PACK",
            "ADD_GARANTIE_TO_PACK": "PACK",
            "CONFIGURATION_PACK": "PACK",
            "RECOMMANDATION": "RECOMMANDATION"
        }
        return action_entity_map.get(action, "UNKNOWN")
