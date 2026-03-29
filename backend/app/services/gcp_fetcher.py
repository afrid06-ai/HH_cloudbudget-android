"""GCP Cloud Billing — fetch real billing data per user."""
from __future__ import annotations

import logging
from datetime import datetime, timedelta, timezone
from typing import Any, Dict, Optional

logger = logging.getLogger(__name__)


def fetch_gcp_full(creds: Dict[str, str]) -> Optional[Dict[str, Any]]:
    """
    Fetch GCP billing data via BigQuery billing export.
    Requires: project_id, credentials_json (service account JSON as string)

    Note: GCP billing requires BigQuery billing export to be enabled.
    Go to: Billing > Billing export > BigQuery export > Enable
    """
    try:
        import json
        import tempfile
        from google.cloud import bigquery
        from google.oauth2 import service_account

        # Parse service account from stored credentials
        sa_info = json.loads(creds["credentials_json"])
        credentials = service_account.Credentials.from_service_account_info(sa_info)

        project_id = creds.get("project_id", sa_info.get("project_id", ""))
        dataset = creds.get("billing_dataset", "billing_export")
        table = creds.get("billing_table", "gcp_billing_export_v1")

        client = bigquery.Client(credentials=credentials, project=project_id)

        now = datetime.now(timezone.utc)
        start = now.replace(day=1).strftime("%Y-%m-%d")
        end = now.strftime("%Y-%m-%d")

        result = {}

        # 1. Total spend this month
        query_total = f"""
            SELECT ROUND(SUM(cost), 2) as total_cost
            FROM `{project_id}.{dataset}.{table}`
            WHERE usage_start_time >= '{start}'
            AND usage_start_time < '{end}'
        """
        rows = list(client.query(query_total).result())
        total = float(rows[0].total_cost or 0) if rows else 0.0
        result["currentSpend"] = round(total, 2)

        # 2. Spend by service
        query_svc = f"""
            SELECT service.description as service_name,
                   ROUND(SUM(cost), 2) as cost
            FROM `{project_id}.{dataset}.{table}`
            WHERE usage_start_time >= '{start}'
            AND usage_start_time < '{end}'
            GROUP BY service_name
            HAVING cost > 0.01
            ORDER BY cost DESC
            LIMIT 10
        """
        services = {}
        for row in client.query(query_svc).result():
            short = _shorten_gcp_service(row.service_name)
            services[short] = round(float(row.cost), 2)
        result["services"] = services

        # 3. Daily spend (last 7 days)
        d_start = (now - timedelta(days=6)).strftime("%Y-%m-%d")
        query_daily = f"""
            SELECT DATE(usage_start_time) as date,
                   ROUND(SUM(cost), 2) as daily_cost
            FROM `{project_id}.{dataset}.{table}`
            WHERE usage_start_time >= '{d_start}'
            AND usage_start_time < '{end}'
            GROUP BY date
            ORDER BY date
        """
        daily = []
        daily_totals = {}
        for row in client.query(query_daily).result():
            date_str = str(row.date)
            amount = round(float(row.daily_cost), 2)
            daily.append({"date": date_str, "amount": amount})
            daily_totals[date_str] = amount
        result["dailySpend"] = daily
        result["dailyTotals"] = daily_totals

        logger.info(f"GCP fetch success: ${total:.2f} MTD, {len(services)} services")
        return result

    except Exception as e:
        logger.exception(f"GCP fetch failed: {e}")
        return None


def _shorten_gcp_service(name: str) -> str:
    mapping = {
        "Compute Engine": "ComputeEngine",
        "Cloud Storage": "CloudStorage",
        "BigQuery": "BigQuery",
        "Cloud SQL": "CloudSQL",
        "Cloud Functions": "CloudFunctions",
        "Cloud Run": "CloudRun",
        "Kubernetes Engine": "GKE",
        "Cloud Pub/Sub": "PubSub",
        "Cloud Monitoring": "CloudMonitoring",
        "Cloud Logging": "CloudLogging",
        "Networking": "Networking",
        "Cloud DNS": "CloudDNS",
        "Firestore": "Firestore",
        "Cloud Spanner": "Spanner",
        "App Engine": "AppEngine",
    }
    for full, short in mapping.items():
        if full.lower() in name.lower():
            return short
    return name.replace(" ", "")
