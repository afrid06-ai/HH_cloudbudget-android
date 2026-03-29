"""Rich user documents for Firestore — matches Android users/{userId} schema."""
from __future__ import annotations

from copy import deepcopy

USER_001 = {
    "name": "Lakshmi Niharika",
    "email": "lakshmi@example.com",
    "totalBudget": 500,
    "currency": "USD",
    "cloudData": {
        "aws": {
            "provider": "aws",
            "currentSpend": 214.37,
            "allocatedBudget": 220.0,
            "overBudget": False,
            "services": {
                "EC2": 92.4,
                "S3": 28.15,
                "RDS": 54.82,
                "Lambda": 16.3,
                "CloudWatch": 8.7,
                "DataTransfer": 14.0,
            },
        },
        "azure": {
            "provider": "azure",
            "currentSpend": 167.84,
            "allocatedBudget": 160.0,
            "overBudget": True,
            "services": {
                "VirtualMachines": 71.25,
                "BlobStorage": 24.4,
                "AzureSQL": 39.75,
                "Functions": 11.2,
                "Monitor": 7.44,
                "Bandwidth": 13.8,
            },
        },
        "gcp": {
            "provider": "gcp",
            "currentSpend": 82.46,
            "allocatedBudget": 120.0,
            "overBudget": False,
            "services": {
                "ComputeEngine": 31.1,
                "CloudStorage": 12.9,
                "BigQuery": 21.35,
                "CloudFunctions": 6.85,
                "CloudMonitoring": 3.26,
                "Networking": 7.0,
            },
        },
    },
    "dailySpend": {
        "2026-03-22": {"date": "2026-03-22", "aws": 27.84, "azure": 21.32, "gcp": 10.47, "total": 59.63},
        "2026-03-23": {"date": "2026-03-23", "aws": 29.12, "azure": 22.05, "gcp": 11.2, "total": 62.37},
        "2026-03-24": {"date": "2026-03-24", "aws": 30.45, "azure": 24.18, "gcp": 10.96, "total": 65.59},
        "2026-03-25": {"date": "2026-03-25", "aws": 31.9, "azure": 25.74, "gcp": 11.84, "total": 69.48},
        "2026-03-26": {"date": "2026-03-26", "aws": 32.64, "azure": 26.81, "gcp": 12.4, "total": 71.85},
        "2026-03-27": {"date": "2026-03-27", "aws": 30.88, "azure": 24.9, "gcp": 11.75, "total": 67.53},
        "2026-03-28": {"date": "2026-03-28", "aws": 31.54, "azure": 22.84, "gcp": 13.01, "total": 67.39},
    },
    "alerts": {
        "alert_001": {
            "title": "Azure Over Budget",
            "message": "Azure has exceeded its allocated budget by $7.84.",
            "type": "over_budget",
            "isRead": False,
            "createdAt": "2026-03-28T10:12:00Z",
        },
        "alert_002": {
            "title": "AWS Spend Rising",
            "message": "AWS daily spend increased by 8% compared to the previous 3-day average.",
            "type": "spend_spike",
            "isRead": True,
            "createdAt": "2026-03-27T15:20:00Z",
        },
        "alert_003": {
            "title": "Monthly Budget Near Limit",
            "message": "You have used 92.9% of your total monthly budget.",
            "type": "budget_warning",
            "isRead": False,
            "createdAt": "2026-03-28T18:05:00Z",
        },
    },
    "wasteInsights": {
        "totalWaste": 63.4,
        "items": [
            {
                "provider": "aws",
                "resourceName": "EC2 (EC2)",
                "description": "Largest line item — review instance sizes and reserved capacity.",
                "monthlySaving": 28.5,
                "recommendation": "Right-size or use Savings Plans for steady workloads.",
            },
            {
                "provider": "azure",
                "resourceName": "Virtual Machines",
                "description": "VMs dominate Azure bill while subscription is over allocated cap.",
                "monthlySaving": 19.2,
                "recommendation": "Schedule deallocate nights/weekends or move to spot where possible.",
            },
            {
                "provider": "gcp",
                "resourceName": "BigQuery",
                "description": "Query cost trending up — check slot commitments and caching.",
                "monthlySaving": 15.7,
                "recommendation": "Add partition filters and cap bytes billed per query.",
            },
        ],
    },
    "devices": {
        "device_001": {"fcmToken": "fcm_mock_token_android_001_xyz", "platform": "android"},
    },
}

USER_002 = {
    "name": "Jordan Chen",
    "email": "jordan@example.com",
    "totalBudget": 750,
    "currency": "USD",
    "cloudData": {
        "aws": {
            "provider": "aws",
            "currentSpend": 198.2,
            "allocatedBudget": 300.0,
            "overBudget": False,
            "services": {
                "EKS": 88.0,
                "S3": 34.5,
                "RDS": 40.2,
                "Lambda": 20.1,
                "VPC": 9.4,
                "Other": 6.0,
            },
        },
        "azure": {
            "provider": "azure",
            "currentSpend": 142.55,
            "allocatedBudget": 250.0,
            "overBudget": False,
            "services": {
                "AKS": 62.0,
                "CosmosDB": 35.1,
                "AppService": 22.3,
                "Functions": 9.9,
                "Monitor": 7.05,
                "CDN": 6.2,
            },
        },
        "gcp": {
            "provider": "gcp",
            "currentSpend": 288.9,
            "allocatedBudget": 200.0,
            "overBudget": True,
            "services": {
                "GKE": 112.0,
                "CloudSQL": 68.4,
                "BigQuery": 48.7,
                "CloudStorage": 31.9,
                "PubSub": 15.0,
                "Networking": 12.9,
            },
        },
    },
    "dailySpend": {
        "2026-03-22": {"date": "2026-03-22", "aws": 26.1, "azure": 19.8, "gcp": 38.2, "total": 84.1},
        "2026-03-23": {"date": "2026-03-23", "aws": 27.4, "azure": 20.6, "gcp": 39.9, "total": 87.9},
        "2026-03-24": {"date": "2026-03-24", "aws": 28.9, "azure": 21.9, "gcp": 41.3, "total": 92.1},
        "2026-03-25": {"date": "2026-03-25", "aws": 29.5, "azure": 22.4, "gcp": 42.8, "total": 94.7},
        "2026-03-26": {"date": "2026-03-26", "aws": 30.1, "azure": 23.0, "gcp": 41.2, "total": 94.3},
        "2026-03-27": {"date": "2026-03-27", "aws": 28.7, "azure": 22.1, "gcp": 40.5, "total": 91.3},
        "2026-03-28": {"date": "2026-03-28", "aws": 29.9, "azure": 23.6, "gcp": 43.7, "total": 97.2},
    },
    "alerts": {
        "alert_a": {
            "title": "GCP Over Allocation",
            "message": "GCP spend is $88.90 above its $200 slice — consider capping GKE autoscale.",
            "type": "over_budget",
            "isRead": False,
            "createdAt": "2026-03-28T12:30:00Z",
        },
        "alert_b": {
            "title": "BigQuery slot pressure",
            "message": "Analytics warehouse cost up 14% vs prior week.",
            "type": "spend_spike",
            "isRead": False,
            "createdAt": "2026-03-28T09:00:00Z",
        },
    },
    "wasteInsights": {
        "totalWaste": 91.25,
        "items": [
            {
                "provider": "gcp",
                "resourceName": "GKE clusters",
                "description": "GKE is the top cost driver and over team budget.",
                "monthlySaving": 45.0,
                "recommendation": "Enable cluster autoscaler min=0 on dev pools; review machine types.",
            },
            {
                "provider": "aws",
                "resourceName": "EKS control plane + nodes",
                "description": "Solid utilization but long-running dev cluster observed.",
                "monthlySaving": 22.0,
                "recommendation": "Consolidate dev into shared cluster with namespaces.",
            },
            {
                "provider": "azure",
                "resourceName": "CosmosDB RU/s",
                "description": "Provisioned throughput higher than P99 usage.",
                "monthlySaving": 24.25,
                "recommendation": "Switch dev to serverless tier or reduce RUs after metrics review.",
            },
        ],
    },
    "devices": {"device_x": {"fcmToken": "fcm_placeholder", "platform": "android"}},
}


def all_demo_users():
    return {"user_001": deepcopy(USER_001), "user_002": deepcopy(USER_002)}
