from fastapi import APIRouter, HTTPException
from typing import Dict, Any, List
from datetime import datetime, timedelta
import logging

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/analytics", tags=["analytics"])

# In-memory storage for analytics (in production, use Redis or a database)
analytics_store = {
    "prompts_processed": 0,
    "recommendations_generated": 0,
    "creations_successful": 0,
    "creations_failed": 0,
    "ai_calls": 0,
    "ai_fallbacks": 0,
    "start_time": datetime.now()
}


@router.get("/health")
async def health_check():
    """Health check endpoint for analytics service."""
    return {
        "status": "UP",
        "service": "Chatbot Analytics Service",
        "description": "Analytics service for chatbot AI metrics",
        "version": "1.0.0",
        "timestamp": datetime.now().isoformat()
    }


@router.get("/metrics")
async def get_all_metrics():
    """Get all chatbot analytics metrics."""
    uptime = datetime.now() - analytics_store["start_time"]
    
    return {
        "timestamp": datetime.now().isoformat(),
        "uptime_seconds": uptime.total_seconds(),
        "metrics": {
            "promptsProcessed": analytics_store["prompts_processed"],
            "recommendationsGenerated": analytics_store["recommendations_generated"],
            "creationsSuccessful": analytics_store["creations_successful"],
            "creationsFailed": analytics_store["creations_failed"],
            "aiCalls": analytics_store["ai_calls"],
            "aiFallbacks": analytics_store["ai_fallbacks"]
        },
        "rates": {
            "successRate": calculate_success_rate(),
            "aiSuccessRate": calculate_ai_success_rate(),
            "fallbackRate": calculate_fallback_rate()
        }
    }


@router.get("/metrics/chatbot")
async def get_chatbot_metrics():
    """Get chatbot-specific metrics."""
    return {
        "timestamp": datetime.now().isoformat(),
        "chatbotMetrics": {
            "promptsTotal": analytics_store["prompts_processed"],
            "recommendationsGenerated": analytics_store["recommendations_generated"],
            "creationsSuccessful": analytics_store["creations_successful"],
            "creationsFailed": analytics_store["creations_failed"]
        },
        "performance": {
            "successRate": calculate_success_rate(),
            "averageCreationSuccess": calculate_creation_success_rate()
        }
    }


@router.get("/metrics/ai")
async def get_ai_metrics():
    """Get AI-specific metrics."""
    return {
        "timestamp": datetime.now().isoformat(),
        "aiMetrics": {
            "aiCallsTotal": analytics_store["ai_calls"],
            "aiFallbacks": analytics_store["ai_fallbacks"],
            "aiSuccessRate": calculate_ai_success_rate(),
            "fallbackRate": calculate_fallback_rate()
        }
    }


@router.get("/statistics")
async def get_global_statistics():
    """Get global statistics summary."""
    uptime = datetime.now() - analytics_store["start_time"]
    
    return {
        "timestamp": datetime.now().isoformat(),
        "uptime_seconds": uptime.total_seconds(),
        "statistics": {
            "totalPromptsProcessed": analytics_store["prompts_processed"],
            "totalRecommendations": analytics_store["recommendations_generated"],
            "totalCreations": analytics_store["creations_successful"] + analytics_store["creations_failed"],
            "successRate": calculate_success_rate(),
            "aiAvailability": calculate_ai_success_rate()
        }
    }


@router.get("/activity/daily")
async def get_daily_activity(days: int = 7):
    """Get daily activity history (simulated for demo)."""
    daily_activity = []
    
    for i in range(days, -1, -1):
        date = datetime.now() - timedelta(days=i)
        # Simulate activity data (in production, query from database)
        daily_activity.append({
            "date": date.date().isoformat(),
            "prompts": max(0, analytics_store["prompts_processed"] // (days + 1)),
            "creations": max(0, analytics_store["creations_successful"] // (days + 1)),
            "recommendations": max(0, analytics_store["recommendations_generated"] // (days + 1))
        })
    
    return {
        "timestamp": datetime.now().isoformat(),
        "dailyActivity": daily_activity,
        "periodDays": days
    }


@router.post("/metrics/increment")
async def increment_metric(metric_name: str):
    """Increment a specific metric (internal use)."""
    valid_metrics = [
        "prompts_processed",
        "recommendations_generated",
        "creations_successful",
        "creations_failed",
        "ai_calls",
        "ai_fallbacks"
    ]
    
    if metric_name not in valid_metrics:
        raise HTTPException(status_code=400, detail=f"Invalid metric name: {metric_name}")
    
    analytics_store[metric_name] = analytics_store.get(metric_name, 0) + 1
    logger.info(f"Metric incremented: {metric_name} = {analytics_store[metric_name]}")
    
    return {
        "metric": metric_name,
        "value": analytics_store[metric_name],
        "timestamp": datetime.now().isoformat()
    }


# Helper functions

def calculate_success_rate() -> float:
    """Calculate overall success rate."""
    total = analytics_store["creations_successful"] + analytics_store["creations_failed"]
    if total == 0:
        return 0.0
    return round(analytics_store["creations_successful"] / total * 100, 2)


def calculate_creation_success_rate() -> float:
    """Calculate creation success rate."""
    return calculate_success_rate()


def calculate_ai_success_rate() -> float:
    """Calculate AI success rate (no fallbacks)."""
    total_ai = analytics_store["ai_calls"]
    if total_ai == 0:
        return 100.0  # Assume 100% if no calls made yet
    fallbacks = analytics_store["ai_fallbacks"]
    return round((total_ai - fallbacks) / total_ai * 100, 2)


def calculate_fallback_rate() -> float:
    """Calculate AI fallback rate."""
    total_ai = analytics_store["ai_calls"]
    if total_ai == 0:
        return 0.0
    return round(analytics_store["ai_fallbacks"] / total_ai * 100, 2)
