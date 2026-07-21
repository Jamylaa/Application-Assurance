import logging
import re
from typing import List, Optional

from fastapi import APIRouter, HTTPException, Query
from fastapi.responses import Response

from app.models.history_schemas import ConversationDTO
from app.services.history_service import history_service
from app.services import export_service

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/chatbot", tags=["chatbot-history"])


@router.post("/conversations", response_model=ConversationDTO)
async def save_conversation(conversation: ConversationDTO):
    """Sauvegarde (ou met à jour) une conversation côté serveur — appelé par
    ConversationStorageService en plus du stockage local existant (IndexedDB), pour que
    l'historique survive à un changement de device ou un nettoyage du navigateur."""
    try:
        return history_service.save_conversation(conversation)
    except Exception as e:
        logger.error(f"Error saving conversation: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Erreur lors de la sauvegarde: {e}")


@router.get("/conversations", response_model=List[ConversationDTO])
async def list_conversations(q: Optional[str] = Query(None, description="Recherche texte optionnelle"),
                              limit: int = Query(50, ge=1, le=200)):
    """Liste les conversations sauvegardées côté serveur, triées par date de modification
    décroissante (ou recherche si `q` est fourni)."""
    try:
        if q:
            return history_service.search_conversations(q, limit=limit)
        return history_service.get_all_conversations(limit=limit)
    except Exception as e:
        logger.error(f"Error listing conversations: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Erreur lors de la récupération: {e}")


@router.get("/conversations/{conversation_id}", response_model=ConversationDTO)
async def get_conversation(conversation_id: str):
    conversation = history_service.get_conversation(conversation_id)
    if not conversation:
        raise HTTPException(status_code=404, detail="Conversation introuvable")
    return conversation


@router.delete("/conversations/{conversation_id}")
async def delete_conversation(conversation_id: str):
    deleted = history_service.delete_conversation(conversation_id)
    if not deleted:
        raise HTTPException(status_code=404, detail="Conversation introuvable")
    return {"success": True}


@router.get("/export/{conversation_id}")
async def export_conversation(conversation_id: str, format: str = Query("txt", pattern="^(pdf|txt)$")):
    """Export serveur d'une conversation (fpdf2 pour le PDF, pur Python sans dépendance
    système — remplace html2canvas+jsPDF côté navigateur pour ce cas d'usage précis)."""
    conversation = history_service.get_conversation(conversation_id)
    if not conversation:
        raise HTTPException(status_code=404, detail="Conversation introuvable")

    safe_title = re.sub(r'[^a-zA-Z0-9_-]+', '_', conversation.title or "conversation")[:60] or "conversation"

    if format == "pdf":
        content = export_service.generate_pdf(conversation)
        return Response(
            content=content,
            media_type="application/pdf",
            headers={"Content-Disposition": f'attachment; filename="{safe_title}.pdf"'}
        )

    content = export_service.generate_text(conversation)
    return Response(
        content=content.encode("utf-8"),
        media_type="text/plain",
        headers={"Content-Disposition": f'attachment; filename="{safe_title}.txt"'}
    )
