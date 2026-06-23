from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.api.chatbot import router as chatbot_router
from app.api.analytics import router as analytics_router
from app.config import settings
import logging

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)

# Create FastAPI app
app = FastAPI(
    title="Chatbot Service API",
    description="Microservice FastAPI pour le chatbot intelligent de gestion des produits d'assurance",
    version="1.0.0",
    docs_url="/docs",
    redoc_url="/redoc"
)

# Configure CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:4200"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(chatbot_router)
app.include_router(analytics_router)


@app.on_event("startup")
async def startup_event():
    """Run on application startup."""
    logger.info("Starting Chatbot FastAPI Service...")
    logger.info(f"Environment: {settings.fastapi_env}")
    logger.info(f"AI Service Available: {settings.gemini_enabled and settings.gemini_api_key}")
    logger.info(f"Spring Boot URL: {settings.spring_boot_base_url}")


@app.on_event("shutdown")
async def shutdown_event():
    """Run on application shutdown."""
    logger.info("Shutting down Chatbot FastAPI Service...")


@app.get("/")
async def root():
    """Root endpoint."""
    return {
        "service": "Chatbot FastAPI Service",
        "version": "1.0.0",
        "status": "running",
        "documentation": "/docs"
    }


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(
        "main:app",
        host=settings.fastapi_host,
        port=settings.fastapi_port,
        reload=settings.fastapi_env == "development"
    )
