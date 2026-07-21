import logging
from datetime import datetime, timezone
from typing import Any, Dict, Optional

from app.db.mongodb import get_database

logger = logging.getLogger(__name__)

COLLECTION = "actions_log"

# Périmètre undo décidé pour Lot B : CREATE + UPDATE uniquement, un seul niveau (la
# dernière action non annulée de la session). DELETE est exclu — re-créer l'entité
# supprimée ne restaurerait pas les associations PackGarantie qui pointaient vers elle,
# une cascade disproportionnée pour la valeur qu'apporterait cet undo.
UNDOABLE_ACTION_TYPES = {"CREATE", "UPDATE"}


class ActionLogService:
    """Journal des actions CREATE/UPDATE effectuées par le chatbot, par session — sert
    uniquement à retrouver et annuler la dernière action ; ce n'est pas l'historique de
    conversation affiché à l'utilisateur (voir history_service pour ça)."""

    def log_action(self, session_id: str, action_type: str, entity_type: str,
                    entity_id: str, entity_name: str,
                    before_state: Optional[Dict[str, Any]],
                    after_state: Optional[Dict[str, Any]]) -> None:
        if not session_id or action_type not in UNDOABLE_ACTION_TYPES:
            return
        db = get_database()
        db[COLLECTION].insert_one({
            "session_id": session_id,
            "action_type": action_type,
            "entity_type": entity_type,
            "entity_id": entity_id,
            "entity_name": entity_name,
            "before_state": before_state,
            "after_state": after_state,
            "timestamp": datetime.now(timezone.utc),
            "undone": False,
        })
        logger.info(f"Logged {action_type} {entity_type} '{entity_name}' (id={entity_id}) for session {session_id}")

    def get_last_undoable_action(self, session_id: str) -> Optional[Dict[str, Any]]:
        db = get_database()
        return db[COLLECTION].find_one(
            {"session_id": session_id, "undone": False},
            sort=[("timestamp", -1)]
        )

    def mark_undone(self, action_id: Any) -> None:
        db = get_database()
        db[COLLECTION].update_one({"_id": action_id}, {"$set": {"undone": True}})


action_log_service = ActionLogService()
