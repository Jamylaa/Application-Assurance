from fastapi import HTTPException, status


class ChatbotException(Exception):
    """Base exception for chatbot errors."""
    pass


class AIServiceUnavailableException(ChatbotException):
    """Exception raised when AI service is unavailable."""
    pass


class ValidationException(ChatbotException):
    """Exception raised when validation fails."""
    pass


class SpringBootServiceException(ChatbotException):
    """Exception raised when Spring Boot service communication fails."""
    pass


class ExtractionException(ChatbotException):
    """Exception raised when data extraction fails."""
    pass


class RecommendationException(ChatbotException):
    """Exception raised when recommendation generation fails."""
    pass


def handle_chatbot_exception(exc: ChatbotException) -> HTTPException:
    """Convert chatbot exception to HTTP exception."""
    if isinstance(exc, AIServiceUnavailableException):
        return HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail=f"AI Service unavailable: {str(exc)}"
        )
    elif isinstance(exc, ValidationException):
        return HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"Validation error: {str(exc)}"
        )
    elif isinstance(exc, SpringBootServiceException):
        return HTTPException(
            status_code=status.HTTP_502_BAD_GATEWAY,
            detail=f"Spring Boot service error: {str(exc)}"
        )
    elif isinstance(exc, ExtractionException):
        return HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"Extraction error: {str(exc)}"
        )
    elif isinstance(exc, RecommendationException):
        return HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail=f"Recommendation error: {str(exc)}"
        )
    else:
        return HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Internal error: {str(exc)}"
        )
