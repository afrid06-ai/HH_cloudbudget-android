"""Build Android-friendly Firestore documents from cloud spends + demo waste/trends."""
from __future__ import annotations

import os
import uuid
from datetime import datetime
from typing import Any, Dict, Literal, Optional, Tuple

from app.services.mock_data import get_mock_dashboard, get_mock_trends, get_mock_waste
from app.services.provider_fetch import fetch_aws_spend_usd, fetch_azure_spend_usd, fetch_gcp_spend_usd

try:
    import firebase_admin
    from firebase_admin import credentials, firestore
except ImportError:  # pragma: no cover
    firebase_admin = None  # type: ignore
    credentials = None  # type: ignore
    firestore = None  # type: ignore

TOTAL_BUDGET = float(os.getenv("DEFAULT_TOTAL_BUDGET", "500"))


def _ensure_firebase():
    if firebase_admin is None:
        raise RuntimeError("firebase-admin is not installed")
    path = os.getenv("FIREBASE_CREDENTIALS_PATH") or os.getenv("GOOGLE_APPLICATION_CREDENTIALS")
    if not path or not os.path.isfile(path):
        raise RuntimeError(
            "Set FIREBASE_CREDENTIALS_PATH (or GOOGLE_APPLICATION_CREDENTIALS) to a service account JSON with Firestore access."
        )
    try:
        firebase_admin.get_app()
    except ValueError:
        firebase_admin.initialize_app(credentials.Certificate(path))
    return firestore.client()


def _mock_triple() -> dict[str, float]:
    dash = get_mock_dashboard()
    out = {"aws": 0.0, "azure": 0.0, "gcp": 0.0}
    for b in dash.get("breakdown", []):
        p = b.get("provider", "")
        if p in out:
            out[p] = float(b.get("amount", 0))
    return out


def _current_spends_from_firestore(db) -> Optional[Dict[str, float]]:
    doc = db.collection("dashboard").document("current").get()
    if not doc.exists:
        return None
    d = doc.to_dict() or {}
    return {
        "aws": float(d.get("awsSpend", 0) or 0),
        "azure": float(d.get("azureSpend", 0) or 0),
        "gcp": float(d.get("gcpSpend", 0) or 0),
    }


def _pct_change(prev: float, new: float) -> float:
    if prev <= 0:
        return 0.0 if abs(new - prev) < 0.01 else round(min(99.0, max(-99.0, new - prev)), 1)
    return round(((new - prev) / prev) * 100.0, 1)


def _build_dashboard_payload(
    aws: float,
    azure: float,
    gcp: float,
    prev: Optional[Dict[str, float]],
) -> Dict[str, Any]:
    total = round(aws + azure + gcp, 2)
    pbudget = TOTAL_BUDGET
    prev = prev or {}
    return {
        "totalSpend": total,
        "awsSpend": aws,
        "azureSpend": azure,
        "gcpSpend": gcp,
        "awsChange": _pct_change(prev.get("aws", aws), aws),
        "azureChange": _pct_change(prev.get("azure", azure), azure),
        "gcpChange": _pct_change(prev.get("gcp", gcp), gcp),
        "cloudsConnected": 3,
        "overBudget": total > pbudget,
    }


def _build_budget_payload(aws: float, azure: float, gcp: float) -> Dict[str, Any]:
    ratios = {"aws": 0.45, "azure": 0.30, "gcp": 0.25}
    return {
        "totalBudget": TOTAL_BUDGET,
        "awsAllocated": round(TOTAL_BUDGET * ratios["aws"], 2),
        "awsSpent": aws,
        "azureAllocated": round(TOTAL_BUDGET * ratios["azure"], 2),
        "azureSpent": azure,
        "gcpAllocated": round(TOTAL_BUDGET * ratios["gcp"], 2),
        "gcpSpent": gcp,
    }


def _mock_waste_firestore() -> Dict[str, Any]:
    raw = get_mock_waste()
    items_out = []
    for it in raw.get("items", []):
        items_out.append(
            {
                "provider": it.get("provider", ""),
                "resourceName": it.get("resource_name") or it.get("resource_id", ""),
                "description": it.get("waste_reason", ""),
                "monthlySaving": round(float(it.get("monthly_cost", 0)), 2),
                "recommendation": it.get("recommendation", ""),
            }
        )
    return {"totalWaste": raw.get("total_waste", 0.0), "items": items_out}


def _mock_trends_firestore() -> Dict[str, Any]:
    raw = get_mock_trends(7)
    days = []
    for d in raw.get("daily_spends", []):
        label = d.get("date", "")
        if isinstance(label, str) and len(label) >= 10:
            try:
                dt = datetime.fromisoformat(label[:10])
                label = dt.strftime("%a")
            except ValueError:
                pass
        days.append(
            {
                "date": label,
                "aws": float(d.get("aws", 0)),
                "azure": float(d.get("azure", 0)),
                "gcp": float(d.get("gcp", 0)),
            }
        )
    daily_total = sum(
        float(d.get("aws", 0)) + float(d.get("azure", 0)) + float(d.get("gcp", 0)) for d in days
    )
    avg = round(daily_total / max(len(days), 1), 2)
    projected = round(avg * 30, 2)
    return {"dailySpends": days, "avgDaily": avg, "projected": projected}


def _alerts_for_spends(aws: float, azure: float, gcp: float):
    alerts = []  # type: list[Tuple[str, Dict[str, Any]]]
    alloc = {"aws": TOTAL_BUDGET * 0.45, "azure": TOTAL_BUDGET * 0.30, "gcp": TOTAL_BUDGET * 0.25}
    spent = {"aws": aws, "azure": azure, "gcp": gcp}
    total = aws + azure + gcp
    if total > TOTAL_BUDGET:
        alerts.append(
            (
                "alert_over_budget",
                {
                    "severity": "critical",
                    "title": "Over monthly budget",
                    "description": f"Total spend ${total:.2f} exceeds budget ${TOTAL_BUDGET:.2f}",
                    "provider": "all",
                    "timeAgo": "just now",
                },
            )
        )
    for pname in ("aws", "azure", "gcp"):
        if spent[pname] > alloc[pname] * 0.9:
            alerts.append(
                (
                    f"alert_{pname}_{uuid.uuid4().hex[:8]}",
                    {
                        "severity": "warning",
                        "title": f"{pname.upper()} budget warning",
                        "description": f"{pname.upper()} spent ${spent[pname]:.2f} of ${alloc[pname]:.2f} allocated",
                        "provider": pname,
                        "timeAgo": "just now",
                    },
                )
            )
    if not alerts:
        alerts.append(
            (
                "alert_ok",
                {
                    "severity": "ok",
                    "title": "Spend on track",
                    "description": "No budget issues detected on last sync.",
                    "provider": "all",
                    "timeAgo": "just now",
                },
            )
        )
    return alerts[:8]


def _pick_value(live: Optional[float], mock_val: float) -> Tuple[float, str]:
    if live is not None:
        return float(live), "live"
    return float(mock_val), "simulated"


def _resolve_spends(
    db,
    mode: Literal["aws", "azure", "gcp", "all"],
) -> Tuple[Dict[str, float], Optional[Dict[str, float]], Dict[str, str]]:
    prev_doc = _current_spends_from_firestore(db)
    mock = _mock_triple()
    provenance: dict[str, str] = {"aws": "simulated", "azure": "simulated", "gcp": "simulated"}

    if mode == "all":
        aws, provenance["aws"] = _pick_value(fetch_aws_spend_usd(), mock["aws"])
        azure, provenance["azure"] = _pick_value(fetch_azure_spend_usd(), mock["azure"])
        gcp, provenance["gcp"] = _pick_value(fetch_gcp_spend_usd(), mock["gcp"])
        return {"aws": aws, "azure": azure, "gcp": gcp}, prev_doc, provenance

    base = dict(prev_doc or mock)
    if mode == "aws":
        base["aws"], provenance["aws"] = _pick_value(fetch_aws_spend_usd(), mock["aws"])
        provenance["azure"] = "unchanged" if prev_doc else "simulated"
        provenance["gcp"] = "unchanged" if prev_doc else "simulated"
    elif mode == "azure":
        base["azure"], provenance["azure"] = _pick_value(fetch_azure_spend_usd(), mock["azure"])
        provenance["aws"] = "unchanged" if prev_doc else "simulated"
        provenance["gcp"] = "unchanged" if prev_doc else "simulated"
    else:
        base["gcp"], provenance["gcp"] = _pick_value(fetch_gcp_spend_usd(), mock["gcp"])
        provenance["aws"] = "unchanged" if prev_doc else "simulated"
        provenance["azure"] = "unchanged" if prev_doc else "simulated"

    # Fill missing keys from mock if no prior Firestore doc
    if not prev_doc:
        for k in ("aws", "azure", "gcp"):
            if provenance[k] == "unchanged":
                base[k] = mock[k]
                provenance[k] = "simulated"

    return base, prev_doc, provenance


def sync_to_firestore(mode: Literal["aws", "azure", "gcp", "all"] = "all") -> Dict[str, Any]:
    db = _ensure_firebase()
    spends, prev, provenance = _resolve_spends(db, mode)
    aws, azure, gcp = spends["aws"], spends["azure"], spends["gcp"]

    dashboard = _build_dashboard_payload(aws, azure, gcp, prev)
    budget = _build_budget_payload(aws, azure, gcp)
    waste = _mock_waste_firestore()
    trends = _mock_trends_firestore()

    batch = db.batch()
    batch.set(db.collection("dashboard").document("current"), dashboard, merge=False)
    batch.set(db.collection("budget").document("current"), budget, merge=False)
    batch.set(db.collection("waste").document("current"), waste, merge=False)
    batch.set(db.collection("trends").document("current"), trends, merge=False)
    batch.commit()

    for doc in db.collection("alerts").stream():
        doc.reference.delete()
    for doc_id, payload in _alerts_for_spends(aws, azure, gcp):
        db.collection("alerts").document(doc_id).set(payload)

    return {
        "ok": True,
        "mode": mode,
        "spends": spends,
        "provenance": provenance,
        "updated": ["dashboard/current", "budget/current", "waste/current", "trends/current", "alerts"],
    }
