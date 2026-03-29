"""Per-user cloud billing sync — fetches real data and writes to Firestore users/{userId}."""
from __future__ import annotations

import logging
from datetime import datetime, timedelta, timezone
from typing import Any, Dict, List, Optional

from app.services.credential_store import get_user_credentials, list_user_providers
from app.services.aws_fetcher import fetch_aws_full
from app.services.azure_fetcher import fetch_azure_full
from app.services.gcp_fetcher import fetch_gcp_full

logger = logging.getLogger(__name__)

# Provider fetcher mapping
FETCHERS = {
    "aws": fetch_aws_full,
    "azure": fetch_azure_full,
    "gcp": fetch_gcp_full,
}

# Required credential fields per provider
REQUIRED_FIELDS = {
    "aws": ["access_key_id", "secret_access_key"],
    "azure": ["tenant_id", "client_id", "client_secret", "subscription_id"],
    "gcp": ["project_id", "credentials_json"],
}


def sync_user(db, user_id: str, providers: Optional[List[str]] = None) -> Dict[str, Any]:
    """
    Sync billing data for a single user.
    - Reads their encrypted credentials from Firestore
    - Fetches real data from each cloud provider
    - Updates the user's Firestore document with fresh data
    """
    user_ref = db.collection("users").document(user_id)
    user_doc = user_ref.get()
    if not user_doc.exists:
        return {"ok": False, "error": f"User {user_id} not found"}

    user_data = user_doc.to_dict()
    connected_providers = list_user_providers(db, user_id)

    if providers is None:
        providers = connected_providers

    results = {}
    cloud_data = user_data.get("cloudData", {})
    daily_spend = user_data.get("dailySpend", {})
    alerts_data = user_data.get("alerts", {})

    for provider in providers:
        if provider not in connected_providers:
            results[provider] = {"status": "no_credentials", "source": "demo"}
            continue

        creds = get_user_credentials(db, user_id, provider)
        if not creds:
            results[provider] = {"status": "decrypt_failed", "source": "demo"}
            continue

        # Fetch real data
        fetcher = FETCHERS.get(provider)
        if not fetcher:
            results[provider] = {"status": "unsupported", "source": "demo"}
            continue

        live_data = fetcher(creds)
        if live_data is None:
            results[provider] = {"status": "fetch_failed", "source": "demo"}
            continue

        # Update cloudData for this provider
        old_spend = cloud_data.get(provider, {}).get("currentSpend", 0)
        allocated = cloud_data.get(provider, {}).get("allocatedBudget", 0)
        new_spend = live_data["currentSpend"]

        cloud_data[provider] = {
            "provider": provider,
            "currentSpend": new_spend,
            "allocatedBudget": allocated if allocated > 0 else _default_budget(provider, user_data.get("totalBudget", 500)),
            "overBudget": new_spend > (allocated if allocated > 0 else _default_budget(provider, user_data.get("totalBudget", 500))),
            "services": live_data.get("services", cloud_data.get(provider, {}).get("services", {})),
        }

        # Update daily spend with real data
        for day_data in live_data.get("dailySpend", []):
            date = day_data["date"]
            if date not in daily_spend:
                daily_spend[date] = {"date": date, "aws": 0, "azure": 0, "gcp": 0, "total": 0}
            daily_spend[date][provider] = day_data["amount"]
            # Recalculate total
            daily_spend[date]["total"] = round(
                sum(daily_spend[date].get(p, 0) for p in ["aws", "azure", "gcp"]), 2
            )

        # Generate alerts based on real data
        alerts_data = _generate_alerts(cloud_data, user_data.get("totalBudget", 500), old_spend, provider, alerts_data)

        results[provider] = {
            "status": "success",
            "source": "live",
            "currentSpend": new_spend,
            "services": len(live_data.get("services", {})),
            "dailyDays": len(live_data.get("dailySpend", [])),
        }

    # Keep only last 7 days of daily spend
    sorted_dates = sorted(daily_spend.keys(), reverse=True)[:7]
    daily_spend = {d: daily_spend[d] for d in sorted(sorted_dates)}

    # Generate waste insights from real service data
    waste_insights = _generate_waste_insights(cloud_data)

    # Write everything back to Firestore
    update_data = {
        "cloudData": cloud_data,
        "dailySpend": daily_spend,
        "alerts": alerts_data,
        "wasteInsights": waste_insights,
        "lastSyncAt": datetime.now(timezone.utc).isoformat(),
    }
    user_ref.update(update_data)

    # Also update flat collections for backward compatibility
    _sync_flat_collections(db, user_data, cloud_data, daily_spend, alerts_data, waste_insights)

    total_spend = sum(c.get("currentSpend", 0) for c in cloud_data.values())
    logger.info(f"User {user_id} synced: ${total_spend:.2f} total — {results}")

    return {
        "ok": True,
        "userId": user_id,
        "totalSpend": round(total_spend, 2),
        "providers": results,
    }


def sync_all_users(db) -> Dict[str, Any]:
    """Sync all users that have credentials stored."""
    results = {}
    users = db.collection("users").stream()
    for doc in users:
        user_id = doc.id
        providers = list_user_providers(db, user_id)
        if providers:
            results[user_id] = sync_user(db, user_id)
        else:
            results[user_id] = {"ok": True, "skipped": True, "reason": "no_credentials"}
    return results


def _default_budget(provider: str, total: float) -> float:
    ratios = {"aws": 0.45, "azure": 0.30, "gcp": 0.25}
    return round(total * ratios.get(provider, 0.33), 2)


def _generate_alerts(
    cloud_data: Dict, total_budget: float, old_spend: float, provider: str, existing_alerts: Dict
) -> Dict:
    """Generate smart alerts based on real billing data."""
    alerts = dict(existing_alerts)  # Keep existing, add/update
    now_str = datetime.now(timezone.utc).isoformat()

    total_spend = sum(c.get("currentSpend", 0) for c in cloud_data.values())

    # Check each provider for over-budget
    for p, data in cloud_data.items():
        spend = data.get("currentSpend", 0)
        budget = data.get("allocatedBudget", 0)

        if budget > 0:
            pct = (spend / budget) * 100

            if pct >= 100:
                alerts[f"alert_{p}_over"] = {
                    "title": f"{p.upper()} Over Budget",
                    "message": f"{p.upper()} has exceeded its ${budget:.0f} budget by ${spend - budget:.2f}.",
                    "type": "over_budget",
                    "severity": "critical",
                    "provider": p,
                    "isRead": False,
                    "createdAt": now_str,
                }
            elif pct >= 85:
                alerts[f"alert_{p}_warning"] = {
                    "title": f"{p.upper()} Budget Warning",
                    "message": f"{p.upper()} has used {pct:.1f}% of its ${budget:.0f} budget.",
                    "type": "budget_warning",
                    "severity": "warning",
                    "provider": p,
                    "isRead": False,
                    "createdAt": now_str,
                }

    # Spend spike detection
    new_spend = cloud_data.get(provider, {}).get("currentSpend", 0)
    if old_spend > 0:
        change_pct = ((new_spend - old_spend) / old_spend) * 100
        if change_pct > 15:
            alerts[f"alert_{provider}_spike"] = {
                "title": f"{provider.upper()} Spend Spike",
                "message": f"{provider.upper()} spend increased by {change_pct:.1f}% since last sync.",
                "type": "spend_spike",
                "severity": "warning",
                "provider": provider,
                "isRead": False,
                "createdAt": now_str,
            }

    # Total budget check
    if total_budget > 0:
        total_pct = (total_spend / total_budget) * 100
        if total_pct >= 95:
            alerts["alert_total_critical"] = {
                "title": "Total Budget Critical",
                "message": f"Overall spend at {total_pct:.1f}% of ${total_budget:.0f} budget.",
                "type": "budget_warning",
                "severity": "critical",
                "provider": "all",
                "isRead": False,
                "createdAt": now_str,
            }
        elif total_pct >= 80:
            alerts["alert_total_warning"] = {
                "title": "Approaching Budget Limit",
                "message": f"You have used {total_pct:.1f}% of your ${total_budget:.0f} monthly budget.",
                "type": "budget_warning",
                "severity": "warning",
                "provider": "all",
                "isRead": False,
                "createdAt": now_str,
            }

    return alerts


def _generate_waste_insights(cloud_data: Dict) -> Dict:
    """Generate waste insights by analyzing service spend patterns."""
    items = []
    total_waste = 0.0

    for provider, data in cloud_data.items():
        services = data.get("services", {})
        if not services:
            continue

        # Find the most expensive service — flag potential optimization
        sorted_svcs = sorted(services.items(), key=lambda x: -x[1])
        if sorted_svcs:
            top_svc, top_cost = sorted_svcs[0]
            total_spend = data.get("currentSpend", 0)

            if total_spend > 0 and (top_cost / total_spend) > 0.35:
                saving = round(top_cost * 0.15, 2)  # Estimate 15% savings
                total_waste += saving
                items.append({
                    "provider": provider,
                    "resourceName": top_svc,
                    "description": f"Largest spend item at ${top_cost:.2f} ({top_cost/total_spend*100:.0f}% of {provider.upper()} total). Review for optimization.",
                    "monthlySaving": saving,
                    "recommendation": _get_recommendation(provider, top_svc),
                })

            # Check for low-cost services that might be orphaned
            for svc, cost in sorted_svcs:
                if cost < 5.0 and cost > 0.5:
                    saving = round(cost * 0.5, 2)
                    total_waste += saving
                    items.append({
                        "provider": provider,
                        "resourceName": svc,
                        "description": f"Low spend (${cost:.2f}/mo) — verify if still needed.",
                        "monthlySaving": saving,
                        "recommendation": f"Review if {svc} is actively used. Consider removing if unused.",
                    })
                    break  # Only 1 low-cost flag per provider

    return {
        "totalWaste": round(total_waste, 2),
        "items": items[:6],  # Cap at 6 items
    }


def _get_recommendation(provider: str, service: str) -> str:
    recs = {
        ("aws", "EC2"): "Right-size instances, use Savings Plans, or switch to Graviton.",
        ("aws", "RDS"): "Check for idle instances, consider Aurora Serverless for variable loads.",
        ("aws", "S3"): "Enable Intelligent-Tiering, lifecycle policies for old objects.",
        ("aws", "Lambda"): "Optimize memory settings, reduce cold starts with provisioned concurrency.",
        ("azure", "VirtualMachines"): "Use Reserved Instances, enable auto-shutdown for dev/test.",
        ("azure", "AzureSQL"): "Consider serverless tier, review DTU/vCore sizing.",
        ("azure", "BlobStorage"): "Move cold data to Archive tier, set lifecycle management rules.",
        ("gcp", "ComputeEngine"): "Use committed use discounts, preemptible VMs for batch jobs.",
        ("gcp", "BigQuery"): "Use partitioned tables, set bytes-billed limits, consider flat-rate.",
        ("gcp", "CloudSQL"): "Right-size instance, enable storage auto-resize, check HA necessity.",
    }
    return recs.get((provider, service), f"Review {service} usage and consider rightsizing or reserved pricing.")


def _sync_flat_collections(db, user_data, cloud_data, daily_spend, alerts, waste):
    """Update legacy flat collections for backward compatibility with older app versions."""
    from datetime import datetime

    total_spend = sum(c.get("currentSpend", 0) for c in cloud_data.values())
    total_budget = user_data.get("totalBudget", 500)

    # Dashboard
    sorted_days = sorted(daily_spend.keys())
    aws_change = azure_change = gcp_change = 0.0
    if len(sorted_days) >= 2:
        prev_day = daily_spend[sorted_days[-2]]
        last_day = daily_spend[sorted_days[-1]]
        for p, var in [("aws", "aws_change"), ("azure", "azure_change"), ("gcp", "gcp_change")]:
            prev_val = prev_day.get(p, 0)
            last_val = last_day.get(p, 0)
            if prev_val > 0:
                locals()[var] = round(((last_val - prev_val) / prev_val) * 100, 1)

    db.collection("dashboard").document("current").set({
        "totalSpend": round(total_spend, 2),
        "awsSpend": cloud_data.get("aws", {}).get("currentSpend", 0),
        "azureSpend": cloud_data.get("azure", {}).get("currentSpend", 0),
        "gcpSpend": cloud_data.get("gcp", {}).get("currentSpend", 0),
        "awsChange": aws_change,
        "azureChange": azure_change,
        "gcpChange": gcp_change,
        "cloudsConnected": len([c for c in cloud_data.values() if c.get("currentSpend", 0) > 0]),
        "overBudget": total_spend > total_budget,
    })

    # Budget
    db.collection("budget").document("current").set({
        "totalBudget": total_budget,
        "awsAllocated": cloud_data.get("aws", {}).get("allocatedBudget", 0),
        "awsSpent": cloud_data.get("aws", {}).get("currentSpend", 0),
        "azureAllocated": cloud_data.get("azure", {}).get("allocatedBudget", 0),
        "azureSpent": cloud_data.get("azure", {}).get("currentSpend", 0),
        "gcpAllocated": cloud_data.get("gcp", {}).get("allocatedBudget", 0),
        "gcpSpent": cloud_data.get("gcp", {}).get("currentSpend", 0),
    })

    # Waste
    db.collection("waste").document("current").set(waste)

    # Trends
    sorted_daily = sorted(daily_spend.values(), key=lambda d: d.get("date", ""))
    totals = [d.get("total", 0) for d in sorted_daily]
    avg = round(sum(totals) / max(len(totals), 1), 2)
    db.collection("trends").document("current").set({
        "avgDaily": avg,
        "projected": round(avg * 30, 2),
        "dailySpends": [
            {"date": d["date"][-5:].replace("-", "/"), "aws": d.get("aws", 0), "azure": d.get("azure", 0), "gcp": d.get("gcp", 0)}
            for d in sorted_daily
        ],
    })

    # Alerts
    for doc in db.collection("alerts").stream():
        doc.reference.delete()
    for aid, alert in list(alerts.items())[:5]:
        db.collection("alerts").document(aid).set({
            "severity": alert.get("severity", "warning"),
            "title": alert["title"],
            "description": alert.get("message", ""),
            "provider": alert.get("provider", "all"),
            "timeAgo": "just now",
        })
