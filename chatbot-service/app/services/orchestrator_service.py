import logging
import re
import time
import traceback
import uuid
from datetime import datetime, timezone
from typing import Dict, Any, Optional, List
from app.models.schemas import (
    ChatbotRequestDTO, ChatbotResponseDTO, GarantieDTO, PackDTO, ProduitDTO,
    BusinessValidationResult, PackCreationResponseDTO, RecommendationRequestDTO,
    RecommendationResponseDTO, PackGarantieDTO,
    PlafondGarantieDTO,
    # FranchiseGarantieDTO,  # TODO: réactiver après correction du calcul de franchise
)
from app.models.enums import (
    ChatbotAction, TypeMontant, DomaineMedical, TypeProduit, NiveauCouverture,
    TypeRemboursement, StatutWorkflow, CouvertureGeographique, TypePlafond
    # TypeFranchise,  # TODO: réactiver après correction du calcul de franchise
)
from app.services.ai_extraction_service import AIExtractionService
from app.services.business_validation_service import BusinessValidationService
from app.services.recommendation_service import RecommendationService
from app.services.spring_boot_client import SpringBootClient
from app.services.prompt_analyzer_service import PromptAnalyzerService
from app.services.prompt_parser_service import PromptParserService
from app.services.action_log_service import action_log_service
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

        logger.info("ChatbotOrchestratorService initialized")
        logger.info(f"AI Service Available: {self.ai_extraction_service.is_ai_available()}")
        logger.info(f"API Key configured: {bool(self.ai_extraction_service.api_key and self.ai_extraction_service.api_key.strip())}")
        logger.info(f"GitHub enabled: {self.ai_extraction_service.github_enabled}")
        logger.info(f"AI extraction enabled: {self.ai_extraction_service.ai_extraction_enabled}")
        logger.info(f"Fallback on error: {self.ai_extraction_service.fallback_on_error}")

    # ------------------------------------------------------------------
    # Entry point
    # ------------------------------------------------------------------

    def process_prompt(self, request: ChatbotRequestDTO) -> ChatbotResponseDTO:
        correlation_id = getattr(request, 'correlation_id', 'unknown')
        logger.info(f"[{correlation_id}] Processing prompt: {request.prompt[:100]}...")

        analytics_store["prompts_processed"] = analytics_store.get("prompts_processed", 0) + 1

        jwt_token = getattr(request, 'jwt_token', None)
        session_id = getattr(request, 'session_id', None)
        logger.info(f"[{correlation_id}] JWT token present: {bool(jwt_token)}")

        try:
            if session_id and session_id in self.pending_sessions:
                logger.info(f"[{correlation_id}] Session {session_id} en attente d'un champ — traitement comme réponse")
                return self._resume_pending_session(session_id, request.prompt, jwt_token)

            logger.info(f"[{correlation_id}] Step 1: Analyzing action...")
            action = self.prompt_analyzer_service.analyze_action(request.prompt)

            if not action or action == ChatbotAction.UNKNOWN:
                logger.warning(f"[{correlation_id}] Action not detected or UNKNOWN")
                return self._create_error_response(
                    "Action non détectée",
                    ["Impossible de déterminer l'action à partir du prompt"]
                )

            logger.info(f"[{correlation_id}] Detected action: {action}")
            logger.info(f"[{correlation_id}] Step 2: Executing action {action}...")
            result = self._execute_action(action, request.prompt, request.session_id, jwt_token)

            self._log_action_if_undoable(action, result, session_id)

            logger.info(f"[{correlation_id}] Step 3: Creating standardized response...")
            response = self._create_standardized_response(action, result, request.prompt)

            logger.info(f"[{correlation_id}] Response created - success: {response.success}")
            return response

        except Exception as e:
            logger.error(f"[{correlation_id}] Error processing prompt: {e}", exc_info=True)
            logger.error(f"[{correlation_id}] Stacktrace: {traceback.format_exc()}")
            return self._create_standardized_error_response(
                "Erreur interne",
                [f"Une erreur est survenue: {str(e)}"]
            )

    # Actions CREATE/UPDATE journalisées pour l'undo (voir action_log_service.UNDOABLE_ACTION_TYPES
    # pour le choix de périmètre — DELETE et les actions d'association de pack sont exclues).
    _UNDO_LOGGABLE_ACTIONS = {
        ChatbotAction.CREATE_GARANTIE: ("CREATE", "GARANTIE"),
        ChatbotAction.CREATE_PRODUIT: ("CREATE", "PRODUIT"),
        ChatbotAction.CREATE_PACK: ("CREATE", "PACK"),
        ChatbotAction.UPDATE_GARANTIE: ("UPDATE", "GARANTIE"),
        ChatbotAction.UPDATE_PRODUIT: ("UPDATE", "PRODUIT"),
        ChatbotAction.UPDATE_PACK: ("UPDATE", "PACK"),
    }

    def _log_action_if_undoable(self, action: ChatbotAction, result: Dict[str, Any],
                                 session_id: Optional[str]) -> None:
        """Enregistre l'action dans le journal d'annulation si elle a réussi, si une
        session est active et si son type fait partie du périmètre undo décidé pour Lot B."""
        if not session_id or not result.get("success"):
            return
        loggable = self._UNDO_LOGGABLE_ACTIONS.get(action)
        if not loggable:
            return
        action_type, entity_type = loggable
        entity_id = result.get("id")
        after_state = result.get("entity")
        if not entity_id or not after_state:
            return
        entity_name = (
            after_state.get("nomGarantie") or after_state.get("nomProduit")
            or after_state.get("nomPack") or "?"
        )
        try:
            action_log_service.log_action(
                session_id=session_id,
                action_type=action_type,
                entity_type=entity_type,
                entity_id=entity_id,
                entity_name=entity_name,
                before_state=result.get("before_state"),
                after_state=after_state,
            )
        except Exception as e:
            # Le journal undo est une fonctionnalité de confort : son indisponibilité
            # (ex. Mongo down) ne doit jamais faire échouer l'action elle-même.
            logger.warning(f"Failed to log action for undo (session {session_id}): {e}")

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
            return self._execute_configure_pack(prompt, jwt_token, session_id=session_id)
        elif action == ChatbotAction.AJOUT_GARANTIE_PACK:
            return self._execute_add_garantie_to_pack(prompt, jwt_token, session_id=session_id)
        elif action == ChatbotAction.RECOMMANDATION:
            return self._execute_recommendation(prompt, session_id, jwt_token)
        elif action == ChatbotAction.UNDO:
            return self._execute_undo(session_id, jwt_token)
        elif action == ChatbotAction.GENERAL:
            return self._execute_general(prompt)
        else:
            return {"success": False, "error": f"Action non implémentée: {action}"}

    def _execute_general(self, prompt: str) -> Dict[str, Any]:
        """Salutation, remerciement ou question sur les capacités de l'assistant : jamais
        une erreur — toujours un rappel des fonctionnalités disponibles."""
        return {
            "success": True,
            "action": "GENERAL",
            "message": (
                "Bonjour ! Je suis l'assistant IA de configuration des produits d'assurance. "
                "Je peux vous aider à : créer des garanties, gérer des packs, configurer des "
                "produits, et générer des recommandations personnalisées à partir d'un profil "
                "client. Décrivez simplement ce que vous souhaitez faire, en langage naturel."
            ),
        }

    # ------------------------------------------------------------------
    # Slot-filling — complétion conversationnelle des champs manquants
    # ------------------------------------------------------------------

    def _demander_champ_manquant(self, session_id: Optional[str], action: ChatbotAction,
                                  extracted_data: Dict[str, Any], missing_fields: List[str],
                                  original_prompt: str, entity_label: str, fallback_used: bool,
                                  correlation_id: str, contexte: Optional[str] = None,
                                  error_override: Optional[str] = None) -> Dict[str, Any]:
        """Si une session est fournie, mémorise l'état et pose une question ciblée sur le
        premier champ manquant au lieu de rejeter la demande. Sans session (appel direct/tests),
        conserve l'ancien comportement de rejet sec.

        `contexte`/`error_override` permettent de distinguer un champ réellement absent d'une
        valeur fournie mais invalide (ex: domaine médical inconnu de l'énumération) — sans cette
        distinction, l'utilisateur reçoit "il me manque : domaine" alors qu'il en avait précisé un,
        simplement mal orthographié ou hors énumération."""
        champ = missing_fields[0]

        if not session_id:
            message = contexte if contexte else f"Pour créer {entity_label}, il me manque : {', '.join(missing_fields)}."
            return {
                "success": False,
                "error": error_override or "Champs manquants",
                "missing_fields": missing_fields,
                "message": message,
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

        # Cas particulier : résolution d'une garantie introuvable (choix structuré
        # créer/remplacer), distinct du slot-filling générique à champ unique ci-dessous.
        if "resolution_garantie" in pending:
            result = self._resume_resolution_garantie(session_id, answer_text, jwt_token, pending)
            return self._create_standardized_response(ChatbotAction.CONFIGURATION_PACK, result, "")

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
        elif action == ChatbotAction.CONFIGURATION_PACK:
            result = self._execute_configure_pack(original_prompt, jwt_token, session_id=session_id,
                                                    preset_extracted_data=extracted_data)
        elif action == ChatbotAction.AJOUT_GARANTIE_PACK:
            result = self._execute_add_garantie_to_pack(original_prompt, jwt_token, session_id=session_id,
                                                          preset_extracted_data=extracted_data)
        else:
            result = {"success": False, "error": "Session de complétion invalide ou expirée."}

        self._log_action_if_undoable(action, result, session_id)

        return self._create_standardized_response(action, result, original_prompt)

    # ------------------------------------------------------------------
    # Résolution d'une garantie introuvable (créer maintenant / remplacer)
    # ------------------------------------------------------------------

    def _build_pack_garantie_dto(self, pack_id: str, garantie, garantie_data: Dict[str, Any]) -> PackGarantieDTO:
        """Construit l'association pack-garantie en reprenant les réglages (taux/plafond/
        optionnelle/supplément) précisés par l'utilisateur pour cette garantie, si présents."""
        return PackGarantieDTO(
            pack_id=pack_id,
            garantie_id=garantie.id_garantie,
            nom_garantie=garantie.nom_garantie,
            code_garantie=garantie.code_garantie,
            taux_remboursement_specifique=self._normaliser_taux(garantie_data.get("tauxRemboursement")),
            plafond_specifique=PlafondGarantieDTO(plafond_annuel=garantie_data.get("plafond")) if garantie_data.get("plafond") else None,
            optionnelle=garantie_data.get("optionnelle", False),
            supplement_prix=garantie_data.get("supplementPrix", 0),
            type_montant=self._parse_enum(TypeMontant, garantie_data.get("typeMontant")),
        )

    _MOTS_VIDES_RECHERCHE = {
        "de", "du", "des", "et", "la", "le", "les", "un", "une", "au", "aux",
        "en", "avec", "pour", "sur", "sans", "dans", "par",
    }

    def _mot_cle_recherche_garantie(self, nom_garantie: str) -> str:
        """get_garantie_by_name et search_garanties interrogent le même endpoint
        (recherche par sous-chaîne) : si le nom complet n'a trouvé aucune correspondance,
        une nouvelle recherche avec ce même nom complet n'en trouvera jamais non plus.
        On isole donc le mot le plus significatif du nom pour obtenir de vraies suggestions
        de garanties proches, plutôt qu'une liste de remplacement systématiquement vide."""
        mots = [m for m in re.findall(r"\w+", nom_garantie, re.UNICODE)
                if m.lower() not in self._MOTS_VIDES_RECHERCHE]
        return max(mots, key=len) if mots else nom_garantie

    def _demander_resolution_garantie(self, session_id: Optional[str], pack_id: str,
                                       garantie_data: Dict[str, Any], remaining_garanties: List[Dict[str, Any]],
                                       correlation_id: str, jwt_token: Optional[str] = None) -> Dict[str, Any]:
        """Pose un choix structuré (plutôt qu'une simple question en texte libre) quand une
        garantie nommée n'existe pas dans le catalogue : remplacer par une garantie existante
        (suggestions par recherche partielle) ou la créer maintenant. Remplace l'ancien
        comportement qui créait la garantie silencieusement, sans confirmation.

        `garantie_data` porte au moins nomGarantie/nom, et éventuellement les réglages
        d'association (tauxRemboursement/plafond/optionnelle/supplementPrix/typeMontant) déjà
        précisés par l'utilisateur — conservés pour être appliqués une fois la garantie résolue."""
        nom_garantie = garantie_data.get("nomGarantie") or garantie_data.get("nom")
        try:
            mot_cle = self._mot_cle_recherche_garantie(nom_garantie)
            suggestions = self.spring_boot_client.search_garanties_sync(mot_cle, jwt_token) or []
        except Exception as e:
            logger.warning(f"[{correlation_id}] Recherche de suggestions échouée pour '{nom_garantie}': {e}")
            suggestions = []

        message = (f"La garantie « {nom_garantie} » n'existe pas dans le catalogue. "
                    f"Voulez-vous en choisir une autre, ou créer « {nom_garantie} » maintenant ?")
        choices = [
            {"id": "creer", "label": f"Créer « {nom_garantie} » maintenant"},
            {
                "id": "remplacer",
                "label": "Choisir une garantie existante",
                "options": [{"id": g.id_garantie, "label": g.nom_garantie} for g in suggestions[:5]]
            }
        ]

        if session_id:
            self.pending_sessions[session_id] = {
                "resolution_garantie": {
                    "pack_id": pack_id,
                    "garantie_data": garantie_data,
                    "remaining_garanties": remaining_garanties,
                }
            }

        return {
            "success": False,
            "needs_input": True,
            "message": message,
            "choices": choices,
            "confidence": 0.0,
            "correlation_id": correlation_id
        }

    def _resume_resolution_garantie(self, session_id: str, answer_code: str,
                                     jwt_token: Optional[str], pending: Dict[str, Any]) -> Dict[str, Any]:
        """Traite le choix de l'utilisateur (créer / remplacer:<id>) pour la garantie en
        attente, associe la garantie résolue au pack, puis poursuit avec les éventuelles
        garanties restantes non encore résolues avant de conclure."""
        etat = pending["resolution_garantie"]
        pack_id = etat["pack_id"]
        garantie_data = etat["garantie_data"]
        nom_garantie = garantie_data.get("nomGarantie") or garantie_data.get("nom")
        remaining = list(etat.get("remaining_garanties") or [])
        code = (answer_code or "").strip()

        garantie = None
        try:
            if code == "creer":
                nouvelle = GarantieDTO(
                    nom_garantie=nom_garantie,
                    domaine=self._parse_enum(DomaineMedical, garantie_data.get("domaine")) or DomaineMedical.AUTRE,
                    statut_workflow=StatutWorkflow.PUBLIE,
                    type_remboursement=self._parse_enum(TypeRemboursement, garantie_data.get("typeMontant")) or TypeRemboursement.FRAIS_REELS,
                )
                self._apply_garantie_defaults(nouvelle)
                garantie = self.spring_boot_client.create_garantie_sync(nouvelle, jwt_token)
                logger.info(f"Garantie '{nom_garantie}' créée sur confirmation explicite de l'utilisateur (ID: {garantie.id_garantie})")
            elif code.startswith("remplacer:"):
                garantie_id = code.split(":", 1)[1]
                # Les suggestions ont été construites via _mot_cle_recherche_garantie (le nom
                # complet ne matche jamais rien, sinon la résolution n'aurait pas été déclenchée) —
                # on doit rechercher avec ce même mot-clé pour retrouver l'id choisi par l'utilisateur.
                mot_cle = self._mot_cle_recherche_garantie(nom_garantie)
                candidats = self.spring_boot_client.search_garanties_sync(mot_cle, jwt_token) or []
                garantie = next((g for g in candidats if g.id_garantie == garantie_id), None)
            else:
                return {"success": False, "error": "Choix non reconnu : merci de cliquer sur une des options proposées."}

            if not garantie:
                return {"success": False, "error": f"Impossible de résoudre la garantie « {nom_garantie} »."}

            pg = self._build_pack_garantie_dto(pack_id, garantie, garantie_data)
            self.spring_boot_client.add_garantie_to_pack_sync(pack_id, garantie.id_garantie, pg, jwt_token)
        except Exception as e:
            return {"success": False, "error": f"Échec de l'association de la garantie « {nom_garantie} » : {e}"}

        # S'il reste d'autres garanties non résolues pour ce pack, poser la question suivante
        # au lieu de conclure — un seul champ manquant est traité par échange, comme pour le
        # slot-filling générique.
        for i, g_data in enumerate(remaining):
            nom_suivant = g_data.get("nomGarantie") or g_data.get("nom")
            if not nom_suivant:
                continue
            existe = self.spring_boot_client.get_garantie_by_name_sync(nom_suivant, jwt_token)
            if existe:
                try:
                    pg_suivant = self._build_pack_garantie_dto(pack_id, existe, g_data)
                    self.spring_boot_client.add_garantie_to_pack_sync(pack_id, existe.id_garantie, pg_suivant, jwt_token)
                except Exception:
                    pass
                continue
            return self._demander_resolution_garantie(
                session_id, pack_id, g_data, remaining[i + 1:], str(uuid.uuid4()), jwt_token
            )

        return {
            "success": True,
            "action": "CONFIGURATION_PACK",
            "entity": {"nomGarantie": garantie.nom_garantie},
            "message": f"Garantie « {garantie.nom_garantie} » associée au pack avec succès.",
            "id": pack_id,
        }

    # ------------------------------------------------------------------
    # CREATE GARANTIE
    # ------------------------------------------------------------------

    def _execute_create_garantie(self, prompt: str, jwt_token: Optional[str] = None,
                                  session_id: Optional[str] = None,
                                  preset_extracted_data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        correlation_id = str(uuid.uuid4())
        fallback_used = False
        logger.info(f"[{correlation_id}] START Creating guarantee from prompt: {prompt[:100]}...")

        try:
            if preset_extracted_data is not None:
                extracted_data = preset_extracted_data
            else:
                ai_available = self.ai_extraction_service.is_ai_available()
                logger.info(f"[{correlation_id}] AI Available: {ai_available}")

                extracted_data = None
                if ai_available:
                    extracted_data = self.ai_extraction_service.extract_garantie_data(prompt)
                    analytics_store["ai_calls"] = analytics_store.get("ai_calls", 0) + 1
                    logger.info(f"[{correlation_id}] AI extraction result: {extracted_data}")
                else:
                    fallback_used = True

                if not extracted_data or not extracted_data.get("nom"):
                    fallback_dto = self.prompt_parser_service.parse_garantie_prompt(prompt)
                    if fallback_dto:
                        fallback_used = True
                        logger.info(f"[{correlation_id}] Fallback parser extracted: {fallback_dto.nom_garantie}")
                        extracted_data = {
                            "nom": fallback_dto.nom_garantie,
                            "domaine": fallback_dto.domaine.value if fallback_dto.domaine else None,
                            "typeMontant": fallback_dto.type_remboursement.value if fallback_dto.type_remboursement else None,
                            "tauxRemboursement": fallback_dto.taux_remboursement_base,
                            "plafondAnnuel": fallback_dto.plafond.plafond_annuel if fallback_dto.plafond else None,
                            "plafondMensuel": fallback_dto.plafond.plafond_mensuel if fallback_dto.plafond else None,
                            "plafondParActe": fallback_dto.plafond.plafond_par_acte if fallback_dto.plafond else None,
                            # TODO: réactiver après correction du calcul de franchise
                            # "franchise": fallback_dto.franchise.montant_fixe if fallback_dto.franchise else None,
                        }
                    else:
                        analytics_store["ai_fallbacks"] = analytics_store.get("ai_fallbacks", 0) + 1
                        extracted_data = {}

            garantie_dto = self._create_garantie_dto(extracted_data, prompt)
            missing_fields = self._check_missing_garantie_fields(garantie_dto, extracted_data)
            if missing_fields:
                if "domaine_invalide" in missing_fields:
                    valeurs = ", ".join(d.value for d in DomaineMedical)
                    return self._demander_champ_manquant(
                        session_id, ChatbotAction.CREATE_GARANTIE, extracted_data,
                        ["domaine" if f == "domaine_invalide" else f for f in missing_fields],
                        prompt, "cette garantie", fallback_used, correlation_id,
                        contexte=f"Le domaine \"{extracted_data.get('domaine')}\" n'est pas une valeur reconnue. "
                                 f"Domaines valides : {valeurs}.",
                        error_override="Valeur de domaine invalide"
                    )
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

            logger.info(f"[{correlation_id}] Calling Spring Boot — JWT present: {bool(jwt_token)}")
            created_garantie = self.spring_boot_client.create_garantie_sync(garantie_dto, jwt_token)
            logger.info(f"[{correlation_id}] Garantie created: {created_garantie.id_garantie}")

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
            logger.error(f"[{correlation_id}] Error: {e}", exc_info=True)
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
        logger.info(f"[{correlation_id}] START Creating product: {prompt[:100]}...")

        try:
            if preset_extracted_data is not None:
                extracted_data = preset_extracted_data
            else:
                ai_available = self.ai_extraction_service.is_ai_available()
                extracted_data = None
                if ai_available:
                    extracted_data = self.ai_extraction_service.extract_produit_data(prompt)
                    analytics_store["ai_calls"] = analytics_store.get("ai_calls", 0) + 1
                    logger.info(f"[{correlation_id}] AI extraction: {extracted_data}")
                else:
                    fallback_used = True

                if not extracted_data or not extracted_data.get("nom"):
                    fallback_dto = self.prompt_parser_service.parse_produit_prompt(prompt)
                    if fallback_dto:
                        fallback_used = True
                        extracted_data = {
                            "nom": fallback_dto.nom_produit,
                            "nomCommercial": fallback_dto.nom_commercial,
                            "description": fallback_dto.description,
                            "typeProduit": fallback_dto.type_produit.value if fallback_dto.type_produit else None,
                            "statutWorkflow": fallback_dto.statut_workflow.value if fallback_dto.statut_workflow else None,
                            "prixBase": fallback_dto.prix_base,
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

            logger.info(f"[{correlation_id}] Calling Spring Boot — JWT: {bool(jwt_token)}")
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

            logger.info(f"[{correlation_id}] Produit created: {created_produit.id_produit}")
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
            logger.error(f"[{correlation_id}] Error: {e}", exc_info=True)
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
        logger.info(f"[{correlation_id}] START Creating pack: {prompt[:100]}...")

        try:
            if preset_extracted_data is not None:
                extracted_data = preset_extracted_data
            else:
                ai_available = self.ai_extraction_service.is_ai_available()
                extracted_data = None
                if ai_available:
                    extracted_data = self.ai_extraction_service.extract_pack_data(prompt)
                    analytics_store["ai_calls"] = analytics_store.get("ai_calls", 0) + 1
                    logger.info(f"[{correlation_id}] AI extraction: {extracted_data}")
                else:
                    fallback_used = True

                if not extracted_data or not extracted_data.get("nom"):
                    fallback_dto = self.prompt_parser_service.parse_pack_prompt(prompt)
                    if fallback_dto:
                        fallback_used = True
                        extracted_data = {
                            "nom": fallback_dto.nom_pack,
                            "nomCommercial": fallback_dto.nom_commercial,
                            "description": fallback_dto.description,
                            "nomProduit": fallback_dto.nom_produit,
                            "prixMensuel": fallback_dto.prix_mensuel,
                            "niveauCouverture": fallback_dto.niveau_couverture.value if fallback_dto.niveau_couverture else None,
                            "statutWorkflow": fallback_dto.statut_workflow.value if fallback_dto.statut_workflow else None,
                            "packRecommande": fallback_dto.pack_recommande,
                            "optionsDisponibles": fallback_dto.options_disponibles,
                        }  # pas de garanties extraites en fallback
                    else:
                        analytics_store["ai_fallbacks"] = analytics_store.get("ai_fallbacks", 0) + 1
                        extracted_data = {}

            pack_dto = self._create_pack_dto(extracted_data, prompt)

            # Résoudre le nom produit → ID, ou l'auto-créer s'il n'existe pas encore. Ceci permet
            # de créer Produit + Pack + Garanties en une seule requête (hiérarchie respectée :
            # le produit doit exister avant le pack) sans interrompre le flux par un dialogue.
            if pack_dto.nom_produit and not pack_dto.produit_id:
                logger.info(f"[{correlation_id}] Resolving product name: {pack_dto.nom_produit}")
                produit = self.spring_boot_client.get_produit_by_name_sync(pack_dto.nom_produit, jwt_token)
                if produit:
                    pack_dto.produit_id = produit.id_produit
                    logger.info(f"[{correlation_id}] Product resolved: {produit.id_produit}")
                else:
                    logger.info(f"[{correlation_id}] Product '{pack_dto.nom_produit}' not found — auto-creating from prompt")
                    produit_extracted = {}
                    if self.ai_extraction_service.is_ai_available():
                        produit_extracted = self.ai_extraction_service.extract_produit_data(prompt) or {}
                    auto_produit_dto = self._create_produit_dto(produit_extracted, prompt)
                    auto_produit_dto.nom_produit = auto_produit_dto.nom_produit or pack_dto.nom_produit
                    if not auto_produit_dto.type_produit:
                        # Type requis côté métier ; ce projet couvre presque exclusivement la
                        # santé, donc c'est la valeur par défaut la plus probable si l'IA ne
                        # l'a pas déduite du prompt.
                        auto_produit_dto.type_produit = TypeProduit.SANTE
                    self._apply_produit_defaults(auto_produit_dto)
                    try:
                        created_produit = self.spring_boot_client.create_produit_sync(auto_produit_dto, jwt_token)
                        pack_dto.produit_id = created_produit.id_produit
                        logger.info(f"[{correlation_id}] Product '{auto_produit_dto.nom_produit}' auto-created (ID: {created_produit.id_produit})")
                    except Exception as produit_err:
                        logger.warning(f"[{correlation_id}] Auto-creation of product '{pack_dto.nom_produit}' failed: {produit_err}")
                        return self._demander_champ_manquant(
                            session_id, ChatbotAction.CREATE_PACK, extracted_data, ["produit associé"],
                            prompt, "ce pack", fallback_used, correlation_id,
                            contexte=f"Le produit '{pack_dto.nom_produit}' n'existe pas et n'a pas pu être créé "
                                     f"automatiquement. Précisez le nom d'un produit existant, ou créez-le d'abord."
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

            logger.info(f"[{correlation_id}] Calling Spring Boot — JWT: {bool(jwt_token)}")
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

            logger.info(f"[{correlation_id}] Pack created: {created_pack.id_pack}")
            analytics_store["creations_successful"] = analytics_store.get("creations_successful", 0) + 1

            # --- Associer les garanties si présentes dans l'extraction IA ---
            garanties_data = extracted_data.get("garanties", []) if extracted_data else []
            associated_count = 0
            association_warnings = []

            if garanties_data and isinstance(garanties_data, list):
                logger.info(f"[{correlation_id}] Associating {len(garanties_data)} garanties to pack...")
                for i, g_data in enumerate(garanties_data):
                    nom_garantie = g_data.get("nomGarantie") or g_data.get("nom")
                    if not nom_garantie:
                        continue
                    try:
                        garantie = self.spring_boot_client.get_garantie_by_name_sync(nom_garantie, jwt_token)
                        if not garantie:
                            # Garantie inexistante — plus de création silencieuse (règle métier A1) :
                            # on interrompt ici et on demande une résolution explicite (créer
                            # maintenant / choisir une garantie existante) ; les garanties déjà
                            # associées ci-dessus le restent, celles non encore traitées sont
                            # reprises après résolution via _resume_resolution_garantie.
                            logger.info(f"[{correlation_id}] Garantie '{nom_garantie}' introuvable — demande de résolution")
                            return self._demander_resolution_garantie(
                                session_id, created_pack.id_pack, g_data,
                                garanties_data[i + 1:], correlation_id, jwt_token
                            )

                        pg = self._build_pack_garantie_dto(created_pack.id_pack, garantie, g_data)
                        self.spring_boot_client.add_garantie_to_pack_sync(
                            created_pack.id_pack, garantie.id_garantie, pg, jwt_token
                        )
                        associated_count += 1
                        logger.info(f"[{correlation_id}] Garantie '{nom_garantie}' associée")
                    except Exception as eg:
                        association_warnings.append(f"Impossible d'associer '{nom_garantie}': {eg}")
                        logger.warning(f"[{correlation_id}] Failed to associate '{nom_garantie}': {eg}")

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
            logger.error(f"[{correlation_id}] Error: {e}", exc_info=True)
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

    def _execute_configure_pack(self, prompt: str, jwt_token: Optional[str] = None,
                                 session_id: Optional[str] = None,
                                 preset_extracted_data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        """Reconfigure une association pack-garantie déjà existante (taux/plafond/franchise
        spécifiques au pack) — distinct de AJOUT_GARANTIE_PACK qui crée une nouvelle association."""
        fallback_used = False
        try:
            if preset_extracted_data is not None:
                extracted_data = preset_extracted_data
            else:
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
                            # TODO: réactiver après correction du calcul de franchise
                            # "franchise": association_data.get("franchise"),
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
            if not pack:
                return {
                    "success": False,
                    "error": "Pack non trouvé",
                    "message": f"Le pack '{extracted_data.get('nomPack', '')}' n'existe pas.",
                    "fallback_used": fallback_used
                }

            garantie = self.spring_boot_client.get_garantie_by_name_sync(extracted_data.get("nomGarantie", ""), jwt_token)
            if not garantie:
                return self._demander_resolution_garantie(
                    session_id, pack.id_pack, extracted_data, [], str(uuid.uuid4()), jwt_token
                )

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
            # TODO: réactiver après correction du calcul de franchise
            # if extracted_data.get("franchise") is not None:
            #     association.franchise_specifique = FranchiseGarantieDTO(type=TypeFranchise.FIXE, montant_fixe=extracted_data.get("franchise"))
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

    def _execute_add_garantie_to_pack(self, prompt: str, jwt_token: Optional[str] = None,
                                       session_id: Optional[str] = None,
                                       preset_extracted_data: Optional[Dict[str, Any]] = None) -> Dict[str, Any]:
        fallback_used = False
        try:
            if preset_extracted_data is not None:
                extracted_data = preset_extracted_data
            else:
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
                            # TODO: réactiver après correction du calcul de franchise
                            # "franchise": association_data.get("franchise"),
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
            if not pack:
                return {
                    "success": False,
                    "error": "Pack non trouvé",
                    "message": f"Le pack '{extracted_data.get('nomPack', '')}' n'existe pas.",
                    "fallback_used": fallback_used
                }

            garantie = self.spring_boot_client.get_garantie_by_name_sync(extracted_data.get("nomGarantie", ""), jwt_token)
            if not garantie:
                return self._demander_resolution_garantie(
                    session_id, pack.id_pack, extracted_data, [], str(uuid.uuid4()), jwt_token
                )

            pack_garantie_dto = self._build_pack_garantie_dto(pack.id_pack, garantie, extracted_data)

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
                "before_state": existing.model_dump(by_alias=True, mode='json'),
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
                "before_state": existing.model_dump(by_alias=True, mode='json'),
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
                "before_state": existing.model_dump(by_alias=True, mode='json'),
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
        logger.info(f"[{correlation_id}] START Recommendation from prompt: {prompt[:100]}...")

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

            def _format_rec(rec):
                return {
                    "id": rec.id,
                    "nom": rec.nom,
                    "description": rec.description or "",
                    "compatibilityScore": min(100.0, round(rec.compatibility_score * 100, 1)),
                    "monthlyPrice": rec.monthly_price,
                    "coverageLevel": rec.coverage_level,
                    "whyRecommended": rec.why_recommended,
                    "explanation": rec.detailed_explanation or rec.why_recommended,
                }

            formatted_recs = [_format_rec(rec) for rec in recommendation_response.recommended_packs]
            formatted_products = [_format_rec(rec) for rec in recommendation_response.recommended_products]

            logger.info(f"[{correlation_id}] Generated {len(formatted_recs)} pack recommendations, {len(formatted_products)} product recommendations")

            return {
                "success": recommendation_response.success,
                "action": "RECOMMANDATION",
                "message": recommendation_response.message,
                "recommendations": formatted_recs,
                "recommended_products": formatted_products,
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
    def _normaliser_couleur(valeur: Optional[str]) -> Optional[str]:
        """Le backend attend un code hexadécimal préfixé par '#' (ex: '#1b5e20').
        L'IA renvoie parfois la couleur sans le préfixe (ex: '1b5e20')."""
        if not valeur:
            return None
        valeur = valeur.strip()
        return valeur if valeur.startswith("#") else f"#{valeur}"

    @staticmethod
    def _generate_code(prefix: str, nom: Optional[str]) -> str:
        """Génère un code métier unique (ex: GAR-HOSPITALISATION-482913) à partir du nom saisi."""
        slug = re.sub(r'[^A-Z0-9]+', '-', (nom or "ENTITE").upper()).strip('-')[:24] or "ENTITE"
        suffix = str(int(time.time() * 1000))[-6:]
        return f"{prefix}-{slug}-{suffix}"

    @staticmethod
    def _as_number(value: Any) -> Optional[float]:
        """Coerce a value extracted by l'IA en nombre, en rejetant silencieusement les objets/listes
        (l'IA renvoie parfois un sous-objet imbriqué malgré la consigne de champs à plat)."""
        if value is None or isinstance(value, (dict, list)):
            return None
        try:
            return float(value)
        except (TypeError, ValueError):
            return None

    def _create_garantie_dto(self, extracted_data: Dict[str, Any], prompt: str) -> GarantieDTO:
        # Défense : si l'IA a malgré tout renvoyé "plafond"/"franchise" comme un objet imbriqué
        # (ex: {"type": "FIXE", "montant": 5}) plutôt que les champs à plat demandés, on récupère
        # les sous-valeurs utiles au lieu de laisser passer le dict brut vers Pydantic (qui rejette
        # un dict là où un float est attendu — c'était la cause de "Input should be a valid number").
        plafond_obj = extracted_data.get("plafond")
        plafond_obj = plafond_obj if isinstance(plafond_obj, dict) else {}
        plafond_annuel = self._as_number(extracted_data.get("plafondAnnuel")) or self._as_number(plafond_obj.get("annuel") or plafond_obj.get("plafondAnnuel"))
        plafond_mensuel = self._as_number(extracted_data.get("plafondMensuel")) or self._as_number(plafond_obj.get("mensuel") or plafond_obj.get("plafondMensuel"))
        plafond_par_acte = self._as_number(extracted_data.get("plafondParActe")) or self._as_number(plafond_obj.get("parActe") or plafond_obj.get("plafondParActe"))
        plafond_global = self._as_number(extracted_data.get("plafondGlobal")) or self._as_number(plafond_obj.get("global") or plafond_obj.get("plafondGlobal"))
        plafond_devise = extracted_data.get("plafondDevise") or plafond_obj.get("devise") or "TND"
        plafond_type_principal = extracted_data.get("plafondTypePrincipal") or plafond_obj.get("typePrincipal")

        plafond = PlafondGarantieDTO(
            plafond_annuel=plafond_annuel,
            plafond_mensuel=plafond_mensuel,
            plafond_par_acte=plafond_par_acte,
            plafond_global=plafond_global,
            devise=plafond_devise,
            type_principal=self._parse_enum(TypePlafond, plafond_type_principal),
        ) if any([plafond_annuel, plafond_mensuel, plafond_par_acte, plafond_global]) else None

        # TODO: réactiver après correction du calcul de franchise
        # franchise_obj = extracted_data.get("franchise")
        # franchise_obj = franchise_obj if isinstance(franchise_obj, dict) else {}
        # franchise_montant = self._as_number(extracted_data.get("franchiseMontantFixe"))
        # if franchise_montant is None:
        #     franchise_montant = self._as_number(franchise_obj.get("montantFixe") or franchise_obj.get("montant"))
        # if franchise_montant is None:
        #     # Rétro-compatibilité : ancien format où "franchise" était directement un nombre.
        #     franchise_montant = self._as_number(extracted_data.get("franchise"))
        # franchise_type_raw = extracted_data.get("franchiseType") or franchise_obj.get("type")
        # franchise_type = self._parse_enum(TypeFranchise, franchise_type_raw)
        # franchise_devise = extracted_data.get("franchiseDevise") or franchise_obj.get("devise") or "TND"
        #
        # franchise = FranchiseGarantieDTO(
        #     type=franchise_type or (TypeFranchise.FIXE if franchise_montant else TypeFranchise.AUCUNE),
        #     montant_fixe=franchise_montant,
        #     devise=franchise_devise,
        # ) if franchise_montant is not None or franchise_type is not None else None
        franchise = None  # Valeur par défaut temporaire (calcul de franchise désactivé)

        return GarantieDTO(
            nom_garantie=extracted_data.get("nom"),
            nom_court=extracted_data.get("nomCourt"),
            code_garantie=extracted_data.get("codeGarantie"),
            description=extracted_data.get("description"),
            description_technique=extracted_data.get("descriptionTechnique"),
            domaine=self._parse_enum(DomaineMedical, extracted_data.get("domaine")),
            garantie_obligatoire_par_defaut=extracted_data.get("garantieObligatoireParDefaut"),
            statut_workflow=self._parse_enum(StatutWorkflow, extracted_data.get("statutWorkflow")),
            evenements_couverts_par_defaut=extracted_data.get("evenementsCouvertsParDefaut"),
            type_remboursement=self._parse_enum(TypeRemboursement, extracted_data.get("typeMontant")) or TypeRemboursement.FRAIS_REELS,
            taux_remboursement_base=self._normaliser_taux(extracted_data.get("tauxRemboursement")),
            taux_remboursement_minimum=self._normaliser_taux(extracted_data.get("tauxRemboursementMinimum")),
            taux_remboursement_maximum=self._normaliser_taux(extracted_data.get("tauxRemboursementMaximum")),
            plafond=plafond,
            franchise=franchise,
            prerequis_garantie_ids=extracted_data.get("prerequisGarantieIds"),
            prime_pure_base=extracted_data.get("primePureBase"),
            cree_par=extracted_data.get("creePar"),
        )

    @staticmethod
    def _safe_datetime(value: Any) -> Optional[datetime]:
        """Parse une date extraite (ISO 'AAAA-MM-JJ' ou datetime déjà résolu) en tolérant
        les valeurs absentes ou mal formées plutôt que de faire échouer toute la création.

        Le backend GestionProduit mappe ces champs sur java.time.Instant, qui exige un fuseau
        horaire explicite — une date "naïve" comme "2026-08-15T00:00:00" est rejetée par Jackson
        avec une DateTimeParseException (remontée en 500 par GestionProduit). On force donc UTC
        sur toute date sans fuseau, pour que la sérialisation JSON produise un suffixe "Z"."""
        if value is None or value == "":
            return None
        if isinstance(value, datetime):
            return value if value.tzinfo else value.replace(tzinfo=timezone.utc)
        try:
            parsed = datetime.fromisoformat(str(value))
            return parsed if parsed.tzinfo else parsed.replace(tzinfo=timezone.utc)
        except ValueError:
            logger.warning(f"Date non parsable ignorée: {value!r}")
            return None

    def _create_produit_dto(self, extracted_data: Dict[str, Any], prompt: str) -> ProduitDTO:
        return ProduitDTO(
            nom_produit=extracted_data.get("nom"),
            nom_commercial=extracted_data.get("nomCommercial"),
            description=extracted_data.get("description"),
            type_produit=self._parse_enum(TypeProduit, extracted_data.get("typeProduit")),
            code_produit=extracted_data.get("codeProduit"),
            prix_base=extracted_data.get("prixBase"),
            devise_prix=extracted_data.get("devisePrix") or "TND",
            couverture_geographique=self._parse_enum(CouvertureGeographique, extracted_data.get("couvertureGeographique")),
            version=extracted_data.get("version"),
            date_effet=self._safe_datetime(extracted_data.get("dateEffet")),
            date_expiration=self._safe_datetime(extracted_data.get("dateExpiration")),
            statut_workflow=self._parse_enum(StatutWorkflow, extracted_data.get("statutWorkflow")),
            valide_par=extracted_data.get("validePar"),
            date_validation=self._safe_datetime(extracted_data.get("dateValidation")),
            cree_par=extracted_data.get("creePar"),
        )

    def _create_pack_dto(self, extracted_data: Dict[str, Any], prompt: str) -> PackDTO:
        return PackDTO(
            nom_pack=extracted_data.get("nom"),
            nom_commercial=extracted_data.get("nomCommercial"),
            description=extracted_data.get("description"),
            description_courte=extracted_data.get("descriptionCourte"),
            nom_produit=extracted_data.get("nomProduit"),
            code_pack=extracted_data.get("codePack"),
            prix_mensuel=extracted_data.get("prixMensuel"),
            prix_annuel=extracted_data.get("prixAnnuel"),
            taux_remise_annuelle=extracted_data.get("tauxRemiseAnnuelle"),
            devise_prix=extracted_data.get("devisePrix") or "TND",
            version_pack=extracted_data.get("versionPack"),
            niveau_couverture=self._parse_enum(NiveauCouverture, extracted_data.get("niveauCouverture")),
            statut_workflow=self._parse_enum(StatutWorkflow, extracted_data.get("statutWorkflow")),
            pack_recommande=extracted_data.get("packRecommande"),
            color_theme=self._normaliser_couleur(extracted_data.get("colorTheme")),
            options_disponibles=extracted_data.get("optionsDisponibles"),
            date_effet=self._safe_datetime(extracted_data.get("dateEffet")),
            date_expiration=self._safe_datetime(extracted_data.get("dateExpiration")),
            cree_par=extracted_data.get("creePar"),
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

    def _check_missing_garantie_fields(self, garantie: GarantieDTO, extracted_data: Optional[Dict[str, Any]] = None) -> List[str]:
        missing = []
        extracted_data = extracted_data or {}
        if not garantie.nom_garantie or not garantie.nom_garantie.strip():
            missing.append("nom")
        if not garantie.domaine:
            # Une valeur a été fournie mais n'a pas pu être résolue en enum (ex: "DIABETOLOGIE"
            # n'existe pas) : c'est une valeur invalide, pas un champ absent — distinction faite
            # par l'appelant pour ne pas répondre "il me manque : domaine" à tort.
            missing.append("domaine_invalide" if extracted_data.get("domaine") else "domaine")
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
        if not garantie.description or not garantie.description.strip():
            # Description obligatoire côté Spring Boot ; l'extraction (notamment celle des
            # garanties imbriquées dans un pack) ne la demande pas toujours au LLM.
            nom = garantie.nom_garantie or ""
            prefix = "" if nom.lower().startswith("garantie") else "Garantie "
            garantie.description = f"{prefix}{nom}"

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
                data = {
                    "recommendations": result.get("recommendations", []),
                    "recommended_products": result.get("recommended_products", [])
                }
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
            entity_type=self._get_entity_type_from_action(action_value),
            choices=result.get("choices")
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
            "RECOMMANDATION": [],
            # L'action annulée peut être n'importe laquelle des 3 entités — on rafraîchit
            # les 3 listes plutôt que de complexifier ce mapping statique par entité annulée.
            "UNDO": ["garanties", "packs", "produits"],
            "GENERAL": []
        }
        return refresh_map.get(action, [])

    def _get_default_success_message(self, action: str) -> str:
        return {
            "CREATE_GARANTIE": "Garantie créée avec succès",
            "CREATE_PRODUIT": "Produit créé avec succès",
            "CREATE_PACK": "Pack créé avec succès",
            "ADD_GARANTIE_TO_PACK": "Garantie ajoutée au pack avec succès",
            "CONFIGURATION_PACK": "Pack configuré avec succès",
            "RECOMMANDATION": "Recommandation générée avec succès",
            "UNDO": "Dernière action annulée avec succès"
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
            "RECOMMANDATION": "RECOMMANDATION",
            "GENERAL": "GENERAL"
        }.get(action, "UNKNOWN")

    # ------------------------------------------------------------------
    # UNDO
    # ------------------------------------------------------------------

    def _execute_undo(self, session_id: Optional[str], jwt_token: Optional[str] = None) -> Dict[str, Any]:
        """Annule la dernière action CREATE/UPDATE non déjà annulée de la session (un seul
        niveau). DELETE est hors périmètre — voir action_log_service.UNDOABLE_ACTION_TYPES
        pour la justification."""
        if not session_id:
            return {"success": False, "error": "Aucune session active : impossible de déterminer quelle action annuler."}

        action_entry = action_log_service.get_last_undoable_action(session_id)
        if not action_entry:
            return {"success": False, "error": "Aucune action à annuler pour cette session."}

        entity_type = action_entry["entity_type"]
        entity_id = action_entry["entity_id"]
        entity_name = action_entry["entity_name"]
        action_type = action_entry["action_type"]

        try:
            if action_type == "CREATE":
                if entity_type == "GARANTIE":
                    self.spring_boot_client.delete_garantie_sync(entity_id, jwt_token)
                elif entity_type == "PRODUIT":
                    self.spring_boot_client.delete_produit_sync(entity_id, jwt_token)
                elif entity_type == "PACK":
                    self.spring_boot_client.delete_pack_sync(entity_id, jwt_token)
                else:
                    return {"success": False, "error": f"Type d'entité inconnu pour l'annulation: {entity_type}"}
                message = f"Création de {entity_type.lower()} « {entity_name} » annulée (supprimée)."
            elif action_type == "UPDATE":
                before_state = action_entry.get("before_state")
                if not before_state:
                    return {"success": False, "error": "Impossible de restaurer l'état antérieur (non enregistré)."}
                if entity_type == "GARANTIE":
                    self.spring_boot_client.update_garantie_sync(entity_id, GarantieDTO.model_validate(before_state), jwt_token)
                elif entity_type == "PRODUIT":
                    self.spring_boot_client.update_produit_sync(entity_id, ProduitDTO.model_validate(before_state), jwt_token)
                elif entity_type == "PACK":
                    self.spring_boot_client.update_pack_sync(entity_id, PackDTO.model_validate(before_state), jwt_token)
                else:
                    return {"success": False, "error": f"Type d'entité inconnu pour l'annulation: {entity_type}"}
                message = f"Modification de {entity_type.lower()} « {entity_name} » annulée (état antérieur restauré)."
            else:
                return {"success": False, "error": f"Type d'action non annulable: {action_type}"}
        except Exception as e:
            return {"success": False, "error": f"Échec de l'annulation : {e}"}

        action_log_service.mark_undone(action_entry["_id"])

        return {
            "success": True,
            "action": "UNDO",
            "message": message,
            "entity_type": entity_type,
            "entity_id": entity_id,
        }

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
