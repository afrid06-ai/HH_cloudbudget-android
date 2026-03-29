"""Per-user cloud credential management + sync endpoints."""
from __future__ import annotations

import logging
import os
from typing import Dict, List, Optional

from fastapi import APIRouter, HTTPException, Header
from pydantic import BaseModel

from app.services.credential_store import (
    store_user_credentials,
    delete_user_credentials,
    list_user_providers,
)
from app.services.user_sync import sync_user, sync_all_users

logger = logging.getLogger(__name__)
router = APIRouter()


def _get_db():
    import firebase_admin
    from firebase_admin import credentials, firestore

    path = os.getenv("FIREBASE_CREDENTIALS_PATH") or os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if not path:
        raise HTTPException(status_code=500, detail="Firebase not configured")
    try:
        firebase_admin.get_app()
    except ValueError:
        firebase_admin.initialize_app(credentials.Certificate(path))
    return firestore.client()


# ─── Models ───

class AWSCredentials(BaseModel):
    access_key_id: str
    secret_access_key: str
    region: str = "us-east-1"


class AzureCredentials(BaseModel):
    tenant_id: str
    client_id: str
    client_secret: str
    subscription_id: str


class GCPCredentials(BaseModel):
    project_id: str
    credentials_json: str  # Full service account JSON as string
    billing_dataset: str = "billing_export"
    billing_table: str = "gcp_billing_export_v1"


class SetBudgetRequest(BaseModel):
    total_budget: float
    aws_budget: Optional[float] = None
    azure_budget: Optional[float] = None
    gcp_budget: Optional[float] = None


# ─── Credential Endpoints ───

@router.post("/users/{user_id}/credentials/aws")
def store_aws_credentials(user_id: str, creds: AWSCredentials):
    """Store AWS credentials for a user (encrypted at rest)."""
    db = _get_db()
    _ensure_user_exists(db, user_id)
    store_user_credentials(db, user_id, "aws", creds.dict())
    return {"ok": True, "provider": "aws", "userId": user_id, "message": "AWS credentials stored securely"}


@router.post("/users/{user_id}/credentials/azure")
def store_azure_credentials(user_id: str, creds: AzureCredentials):
    """Store Azure credentials for a user (encrypted at rest)."""
    db = _get_db()
    _ensure_user_exists(db, user_id)
    store_user_credentials(db, user_id, "azure", creds.dict())
    return {"ok": True, "provider": "azure", "userId": user_id, "message": "Azure credentials stored securely"}


@router.post("/users/{user_id}/credentials/gcp")
def store_gcp_credentials(user_id: str, creds: GCPCredentials):
    """Store GCP credentials for a user (encrypted at rest)."""
    db = _get_db()
    _ensure_user_exists(db, user_id)
    store_user_credentials(db, user_id, "gcp", creds.dict())
    return {"ok": True, "provider": "gcp", "userId": user_id, "message": "GCP credentials stored securely"}


@router.delete("/users/{user_id}/credentials/{provider}")
def remove_credentials(user_id: str, provider: str):
    """Remove stored credentials for a provider."""
    if provider not in ("aws", "azure", "gcp"):
        raise HTTPException(status_code=400, detail="Provider must be aws, azure, or gcp")
    db = _get_db()
    delete_user_credentials(db, user_id, provider)
    return {"ok": True, "provider": provider, "userId": user_id, "message": f"{provider.upper()} credentials removed"}


@router.get("/users/{user_id}/credentials")
def get_connected_providers(user_id: str):
    """List which cloud providers a user has credentials for."""
    db = _get_db()
    providers = list_user_providers(db, user_id)
    return {"userId": user_id, "connectedProviders": providers}


# ─── Budget Endpoints ───

@router.post("/users/{user_id}/budget")
def set_user_budget(user_id: str, req: SetBudgetRequest):
    """Set budget allocations for a user."""
    db = _get_db()
    _ensure_user_exists(db, user_id)

    update = {"totalBudget": req.total_budget}
    cloud_data = db.collection("users").document(user_id).get().to_dict().get("cloudData", {})

    if req.aws_budget is not None and "aws" in cloud_data:
        cloud_data["aws"]["allocatedBudget"] = req.aws_budget
    if req.azure_budget is not None and "azure" in cloud_data:
        cloud_data["azure"]["allocatedBudget"] = req.azure_budget
    if req.gcp_budget is not None and "gcp" in cloud_data:
        cloud_data["gcp"]["allocatedBudget"] = req.gcp_budget

    update["cloudData"] = cloud_data
    db.collection("users").document(user_id).update(update)

    return {"ok": True, "userId": user_id, "totalBudget": req.total_budget}


# ─── Sync Endpoints ───

@router.post("/users/{user_id}/sync")
def sync_user_data(user_id: str, providers: Optional[List[str]] = None):
    """Trigger billing data sync for a specific user."""
    db = _get_db()
    result = sync_user(db, user_id, providers)
    if not result.get("ok"):
        raise HTTPException(status_code=404, detail=result.get("error", "Sync failed"))
    return result


@router.post("/users/{user_id}/sync/{provider}")
def sync_user_provider(user_id: str, provider: str):
    """Sync a single provider for a user."""
    if provider not in ("aws", "azure", "gcp"):
        raise HTTPException(status_code=400, detail="Provider must be aws, azure, or gcp")
    db = _get_db()
    result = sync_user(db, user_id, [provider])
    if not result.get("ok"):
        raise HTTPException(status_code=404, detail=result.get("error", "Sync failed"))
    return result


@router.post("/sync/all-users")
def sync_all_users_endpoint(
    x_sync_key: Optional[str] = Header(None, alias="X-Sync-Key"),
):
    """Sync all users (for scheduled jobs). Requires X-Sync-Key header."""
    expected = os.getenv("SYNC_API_KEY")
    if expected and (not x_sync_key or x_sync_key != expected):
        raise HTTPException(status_code=401, detail="Invalid X-Sync-Key")
    db = _get_db()
    return sync_all_users(db)


# ─── User Info ───

@router.get("/users/{user_id}")
def get_user_profile(user_id: str):
    """Get user profile with current spend data."""
    db = _get_db()
    doc = db.collection("users").document(user_id).get()
    if not doc.exists:
        raise HTTPException(status_code=404, detail=f"User {user_id} not found")

    data = doc.to_dict()
    providers = list_user_providers(db, user_id)
    total_spend = sum(c.get("currentSpend", 0) for c in data.get("cloudData", {}).values())

    return {
        "userId": user_id,
        "name": data.get("name"),
        "email": data.get("email"),
        "totalBudget": data.get("totalBudget"),
        "totalSpend": round(total_spend, 2),
        "connectedProviders": providers,
        "cloudData": data.get("cloudData"),
        "lastSyncAt": data.get("lastSyncAt"),
    }


def _ensure_user_exists(db, user_id: str):
    doc = db.collection("users").document(user_id).get()
    if not doc.exists:
        raise HTTPException(status_code=404, detail=f"User {user_id} not found")
