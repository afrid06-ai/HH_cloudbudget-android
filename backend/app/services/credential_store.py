"""Per-user encrypted credential storage in Firestore."""
from __future__ import annotations

import base64
import json
import os
from typing import Any, Dict, Optional

from cryptography.fernet import Fernet

# Encryption key — generate once: python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"
# Store in .env as CREDENTIAL_ENCRYPTION_KEY
_KEY = os.getenv("CREDENTIAL_ENCRYPTION_KEY")


def _fernet() -> Fernet:
    if not _KEY:
        raise RuntimeError("Set CREDENTIAL_ENCRYPTION_KEY in .env (use Fernet.generate_key())")
    return Fernet(_KEY.encode() if isinstance(_KEY, str) else _KEY)


def encrypt_credentials(creds: Dict[str, str]) -> str:
    """Encrypt a dict of credentials → base64 string for Firestore storage."""
    raw = json.dumps(creds).encode()
    return _fernet().encrypt(raw).decode()


def decrypt_credentials(encrypted: str) -> Dict[str, str]:
    """Decrypt stored credentials back to dict."""
    raw = _fernet().decrypt(encrypted.encode())
    return json.loads(raw.decode())


def store_user_credentials(db, user_id: str, provider: str, creds: Dict[str, str]) -> None:
    """Encrypt and store credentials in Firestore: users/{userId}/credentials/{provider}."""
    encrypted = encrypt_credentials(creds)
    db.collection("users").document(user_id).collection("credentials").document(provider).set({
        "provider": provider,
        "encrypted": encrypted,
        "fields": list(creds.keys()),  # Store field names (not values) for UI display
        "updatedAt": _server_timestamp(),
    })


def get_user_credentials(db, user_id: str, provider: str) -> Optional[Dict[str, str]]:
    """Retrieve and decrypt credentials for a user+provider. Returns None if not found."""
    doc = db.collection("users").document(user_id).collection("credentials").document(provider).get()
    if not doc.exists:
        return None
    data = doc.to_dict()
    encrypted = data.get("encrypted")
    if not encrypted:
        return None
    try:
        return decrypt_credentials(encrypted)
    except Exception:
        return None


def delete_user_credentials(db, user_id: str, provider: str) -> None:
    """Remove stored credentials for a provider."""
    db.collection("users").document(user_id).collection("credentials").document(provider).delete()


def list_user_providers(db, user_id: str) -> list[str]:
    """Return list of providers that have stored credentials."""
    docs = db.collection("users").document(user_id).collection("credentials").stream()
    return [doc.id for doc in docs]


def _server_timestamp():
    try:
        from google.cloud.firestore_v1 import SERVER_TIMESTAMP
        return SERVER_TIMESTAMP
    except ImportError:
        from datetime import datetime, timezone
        return datetime.now(timezone.utc).isoformat()
