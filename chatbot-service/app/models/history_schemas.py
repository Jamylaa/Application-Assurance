from datetime import datetime
from typing import Any, Dict, List, Optional

from pydantic import BaseModel

from app.models.schemas import CamelCaseModel


class ChatMessageDTO(CamelCaseModel):
    """Miroir de l'interface ChatMessage du frontend (conversation-storage.service.ts)."""
    id: str
    sender: str  # 'user' | 'bot'
    text: str
    timestamp: datetime
    intent: Optional[str] = None
    data: Optional[Dict[str, Any]] = None


class ConversationDTO(CamelCaseModel):
    """Miroir de l'interface Conversation du frontend — permet de repointer
    ConversationStorageService sur ces endpoints sans changer sa forme de données."""
    id: str
    title: str
    messages: List[ChatMessageDTO] = []
    created_at: datetime
    updated_at: datetime
    session_id: Optional[str] = None


class UndoRequestDTO(CamelCaseModel):
    session_id: str


class UndoResultDTO(CamelCaseModel):
    success: bool
    message: str
    entity_type: Optional[str] = None
    entity_id: Optional[str] = None
    refresh_targets: Optional[List[str]] = None


class ExportFormat(BaseModel):
    format: str = "txt"  # 'pdf' | 'txt'
