from fastapi import APIRouter, HTTPException
from typing import Dict, Any
from app.models.schemas import (
    ChatbotRequestDTO, ChatbotResponseDTO, RecommendationRequestDTO,
    RecommendationResponseDTO
)
from app.services.orchestrator_service import ChatbotOrchestratorService
from app.services.recommendation_service import RecommendationService
from app.services.spring_boot_client import SpringBootClient
import logging

# Import analytics store for tracking metrics
from app.api.analytics import analytics_store

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/chatbot", tags=["chatbot"])

# Initialize services
orchestrator = ChatbotOrchestratorService()
spring_boot_client = SpringBootClient()
recommendation_service = RecommendationService(spring_boot_client)


@router.post("/process", response_model=ChatbotResponseDTO)
async def process_prompt(request: ChatbotRequestDTO):
    """
    Process a natural language prompt and execute the appropriate action.
    
    Supported actions:
    - GARANTIE: Create a guarantee
    - PRODUIT: Create a product
    - PACK: Create a pack
    - CONFIGURATION_PACK: Configure a pack
    - AJOUT_GARANTIE_PACK: Add a guarantee to a pack
    - RECOMMANDATION: Generate recommendations
    """
    try:
        logger.info(f"Processing prompt: {request.prompt}")
        response = orchestrator.process_prompt(request)
        return response
    except Exception as e:
        logger.error(f"Error processing prompt: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")


@router.post("/recommendations", response_model=RecommendationResponseDTO)
async def generate_recommendations(request: RecommendationRequestDTO):
    """
    Generate personalized recommendations based on client profile.
    
    Returns the top 3 packs and products that match the client's needs.
    """
    try:
        logger.info(f"Generating recommendations for session: {request.session_id}")
        response = recommendation_service.generate_recommendations(request)
        
        # Track recommendation generation
        analytics_store["recommendations_generated"] = analytics_store.get("recommendations_generated", 0) + 1
        
        return response
    except Exception as e:
        logger.error(f"Error generating recommendations: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail=f"Internal server error: {str(e)}")


@router.get("/health")
async def health_check():
    """Health check endpoint."""
    return {
        "status": "UP",
        "service": "Chatbot FastAPI Service",
        "description": "Chatbot intelligent pour la création et configuration des garanties, produits et packs",
        "version": "1.0.0",
        "architecture": "FastAPI Microservice",
        "ai_available": orchestrator.ai_extraction_service.is_ai_available()
    }


@router.get("/actions")
async def get_supported_actions():
    """Get information about supported actions."""
    return {
        "supportedActions": {
            "GARANTIE": {
                "description": "Création d'une garantie",
                "example": "Créer une garantie hospitalisation premium avec un remboursement de 90%"
            },
            "PRODUIT": {
                "description": "Création d'un produit",
                "example": "Créer un produit d'assurance santé nommé 'Santé Premium'"
            },
            "PACK": {
                "description": "Création d'un pack",
                "example": "Créer un pack Gold lié au produit Santé Premium"
            },
            "CONFIGURATION_PACK": {
                "description": "Configuration d'un pack existant",
                "example": "Configurer le pack Silver avec les garanties optique et dentaire"
            },
            "AJOUT_GARANTIE_PACK": {
                "description": "Ajout d'une garantie à un pack",
                "example": "Ajouter la garantie optique au pack Silver"
            },
            "RECOMMANDATION": {
                "description": "Recommandation intelligente de packs et produits",
                "example": "Je suis un homme de 45 ans, marié, avec deux enfants. Je recherche un pack adapté à ma famille."
            }
        },
        "features": {
            "aiExtraction": "Extraction intelligente des données via Google AI",
            "normalization": "Normalisation automatique des enums et valeurs",
            "validation": "Validation robuste des données extraites",
            "businessLogicReuse": "Réutilisation des services métier Spring Boot",
            "errorHandling": "Gestion centralisée des erreurs",
            "fallbackMode": "Mode fallback automatique si IA indisponible"
        }
    }


@router.get("/ai-status")
async def get_ai_status():
    """Get AI service status."""
    ai_available = orchestrator.ai_extraction_service.is_ai_available()
    
    return {
        "aiAvailable": ai_available,
        "provider": "Google AI (Gemini)",
        "fallbackMode": not ai_available,
        "message": (
            "Service IA disponible - Extraction intelligente activée" if ai_available
            else "Service IA indisponible - Mode fallback activé (extraction par patterns)"
        )
    }
