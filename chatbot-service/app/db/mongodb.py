import logging
from typing import Optional

from pymongo import MongoClient
from pymongo.database import Database

from app.config import settings

logger = logging.getLogger(__name__)

_client: Optional[MongoClient] = None
_database: Optional[Database] = None


def connect() -> None:
    """Ouvre la connexion Mongo dédiée au chatbot (historique, journal d'actions).
    Base logique distincte de vermeg_db : ce sont des données opérationnelles du
    chatbot, pas des données métier assurance gérées par GestionProduit.

    Client synchrone (PyMongo) et non Motor : le reste de l'orchestrateur (spring_boot_client
    compris) est entièrement synchrone (cf. le pattern _run_sync existant) — mélanger un
    driver Mongo asynchrone dans ce même code sync ajouterait de la complexité sans bénéfice
    réel pour le volume de trafic d'un projet PFE."""
    global _client, _database
    _client = MongoClient(settings.chatbot_mongodb_uri)
    _database = _client[settings.chatbot_mongodb_database]
    logger.info(f"Connected to chatbot MongoDB database: {settings.chatbot_mongodb_database}")


def close() -> None:
    global _client, _database
    if _client is not None:
        _client.close()
        _client = None
        _database = None
        logger.info("Chatbot MongoDB connection closed")


def get_database() -> Database:
    if _database is None:
        raise RuntimeError("MongoDB not connected — call connect() during app startup first")
    return _database
