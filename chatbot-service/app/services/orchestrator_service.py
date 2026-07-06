import logging
import re
import time
import traceback
import uuid
from typing import Dict, Any, Optional, List
from app.models.schemas import (
    ChatbotRequestDTO, ChatbotResponseDTO, GarantieDTO, PackDTO, ProduitDTO,
    BusinessValidationResult, PackCreationResponseDTO, RecommendationRequestDTO,
    RecommendationResponseDTO, PackGarantieDTO,
    PlafondGarantieDTO, FranchiseGarantieDTO
)
from app.models.enums import (
    ChatbotAction, TypeMontant, DomaineMedical, TypeProduit, NiveauCouverture,
    TypeRemboursement, TypeFranchise
)
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
}

# Question posée à l'utilisateur pour chaque champ manquant possible.
_FIELD_QUESTIONS: Dict[str, str] = {
    "nom": "Quel nom souhaitez-vous lui donner ?",
    "domaine": "Quel est le domaine médical concerné (ex: HOSPITALISATION, DENTAIRE, OPTIQUE, CARDIOLOGIE) ?",
    "taux de remboursement": "Quel taux de remboursement souhaitez-vous, en % (ex: 80) ?",
    "prix mensuel": "Quel est le prix mensuel, en TND ?",
    "type de produit": "Quel type de produit (SANTE, AUTO, HABITATION, VIE, EPARGNE) ?",
    "produit associé": "À quel produit doit-il être associé ?",
    "âge": "Quel âge avez-vous ?",
    "sexe": "Êtes-vous un homme ou une femme ?",
    "situation familiale": "Quelle est votre situation familiale (célibataire, marié(e), divorcé(e), veuf/veuve) ?",
}

# Champ manquant (libellé humain) → clé du dict extracted_data correspondante.
_FIELD_TO_EXTRACTED_KEY: Dict[str, str] = {
    "nom": "nom",
    "domaine": "domaine",
    "taux de remboursement": "tauxRemboursement",
    "prix mensuel": "prixMensuel",
    "type de produit": "typeProduit",
    "produit associé": "nomProduit",
    "âge": "age",
    "sexe": "gender",
    "situation familiale": "maritalStatus",
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
        # Sessions en attente de complétion (slot-filling) : session_id -> état de la demande.
        # Stockage en mémoire — acceptable pour un PFE, à externaliser (Redis) si multi-instance.
        self.pending_sessions: Dict[str, Dict[str, Any]] = {}

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
        session_id = getattr(request, 'session_id', None)
        logger.info(f"[{correlation_id}] 🔑 JWT token present: {bool(jwt_token)}")

        try:
            if session_id and session_id in self.pending_sessions:
                logger.info(f"[{correlation_id}] ↩️ Session {session_id} en attente d'un champ — traitement comme réponse")
                return self._resume_pending_session(session_id, request.prompt, jwt_token)

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
            return self._execute_create_garantie(prompt, jwt_token, session_id=session_id)
        elif action == ChatbotAction.CREATE_PRODUIT:
            return self._execute_create_produit(prompt, jwt_token, session_id=session_id)
        elif action == ChatbotAction.CREATE_PACK:
            return self._execute_create_pack(prompt, jwt_token, session_id=session_id)
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
    # Slot-filling — complétion conversationnelle des champs manquants
    # ------------------------------------------------------------------

    def _demander_champ_manquant(self, session_id: Optional[str], action: ChatbotAction,
                                  extracted_data: Dict[str, Any], missing_fields: List[str],
                                  original_prompt: str, entity_label: str, fallback_used: bool,
                                  correlation_id: str, contexte: Optional[str] = None) -> Dict[str, Any]:
        """Si une session est fournie, mémorise l'état et pose une question ciblée sur le
        premier champ manquant au lieu de rejeter la demande. Sans session (appel direct/tests),
        conserve l'ancien comportement de rejet sec."""
        champ = missing_fields[0]

        if not session_id:
            return {
                "success": False,
                "error": "Champs manquants",
                "missing_fields": missing_fields,
                "message": f"Pour créer {entity_label}, il me manque : {', '.join(missing_fields)}.",
                "fallback_used": fallback_used,
                "confidence": 0.0,
                "correlation_id": correlation_id
            }

        self.pending_sessions[session_id] = {
            "action": action,
            "extracted_data": extracted_data,
            "missing_field": champ,
            "original_prompt": original_prompt,
            "entity_label": entity_label,
        }

        question = _FIELD_QUESTIONS.get(champ, f"Pouvez-vous préciser : {champ} ?")
        message = f"{contexte} {question}" if contexte else (
            f"Pour créer {entity_label}, il me manque {champ}. {question}"
        )

        return {
            "success": False,
            "needs_input": True,
            "missing_fields": missing_fields,
            "message": message,
            "fallback_used": fallback_used,
            "confidence": 0.0,
            "correlation_id": correlation_id
        }

    def _extract_answer_for_field(self, field_label: str, answer_text: str) -> Any:
        """Extrait une valeur exploitable depuis la réponse en langage naturel de l'utilisateur,
        selon le type de champ attendu."""
        text = answer_text.strip()

        if field_label in ("nom", "produit associé"):
            return text.strip(' ."\'')

        if field_label == "domaine":
            resolved = self._parse_enum(DomaineMedical, text)
            if resolved:
                return resolved.value
            fallback = self.prompt_parser_service._extract_domaine_medical(text)
            return fallback.value if fallback else text

        if field_label == "type de produit":
            resolved = self._parse_enum(TypeProduit, text)
            if resolved:
                return resolved.value
            fallback = self.prompt_parser_service._extract_type_produit(text)
            return fallback.value if fallback else text

        if field_label in ("taux de remboursement", "prix mensuel"):
            match = re.search(r'(\d+(?:[.,]\d+)?)', text)
            return float(match.group(1).replace(',', '.')) if match else None

        if field_label == "âge":
            match = re.search(r'(\d+)', text)
            return int(match.group(1)) if match else None

        if field_label == "sexe":
            t = text.lower()
            if 'femme' in t or 'féminin' in t or 'feminin' in t or t.strip(' ."\'') == 'f':
                return "femme"
            if 'homme' in t or 'masculin' in t or t.strip(' ."\'') == 'm':
                return "homme"
            return text

        if field_label == "situation familiale":
            return text.strip(' ."\'')

        return text

    def _resume_pending_session(self, session_id: str, answer_text: str,
                                 jwt_token: Optional[str] = None) -> ChatbotResponseDTO:
        """Traite la réponse de l'utilisateur à une question de complétion posée précédemment,
        complète les données extraites, puis relance l'action d'origine."""
        pending = self.pending_sessions.pop(session_id)
        champ = pending["missing_field"]
        extracted_key = _FIELD_TO_EXTRACTED_KEY.get(champ, champ)

        valeur = self._extract_answer_for_field(champ, answer_text)
        extracted_data = dict(pending["extracted_data"])
        extracted_data[extracted_key] = valeur

        action = pending["action"]
        original_prompt = pending["original_prompt"]

        if action == ChatbotAction.CREATE_GARANTIE:
            result = self._execute_create_garantie(original_prompt, jwt_token, session_id=session_id,
                                                     preset_extracted_data=extracted_data)
        elif action == ChatbotAction.CREATE_PRODUIT:
            result = self._execute_create_produit(original_prompt, jwt_token, session_id=session_id,
                                                    preset_extracted_data=extracted_data)
        elif action == ChatbotAction.CREATE_PACK:
            result = self._execute_create_pack(original_prompt, jwt_token, session_id=session_id,
                                                preset_extracted_data=extracted_data)
        elif action == ChatbotAction.RECOMMANDATION:
            result = self._execute_recommendation(original_prompt, session_id, jwt_token,
                                                   preset_extracted_data=extracted_data)
        else:
            result = {"success": False, "error": "Session de complétion invalide ou expirée."}

        return self._create_standardized_response(action, result, original_prompt)

    # ------------------------------------------------------------------
    # CREATE GARANTIE
    # ------------------------------------------------------------------

    def _execute_create_garantie(self, prompt: str, jwt_token: Optional[str] = None,
                                  session_id: Optional[str] = None,
                                  preset_extracted_data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        correlation_id = str(uuid.uuid4())
        fallback_used = False
        logger.info(f"[{correlation_id}] 🔵 START Creating guarantee from prompt: {prompt[:100]}...")

        try:
            if preset_extracted_data is not None:
                extracted_data = preset_extracted_data
            else:
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
                    fallback_dto = self.prompt_parser_service.parse_garantie_prompt(prompt)
                    if fallback_dto:
                        fallback_used = True
                        logger.info(f"[{correlation_id}] ✅ Fallback parser extracted: {fallback_dto.nom_garantie}")
                        extracted_data = {
                            "nom": fallback_dto.nom_garantie,
                            "domaine": fallback_dto.domaine.value if fallback_dto.domaine else None,
                            "tauxRemboursement": fallback_dto.taux_remboursement_base,
                            "plafondAnnuel": fallback_dto.plafond.plafond_annuel if fallback_dto.plafond else None,
                            "plafondMensuel": fallback_dto.plafond.plafond_mensuel if fallback_dto.plafond else None,
                            "plafondParActe": fallback_dto.plafond.plafond_par_acte if fallback_dto.plafond else None,
                            "franchise": fallback_dto.franchise.montant_fixe if fallback_dto.franchise else None,
                        }
                    else:
                        analytics_store["ai_fallbacks"] = analytics_store.get("ai_fallbacks", 0) + 1
                        extracted_data = {}

            garantie_dto = self._create_garantie_dto(extracted_data, prompt)
            missing_fields = self._check_missing_garantie_fields(garantie_dto)
            if missing_fields:
                return self._demander_champ_manquant(
                    session_id, ChatbotAction.CREATE_GARANTIE, extracted_data, missing_fields,
                    prompt, "cette garantie", fallback_used, correlation_id
                )

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

    def _execute_create_produit(self, prompt: str, jwt_token: Optional[str] = None,
                                 session_id: Optional[str] = None,
                                 preset_extracted_data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        correlation_id = str(uuid.uuid4())
        fallback_used = False
        logger.info(f"[{correlation_id}] 🔵 START Creating product: {prompt[:100]}...")

        try:
            if preset_extracted_data is not None:
                extracted_data = preset_extracted_data
            else:
                ai_available = self.ai_extraction_service.is_ai_available()
                extracted_data = None
                if ai_available:
                    extracted_data = self.ai_extraction_service.extract_produit_data(prompt)
                    analytics_store["ai_calls"] = analytics_store.get("ai_calls", 0) + 1
                    logger.info(f"[{correlation_id}] 🤖 AI extraction: {extracted_data}")
                else:
                    fallback_used = True

                if not extracted_data or not extracted_data.get("nom"):
                    fallback_dto = self.prompt_parser_service.parse_produit_prompt(prompt)
                    if fallback_dto:
                        fallback_used = True
                        extracted_data = {
                            "nom": fallback_dto.nom_produit,
                            "description": fallback_dto.description,
                            "typeProduit": fallback_dto.type_produit.value if fallback_dto.type_produit else None,
                        }
                    else:
                        analytics_store["ai_fallbacks"] = analytics_store.get("ai_fallbacks", 0) + 1
                        extracted_data = {}

            produit_dto = self._create_produit_dto(extracted_data, prompt)
            missing_fields = self._check_missing_produit_fields(produit_dto)
            if missing_fields:
                return self._demander_champ_manquant(
                    session_id, ChatbotAction.CREATE_PRODUIT, extracted_data, missing_fields,
                    prompt, "ce produit", fallback_used, correlation_id
                )

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

    def _execute_create_pack(self, prompt: str, jwt_token: Optional[str] = None,
                              session_id: Optional[str] = None,
                              preset_extracted_data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        correlation_id = str(uuid.uuid4())
        fallback_used = False
        logger.info(f"[{correlation_id}] 🔵 START Creating pack: {prompt[:100]}...")

        try:
            if preset_extracted_data is not None:
                extracted_data = preset_extracted_data
            else:
                ai_available = self.ai_extraction_service.is_ai_available()
                extracted_data = None
                if ai_available:
                    extracted_data = self.ai_extraction_service.extract_pack_data(prompt)
                    analytics_store["ai_calls"] = analytics_store.get("ai_calls", 0) + 1
                    logger.info(f"[{correlation_id}] 🤖 AI extraction: {extracted_data}")
                else:
                    fallback_used = True

                if not extracted_data or not extracted_data.get("nom"):
                    fallback_dto = self.prompt_parser_service.parse_pack_prompt(prompt)
                    if fallback_dto:
                        fallback_used = True
                        extracted_data = {
                            "nom": fallback_dto.nom_pack,
                            "description": fallback_dto.description,
                            "nomProduit": fallback_dto.nom_produit,
                            "prixMensuel": fallback_dto.prix_mensuel,
                            "niveauCouverture": fallback_dto.niveau_couverture.value if fallback_dto.niveau_couverture else None,
                        }  # pas de garanties extraites en fallback
                    else:
                        analytics_store["ai_fallbacks"] = analytics_store.get("ai_fallbacks", 0) + 1
                        extracted_data = {}

            pack_dto = self._create_pack_dto(extracted_data, prompt)

            # Résoudre le nom produit → ID
            if pack_dto.nom_produit and not pack_dto.produit_id:
                logger.info(f"[{correlation_id}] 🔍 Resolving product name: {pack_dto.nom_produit}")
                produit = self.spring_boot_client.get_produit_by_name_sync(pack_dto.nom_produit, jwt_token)
                if produit:
                    pack_dto.produit_id = produit.id_produit
                    logger.info(f"[{correlation_id}] ✅ Product resolved: {produit.id_produit}")
                else:
                    return self._demander_champ_manquant(
                        session_id, ChatbotAction.CREATE_PACK, extracted_data, ["produit associé"],
                        prompt, "ce pack", fallback_used, correlation_id,
                        contexte=f"Le produit '{pack_dto.nom_produit}' n'existe pas. "
                                 f"Précisez le nom d'un produit existant, ou créez-le d'abord."
                    )

            missing_fields = self._check_missing_pack_fields(pack_dto)
            if missing_fields:
                return self._demander_champ_manquant(
                    session_id, ChatbotAction.CREATE_PACK, extracted_data, missing_fields,
                    prompt, "ce pack", fallback_used, correlation_id
                )

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
                                domaine=self._parse_enum(DomaineMedical, g_data.get("domaine")) or DomaineMedical.AUTRE,
                                type_remboursement=self._parse_enum(TypeRemboursement, g_data.get("typeMontant")) or TypeRemboursement.FRAIS_REELS,
                                taux_remboursement_base=self._normaliser_taux(g_data.get("tauxRemboursement", 0.8)),
                                plafond=PlafondGarantieDTO(plafond_annuel=g_data.get("plafond")) if g_data.get("plafond") else None,
                                franchise=FranchiseGarantieDTO(type=TypeFranchise.FIXE, montant_fixe=g_data.get("franchise"))
                                    if g_data.get("franchise") else None,
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
                            nom_garantie=garantie.nom_garantie,
                            code_garantie=garantie.code_garantie,
                            taux_remboursement_specifique=self._normaliser_taux(g_data.get("tauxRemboursement")),
                            plafond_specifique=PlafondGarantieDTO(plafond_annuel=g_data.get("plafond")) if g_data.get("plafond") else None,
                            franchise_specifique=FranchiseGarantieDTO(type=TypeFranchise.FIXE, montant_fixe=g_data.get("franchise"))
                                if g_data.get("franchise") else None,
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
        """Reconfigure une association pack-garantie déjà existante (taux/plafond/franchise
        spécifiques au pack) — distinct de AJOUT_GARANTIE_PACK qui crée une nouvelle association."""
        fallback_used = False
        try:
            extracted_data = None
            if self.ai_extraction_service.is_ai_available():
                extracted_data = self.ai_extraction_service.extract_pack_configuration_data(prompt)
            else:
                fallback_used = True

            if not extracted_data or not extracted_data.get("nomPack"):
                association_data = self.prompt_parser_service.parse_pack_garantie_association(prompt)
                nom_pack = self._extract_entity_name(prompt, "pack")
                if association_data and nom_pack:
                    extracted_data = {
                        "nomPack": nom_pack,
                        "nomGarantie": association_data.get("nom_garantie"),
                        "tauxRemboursement": association_data.get("taux_remboursement"),
                        "plafond": association_data.get("plafond"),
                        "franchise": association_data.get("franchise"),
                        "optionnelle": association_data.get("optionnelle"),
                        "supplementPrix": association_data.get("supplement_prix"),
                    }
                    fallback_used = True
                else:
                    return {
                        "success": False,
                        "error": "Extraction échouée",
                        "message": "Impossible d'extraire les données de configuration. Précisez le pack et la garantie concernés.",
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

            associations = self.spring_boot_client.get_pack_garanties_sync(pack.id_pack, jwt_token)
            association = next((a for a in associations if a.garantie_id == garantie.id_garantie), None)
            if not association:
                return {
                    "success": False,
                    "error": "Association introuvable",
                    "message": f"La garantie '{garantie.nom_garantie}' n'est pas encore associée au pack '{pack.nom_pack}'. "
                               f"Utilisez plutôt une demande d'ajout.",
                    "fallback_used": fallback_used
                }

            if extracted_data.get("tauxRemboursement") is not None:
                association.taux_remboursement_specifique = self._normaliser_taux(extracted_data.get("tauxRemboursement"))
            if extracted_data.get("plafond") is not None:
                association.plafond_specifique = PlafondGarantieDTO(plafond_annuel=extracted_data.get("plafond"))
            if extracted_data.get("franchise") is not None:
                association.franchise_specifique = FranchiseGarantieDTO(type=TypeFranchise.FIXE, montant_fixe=extracted_data.get("franchise"))
            if extracted_data.get("optionnelle") is not None:
                association.optionnelle = extracted_data.get("optionnelle")
            if extracted_data.get("supplementPrix") is not None:
                association.supplement_prix = extracted_data.get("supplementPrix")

            updated = self.spring_boot_client.update_pack_garantie_sync(association.id_pack_garantie, association, jwt_token)

            return {
                "success": True,
                "action": "CONFIGURATION_PACK",
                "entity": updated.model_dump(by_alias=True, mode='json'),
                "message": f"Configuration de '{garantie.nom_garantie}' dans le pack '{pack.nom_pack}' mise à jour",
                "id": updated.id_pack_garantie,
                "fallback_used": fallback_used
            }
        except Exception as e:
            logger.error(f"Error configuring pack: {e}", exc_info=True)
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
                nom_garantie=garantie.nom_garantie,
                code_garantie=garantie.code_garantie,
                taux_remboursement_specifique=self._normaliser_taux(extracted_data.get("tauxRemboursement")),
                plafond_specifique=PlafondGarantieDTO(plafond_annuel=extracted_data.get("plafond")) if extracted_data.get("plafond") else None,
                franchise_specifique=FranchiseGarantieDTO(type=TypeFranchise.FIXE, montant_fixe=extracted_data.get("franchise"))
                    if extracted_data.get("franchise") else None,
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

    def _execute_recommendation(self, prompt: str, session_id: Optional[str], jwt_token: Optional[str] = None,
                                 preset_extracted_data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Délègue au moteur de recommandation pondéré (RecommendationService — éligibilité,
        besoins médicaux, budget, type de client, profil), le même que celui utilisé par
        l'endpoint structuré /api/chatbot/recommendations. Complète le profil par dialogue
        (slot-filling) si l'âge, le sexe ou la situation familiale ne sont pas dans le prompt."""
        correlation_id = str(uuid.uuid4())
        fallback_used = not self.ai_extraction_service.is_ai_available()
        logger.info(f"[{correlation_id}] 🔵 START Recommendation from prompt: {prompt[:100]}...")

        try:
            if preset_extracted_data is not None:
                extracted_data = preset_extracted_data
            else:
                extracted_data = self.ai_extraction_service.extract_recommendation_profile_data(prompt)
                analytics_store["ai_calls"] = analytics_store.get("ai_calls", 0) + 1

            missing_fields = self._check_missing_recommendation_fields(extracted_data)
            if missing_fields:
                return self._demander_champ_manquant(
                    session_id, ChatbotAction.RECOMMANDATION, extracted_data, missing_fields,
                    prompt, "votre profil de recommandation", fallback_used, correlation_id
                )

            request = RecommendationRequestDTO(
                session_id=session_id or correlation_id,
                age=int(extracted_data["age"]),
                gender=extracted_data["gender"],
                marital_status=extracted_data["maritalStatus"],
                number_of_children=extracted_data.get("numberOfChildren"),
                monthly_budget=extracted_data.get("monthlyBudget"),
                smoker=extracted_data.get("smoker"),
                medical_needs=extracted_data.get("medicalNeeds"),
                geographical_zone=extracted_data.get("geographicalZone"),
                profession=extracted_data.get("profession"),
            )

            recommendation_response = self.recommendation_service.generate_recommendations(request, jwt_token)

            formatted_recs = [{
                "id": rec.id,
                "nom": rec.nom,
                "description": rec.description or "",
                "compatibilityScore": min(100.0, round(rec.compatibility_score * 100, 1)),
                "monthlyPrice": rec.monthly_price,
                "coverageLevel": rec.coverage_level,
                "whyRecommended": rec.why_recommended,
                "explanation": rec.detailed_explanation or rec.why_recommended,
            } for rec in recommendation_response.recommended_packs]

            logger.info(f"[{correlation_id}] Generated {len(formatted_recs)} recommendations")

            return {
                "success": recommendation_response.success,
                "action": "RECOMMANDATION",
                "message": recommendation_response.message,
                "recommendations": formatted_recs,
                "explanation": recommendation_response.explanation,
                "missing_fields": [],
                "confidence": 0.9 if formatted_recs else 0.5,
                "fallback_used": fallback_used,
                "correlation_id": correlation_id
            }

        except Exception as e:
            logger.error(f"[{correlation_id}] Error generating recommendation: {e}", exc_info=True)
            return {
                "success": False,
                "error": f"Erreur génération recommandation: {str(e)}",
                "message": "Impossible de générer des recommandations pour le moment",
                "confidence": 0.0,
                "correlation_id": correlation_id
            }

    # ------------------------------------------------------------------
    # DTO builders
    # ------------------------------------------------------------------

    @staticmethod
    def _normaliser_taux(valeur: Optional[float]) -> Optional[float]:
        """Le pipeline d'extraction produit historiquement une fraction (0-1).
        Le backend attend un pourcentage (0-100). On convertit uniquement
        les valeurs déjà exprimées en fraction pour rester tolérant aux deux formats."""
        if valeur is None:
            return None
        return valeur * 100 if 0 < valeur <= 1 else valeur

    @staticmethod
    def _generate_code(prefix: str, nom: Optional[str]) -> str:
        """Génère un code métier unique (ex: GAR-HOSPITALISATION-482913) à partir du nom saisi."""
        slug = re.sub(r'[^A-Z0-9]+', '-', (nom or "ENTITE").upper()).strip('-')[:24] or "ENTITE"
        suffix = str(int(time.time() * 1000))[-6:]
        return f"{prefix}-{slug}-{suffix}"

    def _create_garantie_dto(self, extracted_data: Dict[str, Any], prompt: str) -> GarantieDTO:
        plafond = PlafondGarantieDTO(
            plafond_annuel=extracted_data.get("plafondAnnuel"),
            plafond_mensuel=extracted_data.get("plafondMensuel"),
            plafond_par_acte=extracted_data.get("plafondParActe"),
        )
        franchise_montant = extracted_data.get("franchise")
        franchise = FranchiseGarantieDTO(
            type=TypeFranchise.FIXE if franchise_montant else TypeFranchise.AUCUNE,
            montant_fixe=franchise_montant,
        ) if franchise_montant is not None else None

        return GarantieDTO(
            nom_garantie=extracted_data.get("nom"),
            description=extracted_data.get("description"),
            domaine=self._parse_enum(DomaineMedical, extracted_data.get("domaine")),
            type_remboursement=self._parse_enum(TypeRemboursement, extracted_data.get("typeMontant")) or TypeRemboursement.FRAIS_REELS,
            taux_remboursement_base=self._normaliser_taux(extracted_data.get("tauxRemboursement")),
            plafond=plafond if any([plafond.plafond_annuel, plafond.plafond_mensuel, plafond.plafond_par_acte]) else None,
            franchise=franchise,
        )

    def _create_produit_dto(self, extracted_data: Dict[str, Any], prompt: str) -> ProduitDTO:
        return ProduitDTO(
            nom_produit=extracted_data.get("nom"),
            description=extracted_data.get("description"),
            type_produit=self._parse_enum(TypeProduit, extracted_data.get("typeProduit")),
        )

    def _create_pack_dto(self, extracted_data: Dict[str, Any], prompt: str) -> PackDTO:
        return PackDTO(
            nom_pack=extracted_data.get("nom"),
            description=extracted_data.get("description"),
            nom_produit=extracted_data.get("nomProduit"),
            prix_mensuel=extracted_data.get("prixMensuel"),
            niveau_couverture=self._parse_enum(NiveauCouverture, extracted_data.get("niveauCouverture")),
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
        if garantie.taux_remboursement_base is None:
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

    def _check_missing_recommendation_fields(self, extracted_data: Dict[str, Any]) -> List[str]:
        """Champs requis par RecommendationRequestDTO (age, gender, marital_status) —
        les autres (budget, besoins, sexe étant optionnels côté DTO mais nécessaires au
        scoring pondéré) restent optionnels et sont traités comme neutres si absents."""
        missing = []
        if extracted_data.get("age") is None:
            missing.append("âge")
        if not extracted_data.get("gender"):
            missing.append("sexe")
        if not extracted_data.get("maritalStatus"):
            missing.append("situation familiale")
        return missing

    # ------------------------------------------------------------------
    # Default values
    # ------------------------------------------------------------------

    def _apply_garantie_defaults(self, garantie: GarantieDTO):
        if garantie.taux_remboursement_base is None:
            garantie.taux_remboursement_base = 80.0
        if not garantie.code_garantie:
            garantie.code_garantie = self._generate_code("GAR", garantie.nom_garantie)

    def _apply_produit_defaults(self, produit: ProduitDTO):
        if not produit.code_produit:
            produit.code_produit = self._generate_code("PRD", produit.nom_produit)

    def _apply_pack_defaults(self, pack: PackDTO):
        if pack.niveau_couverture is None:
            pack.niveau_couverture = NiveauCouverture.BASIC
        if not pack.code_pack:
            pack.code_pack = self._generate_code("PACK", pack.nom_pack)

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
