#!/usr/bin/env python3
"""
Verify Firestore data is readable (uses Admin SDK — bypasses security rules).

To test RULES as the Android app sees them:
- Firebase Console → Firestore → Rules → Rules Playground, or
- Run the Android app and watch Logcat for Firestore errors.

Usage:
  export FIREBASE_CREDENTIALS_PATH="/path/to/serviceAccount.json"
  python scripts/test_firestore_read.py [user_001|user_002]
"""
from __future__ import annotations

import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from dotenv import load_dotenv

load_dotenv(ROOT / ".env")


def main() -> None:
    uid = sys.argv[1] if len(sys.argv) > 1 else "user_001"
    path = os.getenv("FIREBASE_CREDENTIALS_PATH") or os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if not path or not os.path.isfile(path):
        print("Set FIREBASE_CREDENTIALS_PATH to your Firebase service account JSON.")
        sys.exit(1)

    import firebase_admin
    from firebase_admin import credentials, firestore

    if not firebase_admin._apps:
        firebase_admin.initialize_app(credentials.Certificate(path))
    db = firestore.client()
    doc = db.collection("users").document(uid).get()
    if not doc.exists:
        print(f"FAIL: users/{uid} does not exist. Run: python scripts/seed_firestore_users.py")
        sys.exit(2)
    data = doc.to_dict() or {}
    name = data.get("name", "?")
    cloud = data.get("cloudData")
    print(f"OK: users/{uid} exists (name={name!r})")
    if cloud:
        print("     cloudData keys:", list(cloud.keys()))
    else:
        print("     WARNING: no cloudData field")


if __name__ == "__main__":
    main()
