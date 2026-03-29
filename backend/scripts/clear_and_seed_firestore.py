#!/usr/bin/env python3
"""
1) Delete Cloud Firestore data this app uses (users + legacy flat collections).
2) Write fresh demo users (user_001, user_002) from app.data.user_profiles.

Uses Admin SDK — bypasses security rules.

  export FIREBASE_CREDENTIALS_PATH="/path/to/serviceAccount.json"
  python scripts/clear_and_seed_firestore.py

Optional: only remove/seed users (keep other collections if any):
  python scripts/clear_and_seed_firestore.py --users-only
"""
from __future__ import annotations

import argparse
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))

from dotenv import load_dotenv

load_dotenv(ROOT / ".env")

# Collections we created for CloudBudget; safe to wipe for a clean slate.
ALL_APP_COLLECTIONS = ("users", "dashboard", "budget", "waste", "trends", "alerts")


def _delete_collection(db, coll_id: str, batch_size: int = 300) -> int:
    """Delete all documents in a top-level collection. Returns count deleted."""
    ref = db.collection(coll_id)
    deleted = 0
    while True:
        docs = list(ref.limit(batch_size).stream())
        if not docs:
            break
        batch = db.batch()
        for doc in docs:
            batch.delete(doc.reference)
            deleted += 1
        batch.commit()
    return deleted


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--users-only",
        action="store_true",
        help="Only touch the users collection (delete all user docs, then seed user_001/user_002).",
    )
    args = parser.parse_args()

    path = os.getenv("FIREBASE_CREDENTIALS_PATH") or os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if not path or not os.path.isfile(path):
        print("Set FIREBASE_CREDENTIALS_PATH to your Firebase service account JSON.")
        sys.exit(1)

    import firebase_admin
    from firebase_admin import credentials, firestore

    from app.data.user_profiles import all_demo_users

    if not firebase_admin._apps:
        firebase_admin.initialize_app(credentials.Certificate(path))
    db = firestore.client()

    targets = ("users",) if args.users_only else ALL_APP_COLLECTIONS

    print("Deleting collections:", ", ".join(targets))
    total_del = 0
    for coll_id in targets:
        n = _delete_collection(db, coll_id)
        total_del += n
        print(f"  {coll_id}: removed {n} document(s)")

    users = all_demo_users()
    batch = db.batch()
    for uid, profile in users.items():
        ref = db.collection("users").document(uid)
        batch.set(ref, profile, merge=False)
    batch.commit()

    print("Seeded:", ", ".join(users.keys()))
    print("Done. Refresh Firebase Console → Firestore → Data.")


if __name__ == "__main__":
    main()
