"""Seed rich user documents into Firestore for Android demos."""
import logging
import os
from typing import Optional

from fastapi import APIRouter, Header, HTTPException

from app.data.user_profiles import all_demo_users

logger = logging.getLogger(__name__)

router = APIRouter()


def _check_seed_key(x_seed_key: Optional[str]) -> None:
    expected = os.getenv("SEED_API_KEY") or os.getenv("SYNC_API_KEY")
    if expected and (not x_seed_key or x_seed_key != expected):
        raise HTTPException(status_code=401, detail="Missing or invalid X-Seed-Key header")


@router.post("/seed/users")
def post_seed_users(x_seed_key: Optional[str] = Header(None, alias="X-Seed-Key")):
    """Write users/user_001 and users/user_002 with nested cloudData, dailySpend, alerts, wasteInsights."""
    _check_seed_key(x_seed_key)
    try:
        from app.services.firestore_sync import _ensure_firebase

        db = _ensure_firebase()
        users = all_demo_users()
        batch = db.batch()
        for uid, profile in users.items():
            ref = db.collection("users").document(uid)
            batch.set(ref, profile, merge=False)
        batch.commit()
        return {"ok": True, "users": list(users.keys()), "message": "Firestore users/* documents written"}
    except RuntimeError as e:
        raise HTTPException(status_code=503, detail=str(e)) from e
    except Exception:
        logger.exception("seed users")
        raise HTTPException(status_code=500, detail="Seed failed") from None
