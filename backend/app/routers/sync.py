"""Trigger cloud → Firestore sync (for Android app real-time listeners)."""
import logging
import os
from typing import Optional

from fastapi import APIRouter, Header, HTTPException

from app.services.firestore_sync import sync_to_firestore

logger = logging.getLogger(__name__)

router = APIRouter()


def _check_sync_key(x_sync_key: Optional[str]) -> None:
    expected = os.getenv("SYNC_API_KEY")
    if expected and (not x_sync_key or x_sync_key != expected):
        raise HTTPException(status_code=401, detail="Missing or invalid X-Sync-Key header")


@router.post("/sync/aws")
def post_sync_aws(x_sync_key: Optional[str] = Header(None, alias="X-Sync-Key")):
    _check_sync_key(x_sync_key)
    try:
        return sync_to_firestore("aws")
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e)) from e
    except Exception:
        logger.exception("sync aws")
        raise HTTPException(status_code=500, detail="Sync failed") from None


@router.post("/sync/azure")
def post_sync_azure(x_sync_key: Optional[str] = Header(None, alias="X-Sync-Key")):
    _check_sync_key(x_sync_key)
    try:
        return sync_to_firestore("azure")
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e)) from e
    except Exception:
        logger.exception("sync azure")
        raise HTTPException(status_code=500, detail="Sync failed") from None


@router.post("/sync/gcp")
def post_sync_gcp(x_sync_key: Optional[str] = Header(None, alias="X-Sync-Key")):
    _check_sync_key(x_sync_key)
    try:
        return sync_to_firestore("gcp")
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e)) from e
    except Exception:
        logger.exception("sync gcp")
        raise HTTPException(status_code=500, detail="Sync failed") from None


@router.post("/sync/all")
def post_sync_all(x_sync_key: Optional[str] = Header(None, alias="X-Sync-Key")):
    _check_sync_key(x_sync_key)
    try:
        return sync_to_firestore("all")
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e)) from e
    except Exception:
        logger.exception("sync all")
        raise HTTPException(status_code=500, detail="Sync failed") from None
