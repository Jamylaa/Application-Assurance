from fastapi import APIRouter, HTTPException, Request
from typing import Dict, Any
import uuid
from datetime import datetime
from app.models.schemas import (
    ChatbotRequestDTO, ChatbotResponseDTO, RecommendationRequestDTO,
    RecommendationResponseDTO
)
from app.services.orchestrator_service import ChatbotOrchestratorService
from app.services.recommendation_service import RecommendationService
from app.services.spring_boot_client import SpringBootClient
from app.config import settings
import logging
import traceback

# Import analytics store for tracking metrics
from app.api.analytics import analytics_store

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/chatbot", tags=["chatbot"])

# Initialize services
orchestrator = ChatbotOrchestratorService()
spring_boot_client = SpringBootClient()
recommendation_service = RecommendationService(spring_boot_client)


@router.post("/process", response_model=ChatbotResponseDTO)
async def process_prompt(request: ChatbotRequestDTO, http_request: Request):
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
    # Generate or use correlation ID
    correlation_id = request.correlation_id or str(uuid.uuid4())
    debug_trace = [] if request.enable_debug else None
    
    def log_debug(message: str):
        """Add debug message if debug mode is enabled."""
        if debug_trace is not None:
            debug_trace.append({"timestamp": str(datetime.now()), "message": message})
        logger.info(f"[{correlation_id}] {message}")
    
    try:
        log_debug(f"START Processing prompt: {request.prompt[:100]}...")
        log_debug(f"Request details - session_id: {request.session_id}, enable_debug: {request.enable_debug}")
        
        # Validate request
        if not request.prompt or not request.prompt.strip():
            error_msg = "Prompt cannot be empty"
            log_debug(f"Validation error: {error_msg}")
            return ChatbotResponseDTO(
                success=False,
                intent="ERROR",
                message=error_msg,
                errors=[error_msg],
                correlation_id=correlation_id,
                debug_trace=debug_trace
            )
        
        # Check AI service availability (but don't block - fallback will handle it)
        ai_available = orchestrator.ai_extraction_service.is_ai_available()
        log_debug(f"AI Service Available: {ai_available}")
        log_debug(f"API Key configured: {bool(orchestrator.ai_extraction_service.api_key and orchestrator.ai_extraction_service.api_key.strip())}")
        log_debug(f"AI enabled: {orchestrator.ai_extraction_service.github_enabled}")
        log_debug(f"AI extraction enabled: {orchestrator.ai_extraction_service.ai_extraction_enabled}")

        if not ai_available:
            log_debug("AI service unavailable, orchestrator will use fallback parser")
        
        # Pass correlation_id and debug flag to orchestrator
        response = orchestrator.process_prompt(request)
        response.correlation_id = correlation_id
        if request.enable_debug:
            response.debug_trace = debug_trace
        
        log_debug(f"Response generated successfully - success: {response.success}, intent: {response.intent}")
        return response
    except HTTPException as he:
        log_debug(f"HTTP error processing prompt: {he.detail}")
        log_debug(f"HTTP status code: {he.status_code}")
        log_debug(f"Stacktrace: {traceback.format_exc()}")
        raise he
    except Exception as e:
        error_msg = f"Internal server error: {str(e)}"
        log_debug(f"Unexpected error processing prompt: {error_msg}")
        log_debug(f"Stacktrace: {traceback.format_exc()}")
        
        # Return error response instead of raising HTTPException to avoid silent 400
        return ChatbotResponseDTO(
            success=False,
            intent="ERROR",
            message=error_msg,
            errors=[error_msg],
            correlation_id=correlation_id,
            debug_trace=debug_trace
        )


@router.post("/recommendations", response_model=RecommendationResponseDTO)
async def generate_recommendations(request: RecommendationRequestDTO):
    """
    Generate personalized recommendations based on client profile.
    Returns the top 3 packs and products that match the client's needs.
    """
    correlation_id = request.correlation_id or str(uuid.uuid4())
    debug_trace = [] if request.enable_debug else None
    
    def log_debug(message: str):
        if debug_trace is not None:
            debug_trace.append({"timestamp": str(datetime.now()), "message": message})
        logger.info(f"[{correlation_id}] {message}")
    
    try:
        log_debug(f"START Generating recommendations for session: {request.session_id}")
        log_debug(f"Client profile - age: {request.age}, gender: {request.gender}, budget: {request.monthly_budget}")
        
        response = recommendation_service.generate_recommendations(request)
        response.correlation_id = correlation_id
        if request.enable_debug:
            response.debug_trace = debug_trace
        
        # Track recommendation generation
        analytics_store["recommendations_generated"] = analytics_store.get("recommendations_generated", 0) + 1
        
        log_debug(f"Recommendations generated successfully - {len(response.recommended_packs)} packs, {len(response.recommended_products)} products")
        return response
    except Exception as e:
        error_msg = f"Error generating recommendations: {str(e)}"
        log_debug(f"{error_msg}")
        log_debug(f"Stacktrace: {traceback.format_exc()}")
        
        raise HTTPException(status_code=500, detail=error_msg)


@router.get("/health")
async def health_check():
    """Health check endpoint."""
    return {
        "status": "UP",
        "service": "Chatbot FastAPI Service",
        "description": "Chatbot intelligent pour la création et configuration des garanties, produits et packs",
        "version": "1.0.0",
        "architecture": "FastAPI Microservice",
        "ai_available": orchestrator.ai_extraction_service.is_ai_available(),
        "ai_details": {
            "api_key_configured": bool(orchestrator.ai_extraction_service.api_key and orchestrator.ai_extraction_service.api_key.strip()),
            "github_enabled": orchestrator.ai_extraction_service.github_enabled,
            "ai_extraction_enabled": orchestrator.ai_extraction_service.ai_extraction_enabled,
            "fallback_on_error": orchestrator.ai_extraction_service.fallback_on_error,
            "model": orchestrator.ai_extraction_service.model_name,
            "api_url": orchestrator.ai_extraction_service.api_url
        }
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
        "provider": "GitHub Models (gpt-4o-mini)",
        "fallbackMode": not ai_available,
        "message": (
            "Service IA disponible - Extraction intelligente activée" if ai_available
            else "Service IA indisponible - Mode fallback activé (extraction par patterns)"
        )
    }
