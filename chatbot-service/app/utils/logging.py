import logging
import sys
from app.config import settings


def setup_logging():
    """Configure application logging."""
    
    # Create logger
    logger = logging.getLogger()
    logger.setLevel(logging.INFO if settings.fastapi_env == "production" else logging.DEBUG)
    
    # Console handler
    console_handler = logging.StreamHandler(sys.stdout)
    console_handler.setLevel(logging.DEBUG)
    
    # Formatter
    formatter = logging.Formatter(
        '%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        datefmt='%Y-%m-%d %H:%M:%S'
    )
    console_handler.setFormatter(formatter)
    
    # Add handler to logger
    logger.addHandler(console_handler)
    
    return logger
