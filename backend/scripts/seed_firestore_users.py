#!/usr/bin/env python3
"""
Push demo user documents into Cloud Firestore for project CloudBudget (cloudbudget-6271a).

Prerequisites
-------------
1. Firebase Console → ⚙ Project settings → Service accounts → Generate new private key (JSON).
2. Save the file OUTSIDE git, e.g.  ~/secrets/cloudbudget-firebase-admin.json
3. From the backend folder:

   pip install firebase-admin python-dotenv
   export FIREBASE_CREDENTIALS_PATH="/full/path/to/your-key.json"
   python scripts/seed_firestore_users.py

   Or put FIREBASE_CREDENTIALS_PATH in backend/.env

Creates:  users/user_001  and  users/user_002  (cloudData, dailySpend, alerts, wasteInsights, …).

If the console shows “Missing permissions”, use a key from the same Firebase project as the app’s
google-services.json, and in Firestore Rules allow access for your demo (or test mode).
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
    path = os.getenv("FIREBASE_CREDENTIALS_PATH") or os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if not path or not os.path.isfile(path):
        print(
            "ERROR: Point FIREBASE_CREDENTIALS_PATH or GOOGLE_APPLICATION_CREDENTIALS "
            "at your Firebase service account JSON file.\n"
            "Firebase Console → Project settings → Service accounts → Generate new private key."
        )
        sys.exit(1)

    import firebase_admin
    from firebase_admin import credentials, firestore

    from app.data.user_profiles import all_demo_users

    cred = credentials.Certificate(path)
    firebase_admin.initialize_app(cred)
    db = firestore.client()
    users = all_demo_users()
    batch = db.batch()
    for uid, profile in users.items():
        ref = db.collection("users").document(uid)
        batch.set(ref, profile, merge=False)
    batch.commit()
    print("OK — wrote documents:", ", ".join(users.keys()))
    print("  Collection: users")
    for uid in users:
        print(f"    users/{uid}")


if __name__ == "__main__":
    main()
