import logging
import re
import traceback
import uuid
from typing import Dict, Any, Optional, List
from app.models.schemas import (
    ChatbotRequestDTO, ChatbotResponseDTO, GarantieDTO, PackDTO, ProduitDTO,
    BusinessValidationResult, PackCreationResponseDTO, RecommendationRequestDTO,
    RecommendationResponseDTO, PackGarantieDTO
)
from app.models.enums import ChatbotAction, Statut, TypeMontant, DomaineMedical, TypeProduit, NiveauCouverture, CouvertureGeographique, TypeClient
from app.services.ai_extraction_service import AIExtractionService
from app.services.business_validation_service import BusinessValidationService
from app.services.recommendation_service import RecommendationService
from app.services.spring_boot_client import SpringBootClient
from app.services.prompt_analyzer_service import PromptAnalyzerService
from app.services.prompt_parser_service import PromptParserService
from app.api.analytics import analytics_store

logger = logging.getLogger(__name__)

# Alias pour les valeurs enum qui peuvent arriver sous des formes alternatives
_ENUM_ALIASES: Dict[str, str] = {
    # NiveauCouverture — STANDARD n'existe pas, on le mappe sur BASIC
    "STANDARD": "BASIC",
    "ESSENTIEL": "BASIC",
    "ESSENTIAL": "BASIC",
    # CouvertureGeographique — formes féminines/alternatives
    "NATIONALE": "NATIONAL",
    "LOCALE": "LOCAL",
    "INTERNATIONALE": "INTERNATIONAL",
    "NATIONAL": "NATIONAL",
    "LOCAL": "LOCAL",
    "INTERNATIONAL": "INTERNATIONAL",
}


class ChatbotOrchestratorService:
    """Orchestrator service that coordinates all chatbot operations."""

    def __init__(self):
        self.ai_extraction_service = AIExtractionService()
        self.business_validation_service = BusinessValidationService()
        self.spring_boot_client = SpringBootClient()
        self.prompt_analyzer_service = PromptAnalyzerService()
        self.prompt_parser_service = PromptParserService()
        self.recommendation_service = RecommendationService(self.spring_boot_client)

        logger.info("🚀 ChatbotOrchestratorService initialized")
        logger.info(f"🤖 AI Service Available: {self.ai_extraction_service.is_ai_available()}")
        logger.info(f"🔑 API Key configured: {bool(self.ai_extraction_service.api_key and self.ai_extraction_service.api_key.strip())}")
        logger.info(f"🔧 GitHub enabled: {self.ai_extraction_service.github_enabled}")
        logger.info(f"🔧 AI extraction enabled: {self.ai_extraction_service.ai_extraction_enabled}")
        logger.info(f"🔧 Fallback on error: {self.ai_extraction_service.fallback_on_error}")

    # ------------------------------------------------------------------
    # Entry point
    # ------------------------------------------------------------------

    def process_prompt(self, request: ChatbotRequestDTO) -> ChatbotResponseDTO:
        correlation_id = getattr(request, 'correlation_id', 'unknown')
        logger.info(f"[{correlation_id}] 🔵 Processing prompt: {request.prompt[:100]}...")

        analytics_store["prompts_processed"] = analytics_store.get("prompts_processed", 0) + 1

        jwt_token = getattr(request, 'jwt_token', None)
        logger.info(f"[{correlation_id}] 🔑 JWT token present: {bool(jwt_token)}")

        try:
            logger.info(f"[{correlation_id}] 🔍 Step 1: Analyzing action...")
            action = self.prompt_analyzer_service.analyze_action(request.prompt)

            if not action or action == ChatbotAction.UNKNOWN:
                logger.warning(f"[{correlation_id}] ⚠️ Action not detected or UNKNOWN")
                return self._create_error_response(
                    "Action non détectée",
                    ["Impossible de déterminer l'action à partir du prompt"]
                )

            logger.info(f"[{correlation_id}] ✅ Detected action: {action}")
            logger.info(f"[{correlation_id}] 🔧 Step 2: Executing action {action}...")
            result = self._execute_action(action, request.prompt, request.session_id, jwt_token)

            logger.info(f"[{correlation_id}] 📤 Step 3: Creating standardized response...")
            response = self._create_standardized_response(action, result, request.prompt)

            logger.info(f"[{correlation_id}] ✅ Response created - success: {response.success}")
            return response

        except Exception as e:
            logger.error(f"[{correlation_id}] ❌ Error processing prompt: {e}", exc_info=True)
            logger.error(f"[{correlation_id}] ❌ Stacktrace: {traceback.format_exc()}")
            return self._create_standardized_error_response(
                "Erreur interne",
                [f"Une erreur est survenue: {str(e)}"]
            )

    # ------------------------------------------------------------------
    # Action dispatcher
    # ------------------------------------------------------------------

    def _execute_action(self, action: ChatbotAction, prompt: str, session_id: Optional[str], jwt_token: Optional[str] = None) -> Dict[str, Any]:
        logger.info(f"Executing action: {action}")
        if action == ChatbotAction.CREATE_GARANTIE:
            return self._execute_create_garantie(prompt, jwt_token)
        elif action == ChatbotAction.CREATE_PRODUIT:
            return self._execute_create_produit(prompt, jwt_token)
        elif action == ChatbotAction.CREATE_PACK:
            return self._execute_create_pack(prompt, jwt_token)
        elif action == ChatbotAction.UPDATE_GARANTIE:
            return self._execute_update_garantie(prompt, jwt_token)
        elif action == ChatbotAction.UPDATE_PRODUIT:
            return self._execute_update_produit(prompt, jwt_token)
        elif action == ChatbotAction.UPDATE_PACK:
            return self._execute_update_pack(prompt, jwt_token)
        elif action == ChatbotAction.DELETE_GARANTIE:
            return self._execute_delete_garantie(prompt, jwt_token)
        elif action == ChatbotAction.DELETE_PRODUIT:
            return self._execute_delete_produit(prompt, jwt_token)
        elif action == ChatbotAction.DELETE_PACK:
            return self._execute_delete_pack(prompt, jwt_token)
        elif action == ChatbotAction.CONFIGURATION_PACK:
            return self._execute_configure_pack(prompt, jwt_token)
        elif action == ChatbotAction.AJOUT_GARANTIE_PACK:
            return self._execute_add_garantie_to_pack(prompt, jwt_token)
        elif action == ChatbotAction.RECOMMANDATION:
            return self._execute_recommendation(prompt, session_id, jwt_token)
        else:
            return {"success": False, "error": f"Action non implémentée: {action}"}

    # ------------------------------------------------------------------
    # CREATE GARANTIE
    # ------------------------------------------------------------------

    def _execute_create_garantie(self, prompt: str, jwt_token: Optional[str] = None) -> Dict[str, Any]:
        correlation_id = str(uuid.uuid4())
        fallback_used = False
        logger.info(f"[{correlation_id}] 🔵 START Creating guarantee from prompt: {prompt[:100]}...")

        try:
            ai_available = self.ai_extraction_service.is_ai_available()
            logger.info(f"[{correlation_id}] 🤖 AI Available: {ai_available}")

            extracted_data = None
            if ai_available:
                extracted_data = self.ai_extraction_service.extract_garantie_data(prompt)
                analytics_store["ai_calls"] = analytics_store.get("ai_calls", 0) + 1
                logger.info(f"[{correlation_id}] 🤖 AI extraction result: {extracted_data}")
            else:
                fallback_used = True

            if not extracted_data or not extracted_data.get("nom"):
                garantie_dto = self.prompt_parser_service.parse_garantie_prompt(prompt)
                if garantie_dto:
                    fallback_used = True
                    logger.info(f"[{correlation_id}] ✅ Fallback parser extracted: {garantie_dto.nom_garantie}")
                else:
                    analytics_store["ai_fallbacks"] = analytics_store.get("ai_fallbacks", 0) + 1
                    return {
                        "success": False,
                        "error": "Extraction échouée",
                        "message": "Impossible d'extraire les données de garantie. Veuillez reformuler.",
                        "missing_fields": ["nom", "domaine", "taux de remboursement"],
                        "fallback_used": fallback_used,
                        "correlation_id": correlation_id
                    }
            else:
                garantie_dto = self._create_garantie_dto(extracted_data, prompt)

            validation_result = self.business_validation_service.validate_garantie_for_scoring(garantie_dto, 1.0)
            if not validation_result.is_valid:
                return {
                    "success": False,
                    "error": "Validation échouée",
                    "details": validation_result.validation_errors,
                    "warnings": validation_result.validation_warnings,
                    "missing_fields": list(validation_result.missing_critical_fields.keys()),
                    "business_validation": self.business_validation_service.generate_validation_message(validation_result),
                    "fallback_used": fallback_used,
                    "correlation_id": correlation_id
                }

            self._apply_garantie_defaults(garantie_dto)

            logger.info(f"[{correlation_id}] 🌐 Calling Spring Boot — JWT present: {bool(jwt_token)}")
            created_garantie = self.spring_boot_client.create_garantie_sync(garantie_dto, jwt_token)
            logger.info(f"[{correlation_id}] ✅ Garantie created: {created_garantie.id_garantie}")

            analytics_store["creations_successful"] = analytics_store.get("creations_successful", 0) + 1
            return {
                "success": True,
                "action": "CREATE_GARANTIE",
                # by_alias=True → camelCase pour Angular
                "entity": created_garantie.model_dump(by_alias=True, mode='json'),
                "message": "Garantie créée avec succès",
                "id": created_garantie.id_garantie,
                "warnings": validation_result.validation_warnings,
                "business_validation": self.business_validation_service.generate_validation_message(validation_result),
                "fallback_used": fallback_used,
                "correlation_id": correlation_id
            }

        except Exception as e:
            analytics_store["creations_failed"] = analytics_store.get("creations_failed", 0) + 1
            logger.error(f"[{correlation_id}] ❌ Error: {e}", exc_info=True)
            return {
                "success": False,
                "error": f"Erreur création garantie: {str(e)}",
                "fallback_used": fallback_used,
                "correlation_id": correlation_id
            }

    # ------------------------------------------------------------------
    # CREATE PRODUIT
    # ------------------------------------------------------------------

    def _execute_create_produit(self, prompt: str, jwt_token: Optional[str] = None) -> Dict[str, Any]:
        correlation_id = str(uuid.uuid4())
        fallback_used = False
        logger.info(f"[{correlation_id}] 🔵 START Creating product: {prompt[:100]}...")

        try:
            ai_available = self.ai_extraction_service.is_ai_available()
            extracted_data = None
            if ai_available:
                extracted_data = self.ai_extraction_service.extract_produit_data(prompt)
                analytics_store["ai_calls"] = analytics_store.get("ai_calls", 0) + 1
                logger.info(f"[{correlation_id}] 🤖 AI extraction: {extracted_data}")
            else:
                fallback_used = True

            if not extracted_data or not extracted_data.get("nom"):
                produit_dto = self.prompt_parser_service.parse_produit_prompt(prompt)
                if produit_dto:
                    fallback_used = True
                else:
                    analytics_store["ai_fallbacks"] = analytics_store.get("ai_fallbacks", 0) + 1
                    return {
                        "success": False,
                        "error": "Extraction échouée",
                        "message": "Impossible d'extraire les données de produit. Veuillez reformuler.",
                        "missing_fields": ["nom", "type de produit"],
                        "fallback_used": fallback_used,
                        "correlation_id": correlation_id
                    }
            else:
                produit_dto = self._create_produit_dto(extracted_data, prompt)

            validation_result = self.business_validation_service.validate_produit_for_scoring(produit_dto, 1.0)
            if not validation_result.is_valid:
                return {
                    "success": False,
                    "error": "Validation échouée",
                    "details": validation_result.validation_errors,
                    "warnings": validation_result.validation_warnings,
                    "missing_fields": list(validation_result.missing_critical_fields.keys()),
                    "business_validation": self.business_validation_service.generate_validation_message(validation_result),
                    "fallback_used": fallback_used,
                    "correlation_id": correlation_id
                }

            self._apply_produit_defaults(produit_dto)

            logger.info(f"[{correlation_id}] 🌐 Calling Spring Boot — JWT: {bool(jwt_token)}")
            try:
                created_produit = self.spring_boot_client.create_produit_sync(produit_dto, jwt_token)
            except Exception as spring_err:
                # 400 Bad Request peut signifier doublon de nom
                if "Bad Request" in str(spring_err) or "400" in str(spring_err):
                    existing = self.spring_boot_client.get_produit_by_name_sync(produit_dto.nom_produit, jwt_token)
                    if existing:
                        return {
                            "success": False,
                            "error": f"Le produit '{produit_dto.nom_produit}' existe déjà (ID: {existing.id_produit})",
                            "message": f"Un produit avec ce nom existe déjà. Utilisez un nom différent ou mettez-le à jour.",
                            "fallback_used": fallback_used,
                            "correlation_id": correlation_id
                        }
                raise

            logger.info(f"[{correlation_id}] ✅ Produit created: {created_produit.id_produit}")
            analytics_store["creations_successful"] = analytics_store.get("creations_successful", 0) + 1
            return {
                "success": True,
                "action": "CREATE_PRODUIT",
                "entity": created_produit.model_dump(by_alias=True, mode='json'),
                "message": "Produit créé avec succès",
                "id": created_produit.id_produit,
                "warnings": validation_result.validation_warnings,
                "business_validation": self.business_validation_service.generate_validation_message(validation_result),
                "fallback_used": fallback_used,
                "correlation_id": correlation_id
            }

        except Exception as e:
            analytics_store["creations_failed"] = analytics_store.get("creations_failed", 0) + 1
            logger.error(f"[{correlation_id}] ❌ Error: {e}", exc_info=True)
            return {
                "success": False,
                "error": f"Erreur création produit: {str(e)}",
                "fallback_used": fallback_used,
                "correlation_id": correlation_id
            }

    # ------------------------------------------------------------------
    # CREATE PACK
    # ------------------------------------------------------------------

    def _execute_create_pack(self, prompt: str, jwt_token: Optional[str] = None) -> Dict[str, Any]:
        correlation_id = str(uuid.uuid4())
        fallback_used = False
        logger.info(f"[{correlation_id}] 🔵 START Creating pack: {prompt[:100]}...")

        try:
            ai_available = self.ai_extraction_service.is_ai_available()
            extracted_data = None
            if ai_available:
                extracted_data = self.ai_extraction_service.extract_pack_data(prompt)
                analytics_store["ai_calls"] = analytics_store.get("ai_calls", 0) + 1
                logger.info(f"[{correlation_id}] 🤖 AI extraction: {extracted_data}")
            else:
                fallback_used = True

            if not extracted_data or not extracted_data.get("nom"):
                pack_dto = self.prompt_parser_service.parse_pack_prompt(prompt)
                if pack_dto:
                    fallback_used = True
                    extracted_data = {}  # pas de garanties extractées en fallback
                else:
                    analytics_store["ai_fallbacks"] = analytics_store.get("ai_fallbacks", 0) + 1
                    return {
                        "success": False,
                        "error": "Extraction échouée",
                        "message": "Impossible d'extraire les données de pack. Veuillez reformuler.",
                        "missing_fields": ["nom", "prix mensuel", "produit associé"],
                        "fallback_used": fallback_used,
                        "confidence": 0.0,
                        "correlation_id": correlation_id
                    }
            else:
                pack_dto = self._create_pack_dto(extracted_data, prompt)

            # Résoudre le nom produit → ID
            if pack_dto.nom_produit and not pack_dto.produit_id:
                logger.info(f"[{correlation_id}] 🔍 Resolving product name: {pack_dto.nom_produit}")
                produit = self.spring_boot_client.get_produit_by_name_sync(pack_dto.nom_produit, jwt_token)
                if produit:
                    pack_dto.produit_id = produit.id_produit
                    logger.info(f"[{correlation_id}] ✅ Product resolved: {produit.id_produit}")
                else:
                    return {
                        "success": False,
                        "error": "Produit non trouvé",
                        "message": f"Le produit '{pack_dto.nom_produit}' n'existe pas. Créez-le d'abord.",
                        "missing_fields": ["produit_id"],
                        "fallback_used": fallback_used,
                        "confidence": 0.0,
                        "correlation_id": correlation_id
                    }

            missing_fields = self._check_missing_pack_fields(pack_dto)
            if missing_fields:
                return {
                    "success": False,
                    "error": "Champs manquants",
                    "missing_fields": missing_fields,
                    "message": f"Pour créer ce pack, il me manque: {', '.join(missing_fields)}. Pouvez-vous les préciser ?",
                    "fallback_used": fallback_used,
                    "confidence": 0.0,
                    "correlation_id": correlation_id
                }

            validation_result = self.business_validation_service.validate_pack_for_scoring(pack_dto, 1.0)
            if not validation_result.is_valid:
                return {
                    "success": False,
                    "error": "Validation échouée",
                    "details": validation_result.validation_errors,
                    "warnings": validation_result.validation_warnings,
                    "missing_fields": list(validation_result.missing_critical_fields.keys()),
                    "business_validation": self.business_validation_service.generate_validation_message(validation_result),
                    "fallback_used": fallback_used,
                    "confidence": validation_result.adjusted_score,
                    "correlation_id": correlation_id
                }

            self._apply_pack_defaults(pack_dto)

            logger.info(f"[{correlation_id}] 🌐 Calling Spring Boot — JWT: {bool(jwt_token)}")
            try:
                created_pack = self.spring_boot_client.create_pack_sync(pack_dto, jwt_token)
            except Exception as spring_err:
                if "Bad Request" in str(spring_err) or "400" in str(spring_err):
                    existing = self.spring_boot_client.get_pack_by_name_sync(pack_dto.nom_pack, jwt_token)
                    if existing:
                        return {
                            "success": False,
                            "error": f"Le pack '{pack_dto.nom_pack}' existe déjà (ID: {existing.id_pack})",
                            "message": "Un pack avec ce nom existe déjà.",
                            "fallback_used": fallback_used,
                            "confidence": 0.0,
                            "correlation_id": correlation_id
                        }
                raise

            logger.info(f"[{correlation_id}] ✅ Pack created: {created_pack.id_pack}")
            analytics_store["creations_successful"] = analytics_store.get("creations_successful", 0) + 1

            # --- Associer les garanties si présentes dans l'extraction IA ---
            garanties_data = extracted_data.get("garanties", []) if extracted_data else []
            associated_count = 0
            association_warnings = []

            if garanties_data and isinstance(garanties_data, list):
                logger.info(f"[{correlation_id}] 🔗 Associating {len(garanties_data)} garanties to pack...")
                for g_data in garanties_data:
                    nom_garantie = g_data.get("nomGarantie") or g_data.get("nom")
                    if not nom_garantie:
                        continue
                    try:
                        garantie = self.spring_boot_client.get_garantie_by_name_sync(nom_garantie, jwt_token)
                        if not garantie:
                            # Garantie inexistante — la créer automatiquement depuis les données extraites
                            logger.info(f"[{correlation_id}] 🆕 Auto-création garantie '{nom_garantie}'...")
                            auto_g = GarantieDTO(
                                nom_garantie=nom_garantie,
                                taux_remboursement=g_data.get("tauxRemboursement", 0.8),
                                type_montant=self._parse_enum(TypeMontant, g_data.get("typeMontant")),
                                plafond_annuel=g_data.get("plafond"),
                                franchise=g_data.get("franchise", 0),
                                statut=Statut.ACTIF,
                                duree_min_contrat=12,
                                duree_max_contrat=36,
                                resiliable_annuellement=True
                            )
                            self._apply_garantie_defaults(auto_g)
                            try:
                                garantie = self.spring_boot_client.create_garantie_sync(auto_g, jwt_token)
                                logger.info(f"[{correlation_id}] ✅ Garantie '{nom_garantie}' auto-créée (ID: {garantie.id_garantie})")
                            except Exception as create_err:
                                association_warnings.append(f"Impossible de créer '{nom_garantie}': {create_err}")
                                logger.warning(f"[{correlation_id}] ⚠️ Auto-création échouée pour '{nom_garantie}': {create_err}")
                                continue

                        pg = PackGarantieDTO(
                            pack_id=created_pack.id_pack,
                            garantie_id=garantie.id_garantie,
                            taux_remboursement=g_data.get("tauxRemboursement", 0.8),
                            plafond=g_data.get("plafond"),
                            franchise=g_data.get("franchise", 0),
                            delai_carence=g_data.get("delaiCarence", 0),
                            priorite=g_data.get("priorite", 1),
                            optionnelle=g_data.get("optionnelle", False),
                            supplement_prix=g_data.get("supplementPrix", 0),
                            type_montant=self._parse_enum(TypeMontant, g_data.get("typeMontant"))
                        )
                        self.spring_boot_client.add_garantie_to_pack_sync(
                            created_pack.id_pack, garantie.id_garantie, pg, jwt_token
                        )
                        associated_count += 1
                        logger.info(f"[{correlation_id}] ✅ Garantie '{nom_garantie}' associée")
                    except Exception as eg:
                        association_warnings.append(f"Impossible d'associer '{nom_garantie}': {eg}")
                        logger.warning(f"[{correlation_id}] ⚠️ Failed to associate '{nom_garantie}': {eg}")

            msg = f"Pack créé avec succès"
            if associated_count:
                msg += f" avec {associated_count} garantie(s) associée(s)"
            if association_warnings:
                msg += f" ({len(association_warnings)} avertissement(s))"

            return {
                "success": True,
                "action": "CREATE_PACK",
                "entity": created_pack.model_dump(by_alias=True, mode='json'),
                "message": msg,
                "id": created_pack.id_pack,
                "missing_fields": [],
                "fallback_used": fallback_used,
                "confidence": validation_result.adjusted_score,
                "warnings": (validation_result.validation_warnings or []) + association_warnings,
                "business_validation": self.business_validation_service.generate_validation_message(validation_result)
            }

        except Exception as e:
            analytics_store["creations_failed"] = analytics_store.get("creations_failed", 0) + 1
            logger.error(f"[{correlation_id}] ❌ Error: {e}", exc_info=True)
            return {
                "success": False,
                "error": f"Erreur création pack: {str(e)}",
                "missing_fields": [],
                "fallback_used": fallback_used,
                "confidence": 0.0,
                "correlation_id": correlation_id
            }

    # ------------------------------------------------------------------
    # CONFIGURE PACK
    # ------------------------------------------------------------------

    def _execute_configure_pack(self, prompt: str, jwt_token: Optional[str] = None) -> Dict[str, Any]:
        fallback_used = False
        try:
            if self.ai_extraction_service.is_ai_available():
                self.ai_extraction_service.extract_pack_configuration_data(prompt)
            else:
                fallback_used = True
            return {
                "success": False,
                "error": "Configuration de pack non encore implémentée",
                "fallback_used": fallback_used
            }
        except Exception as e:
            return {"success": False, "error": f"Erreur configuration pack: {str(e)}", "fallback_used": fallback_used}

    # ------------------------------------------------------------------
    # AJOUT GARANTIE → PACK
    # ------------------------------------------------------------------

    def _execute_add_garantie_to_pack(self, prompt: str, jwt_token: Optional[str] = None) -> Dict[str, Any]:
        fallback_used = False
        try:
            if self.ai_extraction_service.is_ai_available():
                extracted_data = self.ai_extraction_service.extract_add_garantie_to_pack_data(prompt)
            else:
                extracted_data = None
                fallback_used = True

            if not extracted_data or not extracted_data.get("nomPack"):
                association_data = self.prompt_parser_service.parse_pack_garantie_association(prompt)
                if association_data:
                    extracted_data = {
                        "nomPack": self._extract_entity_name(prompt, "pack"),
                        "nomGarantie": association_data.get("nom_garantie"),
                        "tauxRemboursement": association_data.get("taux_remboursement"),
                        "plafond": association_data.get("plafond"),
                        "franchise": association_data.get("franchise"),
                        "optionnelle": association_data.get("optionnelle"),
                        "priorite": association_data.get("priorite"),
                        "delaiCarence": association_data.get("delai_carence"),
                        "typeMontant": association_data.get("type_montant"),
                        "supplementPrix": association_data.get("supplement_prix")
                    }
                    fallback_used = True
                else:
                    return {
                        "success": False,
                        "error": "Extraction échouée",
                        "message": "Impossible d'extraire les données d'association.",
                        "fallback_used": fallback_used
                    }

            pack = self.spring_boot_client.get_pack_by_name_sync(extracted_data.get("nomPack", ""), jwt_token)
            garantie = self.spring_boot_client.get_garantie_by_name_sync(extracted_data.get("nomGarantie", ""), jwt_token)

            if not pack or not garantie:
                return {
                    "success": False,
                    "error": "Pack ou garantie non trouvé",
                    "details": {"pack_found": pack is not None, "garantie_found": garantie is not None},
                    "fallback_used": fallback_used
                }

            pack_garantie_dto = PackGarantieDTO(
                pack_id=pack.id_pack,
                garantie_id=garantie.id_garantie,
                taux_remboursement=extracted_data.get("tauxRemboursement", 0.8),
                plafond=extracted_data.get("plafond", 0),
                franchise=extracted_data.get("franchise", 0),
                delai_carence=extracted_data.get("delaiCarence", 0),
                priorite=extracted_data.get("priorite", 1),
                optionnelle=extracted_data.get("optionnelle", False),
                type_montant=self._parse_enum(TypeMontant, extracted_data.get("typeMontant")),
                supplement_prix=extracted_data.get("supplementPrix", 0)
            )

            self.spring_boot_client.add_garantie_to_pack_sync(
                pack.id_pack, garantie.id_garantie, pack_garantie_dto, jwt_token
            )

            return {
                "success": True,
                "action": "ADD_GARANTIE_TO_PACK",
                "message": f"Garantie ajoutée au pack {pack.nom_pack}",
                "pack_id": pack.id_pack,
                "garantie_id": garantie.id_garantie,
                "fallback_used": fallback_used
            }

        except Exception as e:
            logger.error(f"Error adding garantie to pack: {e}", exc_info=True)
            return {"success": False, "error": f"Erreur ajout garantie au pack: {str(e)}", "fallback_used": fallback_used}

    # ------------------------------------------------------------------
    # UPDATE
    # ------------------------------------------------------------------

    def _execute_update_garantie(self, prompt: str, jwt_token: Optional[str] = None) -> Dict[str, Any]:
        fallback_used = False
        try:
            if self.ai_extraction_service.is_ai_available():
                extracted_data = self.ai_extraction_service.extract_garantie_data(prompt)
            else:
                extracted_data = None
                fallback_used = True

            if not extracted_data or not extracted_data.get("nom"):
                garantie_dto = self.prompt_parser_service.parse_garantie_prompt(prompt)
                if garantie_dto:
                    extracted_data = {"nom": garantie_dto.nom_garantie}
                    fallback_used = True
                else:
                    entity_name = self._extract_entity_name(prompt, "garantie")
                    if not entity_name:
                        return {"success": False, "error": "Nom de la garantie manquant", "fallback_used": fallback_used}
                    extracted_data = {"nom": entity_name}

            entity_name = extracted_data.get("nom")
            existing = self.spring_boot_client.get_garantie_by_name_sync(entity_name, jwt_token)
            if not existing:
                return {"success": False, "error": f"Garantie '{entity_name}' non trouvée", "fallback_used": fallback_used}

            garantie_dto = self._create_garantie_dto(extracted_data, prompt)
            garantie_dto.id_garantie = existing.id_garantie
            updated = self.spring_boot_client.update_garantie_sync(existing.id_garantie, garantie_dto, jwt_token)

            return {
                "success": True,
                "action": "UPDATE_GARANTIE",
                "entity": updated.model_dump(by_alias=True, mode='json'),
                "message": f"Garantie '{entity_name}' mise à jour avec succès",
                "id": updated.id_garantie,
                "fallback_used": fallback_used
            }
        except Exception as e:
            return {"success": False, "error": f"Erreur mise à jour garantie: {str(e)}", "fallback_used": fallback_used}

    def _execute_update_produit(self, prompt: str, jwt_token: Optional[str] = None) -> Dict[str, Any]:
        fallback_used = False
        try:
            if self.ai_extraction_service.is_ai_available():
                extracted_data = self.ai_extraction_service.extract_produit_data(prompt)
            else:
                extracted_data = None
                fallback_used = True

            if not extracted_data or not extracted_data.get("nom"):
                produit_dto = self.prompt_parser_service.parse_produit_prompt(prompt)
                if produit_dto:
                    extracted_data = {"nom": produit_dto.nom_produit}
                    fallback_used = True
                else:
                    entity_name = self._extract_entity_name(prompt, "produit")
                    if not entity_name:
                        return {"success": False, "error": "Nom du produit manquant", "fallback_used": fallback_used}
                    extracted_data = {"nom": entity_name}

            entity_name = extracted_data.get("nom")
            existing = self.spring_boot_client.get_produit_by_name_sync(entity_name, jwt_token)
            if not existing:
                return {"success": False, "error": f"Produit '{entity_name}' non trouvé", "fallback_used": fallback_used}

            produit_dto = self._create_produit_dto(extracted_data, prompt)
            produit_dto.id_produit = existing.id_produit
            updated = self.spring_boot_client.update_produit_sync(existing.id_produit, produit_dto, jwt_token)

            return {
                "success": True,
                "action": "UPDATE_PRODUIT",
                "entity": updated.model_dump(by_alias=True, mode='json'),
                "message": f"Produit '{entity_name}' mis à jour avec succès",
                "id": updated.id_produit,
                "fallback_used": fallback_used
            }
        except Exception as e:
            return {"success": False, "error": f"Erreur mise à jour produit: {str(e)}", "fallback_used": fallback_used}

    def _execute_update_pack(self, prompt: str, jwt_token: Optional[str] = None) -> Dict[str, Any]:
        fallback_used = False
        try:
            if self.ai_extraction_service.is_ai_available():
                extracted_data = self.ai_extraction_service.extract_pack_data(prompt)
            else:
                extracted_data = None
                fallback_used = True

            if not extracted_data or not extracted_data.get("nom"):
                pack_dto = self.prompt_parser_service.parse_pack_prompt(prompt)
                if pack_dto:
                    extracted_data = {"nom": pack_dto.nom_pack}
                    fallback_used = True
                else:
                    entity_name = self._extract_entity_name(prompt, "pack")
                    if not entity_name:
                        return {"success": False, "error": "Nom du pack manquant", "fallback_used": fallback_used}
                    extracted_data = {"nom": entity_name}

            entity_name = extracted_data.get("nom")
            existing = self.spring_boot_client.get_pack_by_name_sync(entity_name, jwt_token)
            if not existing:
                return {"success": False, "error": f"Pack '{entity_name}' non trouvé", "fallback_used": fallback_used}

            pack_dto = self._create_pack_dto(extracted_data, prompt)
            pack_dto.id_pack = existing.id_pack
            if pack_dto.nom_produit and not pack_dto.produit_id:
                produit = self.spring_boot_client.get_produit_by_name_sync(pack_dto.nom_produit, jwt_token)
                if produit:
                    pack_dto.produit_id = produit.id_produit

            updated = self.spring_boot_client.update_pack_sync(existing.id_pack, pack_dto, jwt_token)

            return {
                "success": True,
                "action": "UPDATE_PACK",
                "entity": updated.model_dump(by_alias=True, mode='json'),
                "message": f"Pack '{entity_name}' mis à jour avec succès",
                "id": updated.id_pack,
                "fallback_used": fallback_used
            }
        except Exception as e:
            return {"success": False, "error": f"Erreur mise à jour pack: {str(e)}", "fallback_used": fallback_used}

    # ------------------------------------------------------------------
    # DELETE
    # ------------------------------------------------------------------

    def _execute_delete_garantie(self, prompt: str, jwt_token: Optional[str] = None) -> Dict[str, Any]:
        try:
            entity_name = self._extract_entity_name(prompt, "garantie")
            if not entity_name:
                return {"success": False, "error": "Nom de la garantie manquant"}
            existing = self.spring_boot_client.get_garantie_by_name_sync(entity_name, jwt_token)
            if not existing:
                return {"success": False, "error": f"Garantie '{entity_name}' non trouvée"}
            self.spring_boot_client.delete_garantie_sync(existing.id_garantie, jwt_token)
            return {"success": True, "action": "DELETE_GARANTIE", "message": f"Garantie '{entity_name}' supprimée", "id": existing.id_garantie}
        except Exception as e:
            return {"success": False, "error": f"Erreur suppression garantie: {str(e)}"}

    def _execute_delete_produit(self, prompt: str, jwt_token: Optional[str] = None) -> Dict[str, Any]:
        try:
            entity_name = self._extract_entity_name(prompt, "produit")
            if not entity_name:
                return {"success": False, "error": "Nom du produit manquant"}
            existing = self.spring_boot_client.get_produit_by_name_sync(entity_name, jwt_token)
            if not existing:
                return {"success": False, "error": f"Produit '{entity_name}' non trouvé"}
            self.spring_boot_client.delete_produit_sync(existing.id_produit, jwt_token)
            return {"success": True, "action": "DELETE_PRODUIT", "message": f"Produit '{entity_name}' supprimé", "id": existing.id_produit}
        except Exception as e:
            return {"success": False, "error": f"Erreur suppression produit: {str(e)}"}

    def _execute_delete_pack(self, prompt: str, jwt_token: Optional[str] = None) -> Dict[str, Any]:
        try:
            entity_name = self._extract_entity_name(prompt, "pack")
            if not entity_name:
                return {"success": False, "error": "Nom du pack manquant"}
            existing = self.spring_boot_client.get_pack_by_name_sync(entity_name, jwt_token)
            if not existing:
                return {"success": False, "error": f"Pack '{entity_name}' non trouvé"}
            self.spring_boot_client.delete_pack_sync(existing.id_pack, jwt_token)
            return {"success": True, "action": "DELETE_PACK", "message": f"Pack '{entity_name}' supprimé", "id": existing.id_pack}
        except Exception as e:
            return {"success": False, "error": f"Erreur suppression pack: {str(e)}"}

    # ------------------------------------------------------------------
    # RECOMMENDATION
    # ------------------------------------------------------------------

    def _execute_recommendation(self, prompt: str, session_id: Optional[str], jwt_token: Optional[str] = None) -> Dict[str, Any]:
        logger.info("=== EXECUTE RECOMMENDATION ===")
        try:
            all_packs = self.spring_boot_client.get_all_packs_sync(jwt_token)
            try:
                all_produits = self.spring_boot_client.get_all_produits_sync(jwt_token)
            except Exception:
                all_produits = []

            logger.info(f"Retrieved {len(all_packs)} packs and {len(all_produits)} produits")

            prompt_lower = prompt.lower()
            recommendations = []

            for pack in all_packs:
                if pack.statut != Statut.ACTIF:
                    continue

                score = 0
                reasons = []

                # --- Age ---
                age_match = re.search(r'(\d+)\s*ans?', prompt_lower)
                if age_match and pack.age_minimum is not None and pack.age_maximum is not None:
                    age = int(age_match.group(1))
                    if pack.age_minimum <= age <= pack.age_maximum:
                        score += 3
                        reasons.append(f"Adapté à votre âge ({age} ans)")
                elif pack.age_minimum is None or pack.age_maximum is None:
                    score += 1

                # --- Type client ---
                if 'famille' in prompt_lower or 'enfants' in prompt_lower:
                    if TypeClient.FAMILLE in (pack.type_clients or []):
                        score += 2
                        reasons.append("Convient aux familles")
                elif 'senior' in prompt_lower or 'retraité' in prompt_lower or 'retraitée' in prompt_lower:
                    if TypeClient.SENIOR in (pack.type_clients or []):
                        score += 2
                        reasons.append("Convient aux seniors")
                elif 'étudiant' in prompt_lower or 'etudiant' in prompt_lower:
                    if TypeClient.ETUDIANT in (pack.type_clients or []):
                        score += 2
                        reasons.append("Convient aux étudiants")
                elif 'entreprise' in prompt_lower or 'salarié' in prompt_lower:
                    if TypeClient.ENTREPRISE in (pack.type_clients or []):
                        score += 2
                        reasons.append("Convient aux entreprises")

                # --- Couverture médicale ---
                if 'cardiolog' in prompt_lower or 'cardio' in prompt_lower:
                    if pack.domaines_medicaux and any('cardiolog' in d.lower() or 'cardio' in d.lower() for d in pack.domaines_medicaux):
                        score += 2
                        reasons.append("Couvre la cardiologie")
                if 'hospitalisation' in prompt_lower:
                    if pack.domaines_medicaux and any('hospital' in d.lower() for d in pack.domaines_medicaux):
                        score += 2
                        reasons.append("Couvre l'hospitalisation")
                if 'rhumatolog' in prompt_lower or 'rhumatisme' in prompt_lower:
                    if pack.domaines_medicaux and any('rhumato' in d.lower() for d in pack.domaines_medicaux):
                        score += 2
                        reasons.append("Couvre la rhumatologie")
                if 'kinésithérapie' in prompt_lower or 'kiné' in prompt_lower:
                    if pack.domaines_medicaux and any('kiné' in d.lower() or 'kinesith' in d.lower() for d in pack.domaines_medicaux):
                        score += 2
                        reasons.append("Couvre la kinésithérapie")
                if 'santé' in prompt_lower or 'médical' in prompt_lower:
                    score += 1

                # --- Budget : chercher le contexte "budget de X" avant de chercher un nombre isolé ---
                budget = None
                budget_match = re.search(r'budget\s+(?:mensuel\s+)?(?:de\s+)?(\d+)', prompt_lower)
                if not budget_match:
                    budget_match = re.search(r'(\d+)\s*(?:tnd|dt|dinars?)\s*(?:par\s+mois|mensuel|/mois)?', prompt_lower)
                if budget_match:
                    budget = float(budget_match.group(1))
                    if pack.prix_mensuel and pack.prix_mensuel <= budget:
                        score += 2
                        reasons.append(f"Dans votre budget ({budget} TND/mois)")

                # --- Niveau de couverture ---
                if 'premium' in prompt_lower or 'gold' in prompt_lower:
                    if pack.niveau_couverture in [NiveauCouverture.PREMIUM, NiveauCouverture.GOLD]:
                        score += 2
                        reasons.append("Couverture premium")
                elif 'essentiel' in prompt_lower or 'basic' in prompt_lower or 'économique' in prompt_lower:
                    if pack.niveau_couverture == NiveauCouverture.BASIC:
                        score += 2
                        reasons.append("Couverture essentielle")

                if score >= 2:
                    recommendations.append({"pack": pack, "score": score, "reasons": reasons})

            recommendations.sort(key=lambda x: x["score"], reverse=True)
            top_recommendations = recommendations[:3]

            logger.info(f"Generated {len(top_recommendations)} recommendations from {len(all_packs)} packs")

            if not top_recommendations:
                return {
                    "success": True,
                    "action": "RECOMMANDATION",
                    "message": "Aucune recommandation correspondant à vos critères. Essayez de reformuler.",
                    "recommendations": [],
                    "total_available": len(all_packs),
                    "confidence": 0.5
                }

            formatted_recs = []
            for rec in top_recommendations:
                pack = rec["pack"]
                pack_dict = pack.model_dump(by_alias=True, mode='json')
                formatted_recs.append({
                    "id": pack.id_pack or "",
                    "nom": pack.nom_pack or "",
                    "description": pack.description or "",
                    "compatibilityScore": min(100.0, float(rec["score"]) * 15),
                    "monthlyPrice": pack.prix_mensuel,
                    "coverageLevel": pack.niveau_couverture.value if pack.niveau_couverture else "",
                    "whyRecommended": " | ".join(rec["reasons"]) or "Correspond à votre profil",
                    "explanation": " | ".join(rec["reasons"])
                })

            return {
                "success": True,
                "action": "RECOMMANDATION",
                "message": f"Voici {len(formatted_recs)} recommandation(s) parmi {len(all_packs)} packs disponibles",
                "recommendations": formatted_recs,
                "total_available": len(all_packs),
                "missing_fields": [],
                "confidence": 0.8
            }

        except Exception as e:
            logger.error(f"Error generating recommendation: {e}", exc_info=True)
            return {
                "success": False,
                "error": f"Erreur génération recommandation: {str(e)}",
                "message": "Impossible de générer des recommandations pour le moment",
                "confidence": 0.0
            }

    # ------------------------------------------------------------------
    # DTO builders
    # ------------------------------------------------------------------

    def _create_garantie_dto(self, extracted_data: Dict[str, Any], prompt: str) -> GarantieDTO:
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
        return ProduitDTO(
            nom_produit=extracted_data.get("nom"),
            description=extracted_data.get("description"),
            type_produit=self._parse_enum(TypeProduit, extracted_data.get("typeProduit")),
            statut=self._parse_enum(Statut, extracted_data.get("statut", "ACTIF"))
        )

    def _create_pack_dto(self, extracted_data: Dict[str, Any], prompt: str) -> PackDTO:
        type_clients = extracted_data.get("typeClients")
        if isinstance(type_clients, str):
            type_clients = [self._parse_enum(TypeClient, type_clients)]
        elif isinstance(type_clients, list):
            type_clients = [self._parse_enum(TypeClient, tc) for tc in type_clients if tc]
            type_clients = [tc for tc in type_clients if tc is not None]
        return PackDTO(
            nom_pack=extracted_data.get("nom"),
            description=extracted_data.get("description"),
            nom_produit=extracted_data.get("nomProduit"),
            age_minimum=extracted_data.get("ageMin"),
            age_maximum=extracted_data.get("ageMax"),
            type_clients=type_clients,
            anciennete_contrat_mois=extracted_data.get("ancienneteContratMois"),
            couverture_geographique=self._parse_enum(CouvertureGeographique, extracted_data.get("couvertureGeographique")),
            prix_mensuel=extracted_data.get("prixMensuel"),
            duree_min_contrat=extracted_data.get("dureeMinContrat"),
            duree_max_contrat=extracted_data.get("dureeMaxContrat"),
            niveau_couverture=self._parse_enum(NiveauCouverture, extracted_data.get("niveauCouverture")),
            statut=self._parse_enum(Statut, extracted_data.get("statut", "ACTIF"))
        )

    # ------------------------------------------------------------------
    # Enum parsing avec alias
    # ------------------------------------------------------------------

    def _parse_enum(self, enum_class, value):
        if value is None:
            return None
        if isinstance(value, enum_class):
            return value
        try:
            raw = str(value).strip().upper()
            # Résoudre les alias avant la tentative directe
            resolved = _ENUM_ALIASES.get(raw, raw)
            return enum_class(resolved)
        except (ValueError, AttributeError):
            return None

    # ------------------------------------------------------------------
    # Field validation helpers
    # ------------------------------------------------------------------

    def _check_missing_garantie_fields(self, garantie: GarantieDTO) -> List[str]:
        missing = []
        if not garantie.nom_garantie or not garantie.nom_garantie.strip():
            missing.append("nom")
        if not garantie.domaine:
            missing.append("domaine")
        if garantie.taux_remboursement is None:
            missing.append("taux de remboursement")
        return missing

    def _check_missing_produit_fields(self, produit: ProduitDTO) -> List[str]:
        missing = []
        if not produit.nom_produit or not produit.nom_produit.strip():
            missing.append("nom")
        if not produit.type_produit:
            missing.append("type de produit")
        return missing

    def _check_missing_pack_fields(self, pack: PackDTO) -> List[str]:
        missing = []
        if not pack.nom_pack or not pack.nom_pack.strip():
            missing.append("nom")
        if pack.prix_mensuel is None or pack.prix_mensuel <= 0:
            missing.append("prix mensuel")
        return missing

    # ------------------------------------------------------------------
    # Default values
    # ------------------------------------------------------------------

    def _apply_garantie_defaults(self, garantie: GarantieDTO):
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
        if not produit.statut:
            produit.statut = Statut.ACTIF

    def _apply_pack_defaults(self, pack: PackDTO):
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

    # ------------------------------------------------------------------
    # Response builders
    # ------------------------------------------------------------------

    def _create_standardized_response(self, action: ChatbotAction, result: Dict[str, Any], prompt: str) -> ChatbotResponseDTO:
        action_value = result.get("action", action.value)
        success = result.get("success", True)
        refresh_targets = self._get_refresh_targets(action_value)

        created_entity = None
        if success and result.get("id"):
            entity = result.get("entity", {}) or {}
            name = (entity.get("nomPack") or entity.get("nomProduit") or entity.get("nomGarantie")
                    or entity.get("nom_pack") or entity.get("nom_produit") or entity.get("nom_garantie"))
            created_entity = {
                "type": self._get_entity_type_from_action(action_value),
                "id": result.get("id"),
                "name": name
            }

        errors_list = None
        if not success:
            errors_list = result.get("errors")
            if not errors_list and result.get("error"):
                errors_list = [result.get("error")]
            elif not errors_list and result.get("details"):
                details = result.get("details")
                errors_list = details if isinstance(details, list) else [str(details)]

        # Pour les recommandations, mettre les recs dans data
        data = None
        if success:
            if action_value == "RECOMMANDATION":
                data = {"recommendations": result.get("recommendations", [])}
            elif result.get("entity"):
                data = result.get("entity")

        return ChatbotResponseDTO(
            success=success,
            intent=action_value,
            message=result.get("message", self._get_default_success_message(action_value) if success else result.get("error", "Erreur")),
            data=data,
            created_entity=created_entity,
            refresh_targets=refresh_targets,
            errors=errors_list,
            warnings=result.get("warnings"),
            missing_fields=result.get("missing_fields"),
            confidence=result.get("confidence", 1.0 if success else 0.0),
            fallback_used=result.get("fallback_used", False),
            action=action_value,
            result=result,
            entity_type=self._get_entity_type_from_action(action_value)
        )

    def _create_standardized_error_response(self, message: str, errors: list) -> ChatbotResponseDTO:
        return ChatbotResponseDTO(
            success=False,
            intent="ERROR",
            message=message,
            data=None,
            created_entity=None,
            refresh_targets=[],
            errors=errors if errors else [message],
            warnings=None,
            missing_fields=None,
            confidence=0.0,
            fallback_used=False,
            action="ERROR",
            result={},
            entity_type="UNKNOWN"
        )

    def _create_error_response(self, message: str, errors: list) -> ChatbotResponseDTO:
        return self._create_standardized_error_response(message, errors)

    def _get_refresh_targets(self, action: str) -> List[str]:
        refresh_map = {
            "CREATE_GARANTIE": ["garanties", "packs"],
            "CREATE_PRODUIT": ["produits", "packs"],
            "CREATE_PACK": ["packs", "produits"],
            "UPDATE_GARANTIE": ["garanties", "packs"],
            "UPDATE_PRODUIT": ["produits", "packs"],
            "UPDATE_PACK": ["packs", "produits"],
            "DELETE_GARANTIE": ["garanties", "packs"],
            "DELETE_PRODUIT": ["produits", "packs"],
            "DELETE_PACK": ["packs", "produits"],
            "ADD_GARANTIE_TO_PACK": ["packs", "garanties"],
            "CONFIGURATION_PACK": ["packs"],
            "RECOMMANDATION": []
        }
        return refresh_map.get(action, [])

    def _get_default_success_message(self, action: str) -> str:
        return {
            "CREATE_GARANTIE": "Garantie créée avec succès",
            "CREATE_PRODUIT": "Produit créé avec succès",
            "CREATE_PACK": "Pack créé avec succès",
            "ADD_GARANTIE_TO_PACK": "Garantie ajoutée au pack avec succès",
            "CONFIGURATION_PACK": "Pack configuré avec succès",
            "RECOMMANDATION": "Recommandation générée avec succès"
        }.get(action, "Opération réussie")

    def _get_entity_type_from_action(self, action: str) -> str:
        return {
            "CREATE_GARANTIE": "GARANTIE",
            "CREATE_PRODUIT": "PRODUIT",
            "CREATE_PACK": "PACK",
            "UPDATE_GARANTIE": "GARANTIE",
            "UPDATE_PRODUIT": "PRODUIT",
            "UPDATE_PACK": "PACK",
            "DELETE_GARANTIE": "GARANTIE",
            "DELETE_PRODUIT": "PRODUIT",
            "DELETE_PACK": "PACK",
            "ADD_GARANTIE_TO_PACK": "PACK",
            "CONFIGURATION_PACK": "PACK",
            "RECOMMANDATION": "RECOMMANDATION"
        }.get(action, "UNKNOWN")

    # ------------------------------------------------------------------
    # Helpers
    # ------------------------------------------------------------------

    def _extract_entity_name(self, prompt: str, entity_type: str) -> Optional[str]:
        patterns = [
            rf'{entity_type}\s+(?:nommé|appelé|appelée)?\s+([A-Z][a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\s]+?)(?:\s+avec|\s+et|,|\.$|$)',
            rf'(?:supprimer|modifier|mettre à jour)\s+(?:le|la|l\'|un|une)\s+{entity_type}\s+([A-Z][a-zA-ZàâäéèêëïîôöùûüÿçÀÂÄÉÈÊËÏÎÔÖÙÛÜŸÇ\s]+?)(?:\s+avec|\s+et|,|\.$|$)'
        ]
        for pattern in patterns:
            match = re.search(pattern, prompt, re.IGNORECASE)
            if match:
                return match.group(1).strip()
        return None
