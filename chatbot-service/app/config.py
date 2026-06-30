from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    # FastAPI Configuration
    fastapi_env: str = "development"
    fastapi_port: int = 9001
    fastapi_host: str = "0.0.0.0"
    
    # Spring Boot Service Configuration
    spring_boot_base_url: str = "http://localhost:8080/api"
    
    # Google Gemini API Configuration
    gemini_api_key: str = "GITHUB_API_KEY"
    gemini_model: str = "gemini-2.0-flash"
    gemini_url: str = "https://generativelanguage.googleapis.com/v1beta/models"
    gemini_timeout_seconds: int = 30
    gemini_max_retries: int = 3
    gemini_retry_delay_ms: int = 1000
    gemini_enabled: bool = True
    
    # Chatbot Configuration
    ai_extraction_enabled: bool = True
    fallback_on_ai_error: bool = True
    
    class Config:
        env_file = ".env"
        case_sensitive = False


settings = Settings()
