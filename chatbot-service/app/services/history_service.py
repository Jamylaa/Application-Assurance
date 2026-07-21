import logging
from typing import Any, Dict, List, Optional

from app.db.mongodb import get_database
from app.models.history_schemas import ConversationDTO

logger = logging.getLogger(__name__)

COLLECTION = "conversations"
DEFAULT_LIMIT = 50


class HistoryService:
    """Persistance serveur de l'historique des conversations chatbot — complète (ne
    remplace pas) le stockage local existant côté frontend (IndexedDB via localforage),
    pour que l'historique survive à un changement de device ou un nettoyage du navigateur."""

    def save_conversation(self, conversation: ConversationDTO) -> ConversationDTO:
        db = get_database()
        doc = conversation.model_dump(mode="json", by_alias=False)
        doc["_id"] = doc.pop("id")
        db[COLLECTION].replace_one({"_id": doc["_id"]}, doc, upsert=True)
        return conversation

    def get_conversation(self, conversation_id: str) -> Optional[ConversationDTO]:
        db = get_database()
        doc = db[COLLECTION].find_one({"_id": conversation_id})
        return self._to_dto(doc) if doc else None

    def get_conversation_by_session(self, session_id: str) -> Optional[ConversationDTO]:
        db = get_database()
        doc = db[COLLECTION].find_one({"session_id": session_id})
        return self._to_dto(doc) if doc else None

    def get_all_conversations(self, limit: int = DEFAULT_LIMIT) -> List[ConversationDTO]:
        db = get_database()
        cursor = db[COLLECTION].find().sort("updated_at", -1).limit(limit)
        return [self._to_dto(doc) for doc in cursor]

    def delete_conversation(self, conversation_id: str) -> bool:
        db = get_database()
        result = db[COLLECTION].delete_one({"_id": conversation_id})
        return result.deleted_count > 0

    def search_conversations(self, query: str, limit: int = DEFAULT_LIMIT) -> List[ConversationDTO]:
        db = get_database()
        regex = {"$regex": query, "$options": "i"}
        cursor = db[COLLECTION].find({
            "$or": [
                {"title": regex},
                {"messages.text": regex},
            ]
        }).sort("updated_at", -1).limit(limit)
        return [self._to_dto(doc) for doc in cursor]

    @staticmethod
    def _to_dto(doc: Dict[str, Any]) -> ConversationDTO:
        doc = dict(doc)
        doc["id"] = doc.pop("_id")
        return ConversationDTO.model_validate(doc)


history_service = HistoryService()
