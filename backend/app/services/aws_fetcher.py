"""AWS Cost Explorer — fetch real billing data per user."""
from __future__ import annotations

import logging
from calendar import monthrange
from datetime import datetime, timedelta, timezone
from typing import Any, Dict, List, Optional, Tuple

logger = logging.getLogger(__name__)


def _month_bounds() -> Tuple[str, str]:
    now = datetime.now(timezone.utc)
    start = now.replace(day=1).strftime("%Y-%m-%d")
    _, last = monthrange(now.year, now.month)
    end = (now.replace(day=1) + timedelta(days=last)).strftime("%Y-%m-%d")
    return start, end


def _week_bounds() -> Tuple[str, str]:
    now = datetime.now(timezone.utc)
    start = (now - timedelta(days=6)).strftime("%Y-%m-%d")
    end = (now + timedelta(days=1)).strftime("%Y-%m-%d")
    return start, end


def fetch_aws_full(creds: Dict[str, str]) -> Optional[Dict[str, Any]]:
    """
    Fetch comprehensive AWS billing data using user's credentials.
    Returns dict with: currentSpend, services, dailySpend
    """
    try:
        import boto3

        client = boto3.client(
            "ce",
            region_name=creds.get("region", "us-east-1"),
            aws_access_key_id=creds["access_key_id"],
            aws_secret_access_key=creds["secret_access_key"],
        )

        result = {}

        # 1. Monthly total spend
        start, end = _month_bounds()
        resp = client.get_cost_and_usage(
            TimePeriod={"Start": start, "End": end},
            Granularity="MONTHLY",
            Metrics=["UnblendedCost"],
        )
        results_by_time = resp.get("ResultsByTime", [])
        total = 0.0
        if results_by_time:
            total = float(results_by_time[0].get("Total", {}).get("UnblendedCost", {}).get("Amount", 0))
        result["currentSpend"] = round(total, 2)

        # 2. Spend by service
        resp_svc = client.get_cost_and_usage(
            TimePeriod={"Start": start, "End": end},
            Granularity="MONTHLY",
            Metrics=["UnblendedCost"],
            GroupBy=[{"Type": "DIMENSION", "Key": "SERVICE"}],
        )
        services = {}
        for group_result in resp_svc.get("ResultsByTime", []):
            for group in group_result.get("Groups", []):
                svc_name = group["Keys"][0]
                amount = float(group.get("Metrics", {}).get("UnblendedCost", {}).get("Amount", 0))
                if amount > 0.01:
                    # Shorten service names
                    short_name = _shorten_aws_service(svc_name)
                    services[short_name] = round(amount, 2)
        result["services"] = dict(sorted(services.items(), key=lambda x: -x[1])[:10])  # Top 10

        # 3. Daily spend (last 7 days)
        d_start, d_end = _week_bounds()
        resp_daily = client.get_cost_and_usage(
            TimePeriod={"Start": d_start, "End": d_end},
            Granularity="DAILY",
            Metrics=["UnblendedCost"],
        )
        daily = []
        for day_result in resp_daily.get("ResultsByTime", []):
            date = day_result.get("TimePeriod", {}).get("Start", "")
            amount = float(day_result.get("Total", {}).get("UnblendedCost", {}).get("Amount", 0))
            daily.append({"date": date, "amount": round(amount, 2)})
        result["dailySpend"] = daily

        # 4. Daily spend by service (for trends)
        resp_daily_svc = client.get_cost_and_usage(
            TimePeriod={"Start": d_start, "End": d_end},
            Granularity="DAILY",
            Metrics=["UnblendedCost"],
            GroupBy=[{"Type": "DIMENSION", "Key": "SERVICE"}],
        )
        daily_by_service = {}
        for day_result in resp_daily_svc.get("ResultsByTime", []):
            date = day_result.get("TimePeriod", {}).get("Start", "")
            day_total = 0.0
            for group in day_result.get("Groups", []):
                amount = float(group.get("Metrics", {}).get("UnblendedCost", {}).get("Amount", 0))
                day_total += amount
            daily_by_service[date] = round(day_total, 2)
        result["dailyTotals"] = daily_by_service

        logger.info(f"AWS fetch success: ${total:.2f} MTD, {len(services)} services, {len(daily)} days")
        return result

    except Exception as e:
        logger.exception(f"AWS fetch failed: {e}")
        return None


def _shorten_aws_service(name: str) -> str:
    """Shorten verbose AWS service names."""
    mapping = {
        "Amazon Elastic Compute Cloud - Compute": "EC2",
        "Amazon Simple Storage Service": "S3",
        "Amazon Relational Database Service": "RDS",
        "AWS Lambda": "Lambda",
        "Amazon CloudWatch": "CloudWatch",
        "Amazon DynamoDB": "DynamoDB",
        "Amazon Elastic Container Service for Kubernetes": "EKS",
        "Amazon Elastic Container Service": "ECS",
        "Amazon CloudFront": "CloudFront",
        "Amazon Simple Queue Service": "SQS",
        "Amazon Simple Notification Service": "SNS",
        "Amazon Elastic Load Balancing": "ELB",
        "Amazon Route 53": "Route53",
        "AWS Data Transfer": "DataTransfer",
        "Amazon ElastiCache": "ElastiCache",
        "Amazon Elastic File System": "EFS",
        "Amazon Kinesis": "Kinesis",
        "Amazon Redshift": "Redshift",
        "Amazon SageMaker": "SageMaker",
        "AWS Key Management Service": "KMS",
        "Amazon Virtual Private Cloud": "VPC",
        "AWS Config": "Config",
        "AWS CloudTrail": "CloudTrail",
        "Amazon API Gateway": "API Gateway",
        "AWS Secrets Manager": "SecretsManager",
        "Amazon Elastic Block Store": "EBS",
    }
    for full, short in mapping.items():
        if full in name:
            return short
    # Fallback: take first 2-3 words
    parts = name.replace("Amazon ", "").replace("AWS ", "").split()
    return " ".join(parts[:2]) if len(parts) > 2 else name
