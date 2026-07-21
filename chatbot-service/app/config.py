from pydantic_settings import BaseSettings
from typing import Optional
from pathlib import Path

_ENV_FILE = Path(__file__).resolve().parent.parent / ".env"


class Settings(BaseSettings):
    # FastAPI Configuration
    fastapi_env: str = "development"
    fastapi_port: int = 9001
    fastapi_host: str = "0.0.0.0"

    # Spring Boot Service Configuration
    spring_boot_base_url: str = "http://gestionProduit:9093/api"
    spring_boot_timeout: int = 30
    spring_boot_max_retries: int = 3
    spring_boot_retry_delay: int = 1000

    # MongoDB Configuration — base logique dédiée au chatbot (historique des conversations,
    # journal d'actions pour l'undo), distincte de vermeg_db (données métier GestionProduit).
    # Même instance Mongo que GestionProduit par défaut, base différente.
    chatbot_mongodb_uri: str = "mongodb://admin:password@mongodb:27017/vermeg_chatbot_db?authSource=admin"
    chatbot_mongodb_database: str = "vermeg_chatbot_db"

    # GitHub AI Models API Configuration
    github_api_key: Optional[str] = None
    github_model: str = "gpt-4o-mini"  # Default model for GitHub Models
    github_url: str = "https://models.inference.ai.azure.com"  # GitHub Models via Azure
    github_timeout_seconds: int = 30
    github_max_retries: int = 3
    github_retry_delay_ms: int = 1000
    github_enabled: bool = True

    # Chatbot Configuration
    ai_extraction_enabled: bool = True
    fallback_on_ai_error: bool = True
    enable_detailed_logging: bool = True
    strict_validation: bool = True

    # CORS Configuration — liste d'origines separees par des virgules
    cors_allowed_origins: str = "http://localhost:4200"

    @property
    def cors_allowed_origins_list(self) -> list[str]:
        return [origin.strip() for origin in self.cors_allowed_origins.split(",") if origin.strip()]

    class Config:
        env_file = str(_ENV_FILE)
        case_sensitive = False
        extra = "ignore"  # Allow extra env variables


settings = Settings()
