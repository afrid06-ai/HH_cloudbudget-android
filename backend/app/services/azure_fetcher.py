"""Azure Cost Management — fetch real billing data per user."""
from __future__ import annotations

import logging
from datetime import datetime, timezone
from typing import Any, Dict, Optional

logger = logging.getLogger(__name__)


def fetch_azure_full(creds: Dict[str, str]) -> Optional[Dict[str, Any]]:
    """
    Fetch comprehensive Azure billing using user's credentials.
    Requires: tenant_id, client_id, client_secret, subscription_id
    """
    try:
        from azure.identity import ClientSecretCredential
        from azure.mgmt.costmanagement import CostManagementClient

        credential = ClientSecretCredential(
            tenant_id=creds["tenant_id"],
            client_id=creds["client_id"],
            client_secret=creds["client_secret"],
        )
        sub_id = creds["subscription_id"]
        client = CostManagementClient(credential=credential)
        scope = f"/subscriptions/{sub_id}"

        now = datetime.now(timezone.utc)
        start = now.replace(day=1).strftime("%Y-%m-%dT00:00:00Z")
        end = now.strftime("%Y-%m-%dT23:59:59Z")

        result = {}

        # 1. Total spend this month
        body_total = {
            "type": "ActualCost",
            "timeframe": "Custom",
            "timePeriod": {"from": start, "to": end},
            "dataset": {
                "granularity": "None",
                "aggregation": {"totalCost": {"name": "PreTaxCost", "function": "Sum"}},
            },
        }
        resp = client.query.usage(scope=scope, parameters=body_total)
        cols = {c.name: i for i, c in enumerate(resp.columns)}
        rows = resp.rows or []
        total = 0.0
        if rows:
            idx = cols.get("PreTaxCost", cols.get("Cost", 0))
            total = float(rows[0][idx] or 0)
        result["currentSpend"] = round(total, 2)

        # 2. Spend by service (MeterCategory)
        body_svc = {
            "type": "ActualCost",
            "timeframe": "Custom",
            "timePeriod": {"from": start, "to": end},
            "dataset": {
                "granularity": "None",
                "aggregation": {"totalCost": {"name": "PreTaxCost", "function": "Sum"}},
                "grouping": [{"type": "Dimension", "name": "MeterCategory"}],
            },
        }
        resp_svc = client.query.usage(scope=scope, parameters=body_svc)
        cols_svc = {c.name: i for i, c in enumerate(resp_svc.columns)}
        services = {}
        cost_idx = cols_svc.get("PreTaxCost", cols_svc.get("Cost", 0))
        cat_idx = cols_svc.get("MeterCategory", 1)
        for row in (resp_svc.rows or []):
            svc_name = _shorten_azure_service(str(row[cat_idx]))
            amount = float(row[cost_idx] or 0)
            if amount > 0.01:
                services[svc_name] = round(services.get(svc_name, 0) + amount, 2)
        result["services"] = dict(sorted(services.items(), key=lambda x: -x[1])[:10])

        # 3. Daily spend (last 7 days)
        from datetime import timedelta
        d_start = (now - timedelta(days=6)).strftime("%Y-%m-%dT00:00:00Z")
        body_daily = {
            "type": "ActualCost",
            "timeframe": "Custom",
            "timePeriod": {"from": d_start, "to": end},
            "dataset": {
                "granularity": "Daily",
                "aggregation": {"totalCost": {"name": "PreTaxCost", "function": "Sum"}},
            },
        }
        resp_daily = client.query.usage(scope=scope, parameters=body_daily)
        cols_d = {c.name: i for i, c in enumerate(resp_daily.columns)}
        daily = []
        cost_d_idx = cols_d.get("PreTaxCost", cols_d.get("Cost", 0))
        date_d_idx = cols_d.get("UsageDate", 1)
        daily_totals = {}
        for row in (resp_daily.rows or []):
            date_val = str(row[date_d_idx])[:10]
            amount = float(row[cost_d_idx] or 0)
            daily.append({"date": date_val, "amount": round(amount, 2)})
            daily_totals[date_val] = round(amount, 2)
        result["dailySpend"] = daily
        result["dailyTotals"] = daily_totals

        logger.info(f"Azure fetch success: ${total:.2f} MTD, {len(services)} services")
        return result

    except Exception as e:
        logger.exception(f"Azure fetch failed: {e}")
        return None


def _shorten_azure_service(name: str) -> str:
    mapping = {
        "Virtual Machines": "VirtualMachines",
        "Storage": "BlobStorage",
        "Azure App Service": "AppService",
        "SQL Database": "AzureSQL",
        "Azure Cosmos DB": "CosmosDB",
        "Functions": "Functions",
        "Azure Monitor": "Monitor",
        "Bandwidth": "Bandwidth",
        "Azure Kubernetes Service": "AKS",
        "Virtual Network": "VNet",
        "Load Balancer": "LoadBalancer",
        "Azure DNS": "DNS",
    }
    for full, short in mapping.items():
        if full.lower() in name.lower():
            return short
    return name.replace(" ", "")
