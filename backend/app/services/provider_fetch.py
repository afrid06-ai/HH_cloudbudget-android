"""Per-cloud spend fetchers: real APIs when credentials exist, else None (caller uses mock)."""
from __future__ import annotations

import os
from calendar import monthrange
from datetime import datetime, timedelta, timezone
from typing import Optional

USE_MOCK_FORCE = os.getenv("USE_MOCK_DATA", "true").lower() == "true"


def _month_bounds_utc() -> tuple[str, str]:
    """AWS CE-style dates: Start inclusive, End exclusive (month-to-date / full month)."""
    now = datetime.now(timezone.utc)
    start = now.replace(day=1, hour=0, minute=0, second=0, microsecond=0)
    _, last = monthrange(now.year, now.month)
    end = start + timedelta(days=last + 1)
    return start.strftime("%Y-%m-%d"), end.strftime("%Y-%m-%d")


def fetch_aws_spend_usd() -> Optional[float]:
    if USE_MOCK_FORCE or not os.getenv("AWS_ACCESS_KEY_ID"):
        return None
    try:
        import boto3

        start, end = _month_bounds_utc()
        client = boto3.client(
            "ce",
            region_name=os.getenv("AWS_REGION", "us-east-1"),
            aws_access_key_id=os.getenv("AWS_ACCESS_KEY_ID"),
            aws_secret_access_key=os.getenv("AWS_SECRET_ACCESS_KEY"),
        )
        resp = client.get_cost_and_usage(
            TimePeriod={"Start": start, "End": end},
            Granularity="MONTHLY",
            Metrics=["UnblendedCost"],
        )
        results = resp.get("ResultsByTime") or []
        if not results:
            return 0.0
        amt = results[0].get("Total", {}).get("UnblendedCost", {}).get("Amount")
        return round(float(amt or 0), 2)
    except Exception:
        return None


def fetch_azure_spend_usd() -> Optional[float]:
    if USE_MOCK_FORCE or not os.getenv("AZURE_SUBSCRIPTION_ID"):
        return None
    try:
        from azure.identity import ClientSecretCredential
        from azure.mgmt.costmanagement import CostManagementClient

        cred = ClientSecretCredential(
            tenant_id=os.environ["AZURE_TENANT_ID"],
            client_id=os.environ["AZURE_CLIENT_ID"],
            client_secret=os.environ["AZURE_CLIENT_SECRET"],
        )
        sub_id = os.environ["AZURE_SUBSCRIPTION_ID"]
        client = CostManagementClient(credential=cred)
        scope = f"/subscriptions/{sub_id}/"
        start, end = _month_bounds_utc()
        body = {
            "type": "ActualCost",
            "timeframe": "Custom",
            "timePeriod": {"from": f"{start}T00:00:00Z", "to": f"{end}T00:00:00Z"},
            "dataset": {
                "granularity": "None",
                "aggregation": {"totalCost": {"name": "PreTaxCost", "function": "Sum"}},
            },
        }
        result = client.query.usage(scope=scope, parameters=body)
        cols = {c.name: i for i, c in enumerate(result.columns)}
        rows = result.rows or []
        if not rows:
            return 0.0
        idx = cols.get("PreTaxCost")
        if idx is None:
            idx = cols.get("Cost", 0)
        return round(float(rows[0][idx] or 0), 2)
    except Exception:
        return None


def fetch_gcp_spend_usd() -> Optional[float]:
    """GCP list pricing/Catalog is not MTD spend; wire BigQuery billing export or Budgets API for production."""
    if USE_MOCK_FORCE:
        return None
    # Placeholder: extend with BigQuery billing table or Cloud Billing Reports.
    return None
