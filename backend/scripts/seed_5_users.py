#!/usr/bin/env python3
"""
Seed Firestore with 5 demo users for CloudBudget.
Usage:
  export FIREBASE_CREDENTIALS_PATH="/path/to/serviceAccountKey.json"
  python scripts/seed_5_users.py
"""
import os, sys, json

def get_users():
    return {
        "user_001": {
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
                        "EC2": 92.4, "S3": 28.15, "RDS": 54.82,
                        "Lambda": 16.3, "CloudWatch": 8.7, "DataTransfer": 14.0
                    }
                },
                "azure": {
                    "provider": "azure",
                    "currentSpend": 167.84,
                    "allocatedBudget": 160.0,
                    "overBudget": True,
                    "services": {
                        "VirtualMachines": 71.25, "BlobStorage": 24.4, "AzureSQL": 39.75,
                        "Functions": 11.2, "Monitor": 7.44, "Bandwidth": 13.8
                    }
                },
                "gcp": {
                    "provider": "gcp",
                    "currentSpend": 82.46,
                    "allocatedBudget": 120.0,
                    "overBudget": False,
                    "services": {
                        "ComputeEngine": 31.1, "CloudStorage": 12.9, "BigQuery": 21.35,
                        "CloudFunctions": 6.85, "CloudMonitoring": 3.26, "Networking": 7.0
                    }
                }
            },
            "dailySpend": {
                "2026-03-22": {"date": "2026-03-22", "aws": 27.84, "azure": 21.32, "gcp": 10.47, "total": 59.63},
                "2026-03-23": {"date": "2026-03-23", "aws": 29.12, "azure": 22.05, "gcp": 11.2, "total": 62.37},
                "2026-03-24": {"date": "2026-03-24", "aws": 30.45, "azure": 24.18, "gcp": 10.96, "total": 65.59},
                "2026-03-25": {"date": "2026-03-25", "aws": 31.9, "azure": 25.74, "gcp": 11.84, "total": 69.48},
                "2026-03-26": {"date": "2026-03-26", "aws": 32.64, "azure": 26.81, "gcp": 12.4, "total": 71.85},
                "2026-03-27": {"date": "2026-03-27", "aws": 30.88, "azure": 24.9, "gcp": 11.75, "total": 67.53},
                "2026-03-28": {"date": "2026-03-28", "aws": 31.54, "azure": 22.84, "gcp": 13.01, "total": 67.39}
            },
            "alerts": {
                "alert_001": {"title": "Azure Over Budget", "message": "Azure has exceeded its allocated budget by $7.84.", "type": "over_budget", "severity": "critical", "provider": "azure", "isRead": False, "createdAt": "2026-03-28T10:12:00Z"},
                "alert_002": {"title": "AWS Spend Rising", "message": "AWS daily spend increased by 8% compared to the previous 3-day average.", "type": "spend_spike", "severity": "warning", "provider": "aws", "isRead": True, "createdAt": "2026-03-27T15:20:00Z"},
                "alert_003": {"title": "Monthly Budget Near Limit", "message": "You have used 92.9% of your total monthly budget.", "type": "budget_warning", "severity": "warning", "provider": "all", "isRead": False, "createdAt": "2026-03-28T18:05:00Z"}
            },
            "waste": {
                "totalWaste": 47.3,
                "items": [
                    {"provider": "aws", "resourceName": "EC2 t2.medium", "description": "Running 24/7 — avg CPU below 5%", "monthlySaving": 28.5, "recommendation": "Resize to t2.micro or stop instance"},
                    {"provider": "azure", "resourceName": "Standard_D2s VM", "description": "Idle VM — 0 active connections in 7 days", "monthlySaving": 14.2, "recommendation": "Deallocate VM or enable auto-shutdown"},
                    {"provider": "gcp", "resourceName": "Persistent Disk 200GB", "description": "Attached to stopped instance for 14 days", "monthlySaving": 4.6, "recommendation": "Delete disk or snapshot and remove"}
                ]
            }
        },

        "user_002": {
            "name": "Afrid Shaik",
            "email": "afrid@example.com",
            "totalBudget": 800,
            "currency": "USD",
            "cloudData": {
                "aws": {
                    "provider": "aws",
                    "currentSpend": 342.18,
                    "allocatedBudget": 350.0,
                    "overBudget": False,
                    "services": {
                        "EC2": 145.6, "S3": 42.3, "RDS": 78.9,
                        "Lambda": 32.18, "CloudWatch": 18.2, "EKS": 25.0
                    }
                },
                "azure": {
                    "provider": "azure",
                    "currentSpend": 198.52,
                    "allocatedBudget": 250.0,
                    "overBudget": False,
                    "services": {
                        "VirtualMachines": 89.4, "BlobStorage": 31.2, "AzureSQL": 42.5,
                        "Functions": 15.82, "CosmosDB": 12.6, "Bandwidth": 7.0
                    }
                },
                "gcp": {
                    "provider": "gcp",
                    "currentSpend": 156.73,
                    "allocatedBudget": 200.0,
                    "overBudget": False,
                    "services": {
                        "ComputeEngine": 62.3, "CloudStorage": 18.4, "BigQuery": 38.93,
                        "CloudRun": 14.1, "CloudSQL": 16.0, "Networking": 7.0
                    }
                }
            },
            "dailySpend": {
                "2026-03-22": {"date": "2026-03-22", "aws": 45.12, "azure": 26.8, "gcp": 20.15, "total": 92.07},
                "2026-03-23": {"date": "2026-03-23", "aws": 47.35, "azure": 28.42, "gcp": 21.8, "total": 97.57},
                "2026-03-24": {"date": "2026-03-24", "aws": 49.8, "azure": 27.9, "gcp": 22.45, "total": 100.15},
                "2026-03-25": {"date": "2026-03-25", "aws": 50.22, "azure": 29.15, "gcp": 23.1, "total": 102.47},
                "2026-03-26": {"date": "2026-03-26", "aws": 48.9, "azure": 28.75, "gcp": 22.63, "total": 100.28},
                "2026-03-27": {"date": "2026-03-27", "aws": 49.45, "azure": 27.5, "gcp": 23.4, "total": 100.35},
                "2026-03-28": {"date": "2026-03-28", "aws": 51.34, "azure": 30.0, "gcp": 23.2, "total": 104.54}
            },
            "alerts": {
                "alert_001": {"title": "EKS Cluster Cost Alert", "message": "EKS cluster spending $25/day, 40% above forecast.", "type": "spend_spike", "severity": "warning", "provider": "aws", "isRead": False, "createdAt": "2026-03-28T09:30:00Z"},
                "alert_002": {"title": "BigQuery Scan Cost", "message": "Large query scanned 2.1TB — cost $18.93 in single run.", "type": "anomaly", "severity": "critical", "provider": "gcp", "isRead": False, "createdAt": "2026-03-28T14:15:00Z"},
                "alert_003": {"title": "Budget On Track", "message": "Total spend is 87.2% with 3 days remaining.", "type": "budget_warning", "severity": "ok", "provider": "all", "isRead": True, "createdAt": "2026-03-27T20:00:00Z"}
            },
            "waste": {
                "totalWaste": 62.8,
                "items": [
                    {"provider": "aws", "resourceName": "NAT Gateway us-east-1", "description": "Processing only 1.2GB/day — overprovisioned", "monthlySaving": 32.0, "recommendation": "Switch to NAT instance or VPC endpoints"},
                    {"provider": "gcp", "resourceName": "n1-standard-4 Instance", "description": "Dev/test instance running 24/7, used only during business hours", "monthlySaving": 18.5, "recommendation": "Schedule auto-start/stop or use preemptible"},
                    {"provider": "azure", "resourceName": "Premium SSD 512GB", "description": "Attached to deallocated VM since March 15", "monthlySaving": 12.3, "recommendation": "Downgrade to Standard HDD or delete"}
                ]
            }
        },

        "user_003": {
            "name": "Priya Sharma",
            "email": "priya@example.com",
            "totalBudget": 1200,
            "currency": "USD",
            "cloudData": {
                "aws": {
                    "provider": "aws",
                    "currentSpend": 485.6,
                    "allocatedBudget": 500.0,
                    "overBudget": False,
                    "services": {
                        "EC2": 198.4, "S3": 65.2, "RDS": 112.0,
                        "Lambda": 45.0, "CloudFront": 38.0, "SQS": 27.0
                    }
                },
                "azure": {
                    "provider": "azure",
                    "currentSpend": 412.75,
                    "allocatedBudget": 400.0,
                    "overBudget": True,
                    "services": {
                        "VirtualMachines": 178.5, "BlobStorage": 52.25, "AzureSQL": 89.0,
                        "Functions": 28.5, "AppService": 42.0, "Bandwidth": 22.5
                    }
                },
                "gcp": {
                    "provider": "gcp",
                    "currentSpend": 245.3,
                    "allocatedBudget": 300.0,
                    "overBudget": False,
                    "services": {
                        "ComputeEngine": 95.0, "CloudStorage": 32.8, "BigQuery": 52.5,
                        "GKE": 38.0, "CloudSQL": 18.0, "Networking": 9.0
                    }
                }
            },
            "dailySpend": {
                "2026-03-22": {"date": "2026-03-22", "aws": 65.2, "azure": 55.8, "gcp": 32.4, "total": 153.4},
                "2026-03-23": {"date": "2026-03-23", "aws": 68.1, "azure": 57.3, "gcp": 34.1, "total": 159.5},
                "2026-03-24": {"date": "2026-03-24", "aws": 70.5, "azure": 59.8, "gcp": 35.6, "total": 165.9},
                "2026-03-25": {"date": "2026-03-25", "aws": 69.3, "azure": 61.2, "gcp": 36.8, "total": 167.3},
                "2026-03-26": {"date": "2026-03-26", "aws": 71.8, "azure": 58.9, "gcp": 35.2, "total": 165.9},
                "2026-03-27": {"date": "2026-03-27", "aws": 68.9, "azure": 60.5, "gcp": 36.1, "total": 165.5},
                "2026-03-28": {"date": "2026-03-28", "aws": 71.8, "azure": 59.25, "gcp": 35.1, "total": 166.15}
            },
            "alerts": {
                "alert_001": {"title": "Azure Over Budget", "message": "Azure exceeded budget by $12.75. Investigate AppService scaling.", "type": "over_budget", "severity": "critical", "provider": "azure", "isRead": False, "createdAt": "2026-03-28T08:00:00Z"},
                "alert_002": {"title": "CloudFront Spike", "message": "CloudFront costs up 35% — possible DDoS or traffic surge.", "type": "anomaly", "severity": "critical", "provider": "aws", "isRead": False, "createdAt": "2026-03-28T11:45:00Z"},
                "alert_003": {"title": "GCP Under Budget", "message": "GCP spend at 81.8% of allocated — healthy pace.", "type": "info", "severity": "ok", "provider": "gcp", "isRead": True, "createdAt": "2026-03-28T06:00:00Z"},
                "alert_004": {"title": "Total Budget Critical", "message": "Overall spend at 95.3% of $1,200 budget.", "type": "budget_warning", "severity": "critical", "provider": "all", "isRead": False, "createdAt": "2026-03-28T19:00:00Z"}
            },
            "waste": {
                "totalWaste": 89.5,
                "items": [
                    {"provider": "aws", "resourceName": "RDS Multi-AZ db.r5.large", "description": "Dev database with Multi-AZ enabled — unnecessary for non-prod", "monthlySaving": 45.0, "recommendation": "Disable Multi-AZ for dev environment"},
                    {"provider": "azure", "resourceName": "App Service Plan P2v3", "description": "Premium plan with avg 12% CPU utilization", "monthlySaving": 28.5, "recommendation": "Downgrade to B2 or S1 plan"},
                    {"provider": "gcp", "resourceName": "Cloud SQL HA Instance", "description": "High-availability replica for staging — rarely accessed", "monthlySaving": 16.0, "recommendation": "Remove HA for staging, keep for prod only"}
                ]
            }
        },

        "user_004": {
            "name": "Ravi Kumar",
            "email": "ravi@example.com",
            "totalBudget": 350,
            "currency": "USD",
            "cloudData": {
                "aws": {
                    "provider": "aws",
                    "currentSpend": 128.45,
                    "allocatedBudget": 150.0,
                    "overBudget": False,
                    "services": {
                        "EC2": 52.0, "S3": 18.45, "RDS": 32.0,
                        "Lambda": 12.0, "CloudWatch": 6.0, "SNS": 8.0
                    }
                },
                "azure": {
                    "provider": "azure",
                    "currentSpend": 95.2,
                    "allocatedBudget": 100.0,
                    "overBudget": False,
                    "services": {
                        "VirtualMachines": 42.0, "BlobStorage": 15.2, "AzureSQL": 22.0,
                        "Functions": 8.0, "Monitor": 4.0, "Bandwidth": 4.0
                    }
                },
                "gcp": {
                    "provider": "gcp",
                    "currentSpend": 108.6,
                    "allocatedBudget": 100.0,
                    "overBudget": True,
                    "services": {
                        "ComputeEngine": 42.6, "CloudStorage": 14.0, "BigQuery": 28.0,
                        "CloudFunctions": 10.0, "Firestore": 8.0, "Networking": 6.0
                    }
                }
            },
            "dailySpend": {
                "2026-03-22": {"date": "2026-03-22", "aws": 16.8, "azure": 12.5, "gcp": 13.2, "total": 42.5},
                "2026-03-23": {"date": "2026-03-23", "aws": 17.5, "azure": 13.1, "gcp": 14.8, "total": 45.4},
                "2026-03-24": {"date": "2026-03-24", "aws": 18.2, "azure": 13.8, "gcp": 15.5, "total": 47.5},
                "2026-03-25": {"date": "2026-03-25", "aws": 18.9, "azure": 14.0, "gcp": 16.2, "total": 49.1},
                "2026-03-26": {"date": "2026-03-26", "aws": 19.1, "azure": 13.6, "gcp": 16.8, "total": 49.5},
                "2026-03-27": {"date": "2026-03-27", "aws": 19.45, "azure": 14.2, "gcp": 15.9, "total": 49.55},
                "2026-03-28": {"date": "2026-03-28", "aws": 18.5, "azure": 14.0, "gcp": 16.2, "total": 48.7}
            },
            "alerts": {
                "alert_001": {"title": "GCP Over Budget", "message": "GCP exceeded $100 budget by $8.60. BigQuery costs spiking.", "type": "over_budget", "severity": "critical", "provider": "gcp", "isRead": False, "createdAt": "2026-03-28T12:30:00Z"},
                "alert_002": {"title": "AWS Healthy", "message": "AWS spend is tracking well at 85.6% of budget.", "type": "info", "severity": "ok", "provider": "aws", "isRead": True, "createdAt": "2026-03-28T07:00:00Z"}
            },
            "waste": {
                "totalWaste": 22.4,
                "items": [
                    {"provider": "gcp", "resourceName": "BigQuery Flat-Rate Slot", "description": "Reserved 100 slots but avg utilization is 15 slots", "monthlySaving": 14.0, "recommendation": "Switch to on-demand pricing or reduce slots"},
                    {"provider": "aws", "resourceName": "EBS Volume gp2 500GB", "description": "Orphaned volume — was attached to terminated instance", "monthlySaving": 8.4, "recommendation": "Snapshot and delete the orphaned volume"}
                ]
            }
        },

        "user_005": {
            "name": "Sara Ahmed",
            "email": "sara@example.com",
            "totalBudget": 2000,
            "currency": "USD",
            "cloudData": {
                "aws": {
                    "provider": "aws",
                    "currentSpend": 725.8,
                    "allocatedBudget": 800.0,
                    "overBudget": False,
                    "services": {
                        "EC2": 285.0, "S3": 95.8, "RDS": 145.0,
                        "Lambda": 68.0, "EKS": 82.0, "CloudFront": 50.0
                    }
                },
                "azure": {
                    "provider": "azure",
                    "currentSpend": 580.45,
                    "allocatedBudget": 600.0,
                    "overBudget": False,
                    "services": {
                        "VirtualMachines": 245.0, "BlobStorage": 68.45, "AzureSQL": 118.0,
                        "Functions": 42.0, "AKS": 72.0, "Bandwidth": 35.0
                    }
                },
                "gcp": {
                    "provider": "gcp",
                    "currentSpend": 542.15,
                    "allocatedBudget": 600.0,
                    "overBudget": False,
                    "services": {
                        "ComputeEngine": 195.0, "CloudStorage": 58.15, "BigQuery": 112.0,
                        "GKE": 95.0, "CloudSQL": 52.0, "Networking": 30.0
                    }
                }
            },
            "dailySpend": {
                "2026-03-22": {"date": "2026-03-22", "aws": 98.5, "azure": 78.2, "gcp": 72.4, "total": 249.1},
                "2026-03-23": {"date": "2026-03-23", "aws": 102.3, "azure": 81.5, "gcp": 75.8, "total": 259.6},
                "2026-03-24": {"date": "2026-03-24", "aws": 105.1, "azure": 83.0, "gcp": 78.2, "total": 266.3},
                "2026-03-25": {"date": "2026-03-25", "aws": 103.8, "azure": 84.5, "gcp": 77.5, "total": 265.8},
                "2026-03-26": {"date": "2026-03-26", "aws": 106.2, "azure": 82.8, "gcp": 79.1, "total": 268.1},
                "2026-03-27": {"date": "2026-03-27", "aws": 104.5, "azure": 85.2, "gcp": 78.65, "total": 268.35},
                "2026-03-28": {"date": "2026-03-28", "aws": 105.4, "azure": 85.25, "gcp": 80.5, "total": 271.15}
            },
            "alerts": {
                "alert_001": {"title": "EKS Cost Optimization", "message": "EKS cluster has 3 underutilized nodes. Consider Cluster Autoscaler.", "type": "optimization", "severity": "warning", "provider": "aws", "isRead": False, "createdAt": "2026-03-28T10:00:00Z"},
                "alert_002": {"title": "AKS Node Pool Alert", "message": "AKS system pool scaled to 5 nodes but avg utilization is 35%.", "type": "optimization", "severity": "warning", "provider": "azure", "isRead": False, "createdAt": "2026-03-28T13:20:00Z"},
                "alert_003": {"title": "GKE Autopilot Savings", "message": "Switching dev workloads to Autopilot could save ~$28/month.", "type": "optimization", "severity": "ok", "provider": "gcp", "isRead": True, "createdAt": "2026-03-27T16:00:00Z"},
                "alert_004": {"title": "Multi-Cloud Summary", "message": "Total spend at 92.4% of $2,000 budget. 3 days remaining.", "type": "budget_warning", "severity": "warning", "provider": "all", "isRead": False, "createdAt": "2026-03-28T20:00:00Z"}
            },
            "waste": {
                "totalWaste": 134.2,
                "items": [
                    {"provider": "aws", "resourceName": "EC2 Reserved Instances (3x m5.xlarge)", "description": "Reserved instances in us-west-2 with <20% utilization", "monthlySaving": 52.0, "recommendation": "Sell on Reserved Instance Marketplace or modify"},
                    {"provider": "azure", "resourceName": "AKS System Node Pool", "description": "5 Standard_D4s nodes but workload fits on 3", "monthlySaving": 38.0, "recommendation": "Enable cluster autoscaler, set min=2 max=4"},
                    {"provider": "gcp", "resourceName": "Cloud SQL Replica", "description": "Read replica in asia-east1 with 0 queries in 30 days", "monthlySaving": 26.0, "recommendation": "Delete unused replica, recreate if needed"},
                    {"provider": "aws", "resourceName": "S3 Standard (Archive Data)", "description": "42GB of logs older than 90 days in Standard tier", "monthlySaving": 18.2, "recommendation": "Move to S3 Glacier or Intelligent-Tiering"}
                ]
            }
        }
    }


def main():
    cred_path = os.environ.get("FIREBASE_CREDENTIALS_PATH") or os.environ.get("GOOGLE_APPLICATION_CREDENTIALS")
    if not cred_path or not os.path.isfile(cred_path):
        print("ERROR: Set FIREBASE_CREDENTIALS_PATH to your Firebase service account JSON.")
        print("  Download from: Firebase Console > Project Settings > Service Accounts > Generate New Private Key")
        sys.exit(1)

    import firebase_admin
    from firebase_admin import credentials, firestore

    if not firebase_admin._apps:
        firebase_admin.initialize_app(credentials.Certificate(cred_path))
    db = firestore.client()

    users = get_users()

    # Clear existing users
    print("Clearing existing user data...")
    existing = db.collection("users").stream()
    for doc in existing:
        doc.reference.delete()
        print(f"  Deleted: {doc.id}")

    # Also clear legacy flat collections
    for coll in ["dashboard", "budget", "waste", "trends", "alerts"]:
        docs = list(db.collection(coll).stream())
        for doc in docs:
            doc.reference.delete()
        if docs:
            print(f"  Cleared legacy collection: {coll} ({len(docs)} docs)")

    # Seed new users
    print(f"\nSeeding {len(users)} users...")
    for uid, profile in users.items():
        db.collection("users").document(uid).set(profile)
        total_spend = sum(
            cloud["currentSpend"]
            for cloud in profile["cloudData"].values()
        )
        print(f"  {uid}: {profile['name']} — Total Spend: ${total_spend:.2f} / ${profile['totalBudget']}")

    # Also seed flat collections for backward compatibility with current Android app
    print("\nSeeding flat collections for Android app compatibility...")
    default_user = users["user_001"]
    cd = default_user["cloudData"]

    # Dashboard
    total_spend = sum(c["currentSpend"] for c in cd.values())
    db.collection("dashboard").document("current").set({
        "totalSpend": round(total_spend, 2),
        "awsSpend": cd["aws"]["currentSpend"],
        "azureSpend": cd["azure"]["currentSpend"],
        "gcpSpend": cd["gcp"]["currentSpend"],
        "awsChange": 5.3,
        "azureChange": 12.1,
        "gcpChange": -2.4,
        "cloudsConnected": 3,
        "overBudget": any(c["overBudget"] for c in cd.values())
    })
    print("  dashboard/current ✓")

    # Budget
    db.collection("budget").document("current").set({
        "totalBudget": default_user["totalBudget"],
        "awsAllocated": cd["aws"]["allocatedBudget"],
        "awsSpent": cd["aws"]["currentSpend"],
        "azureAllocated": cd["azure"]["allocatedBudget"],
        "azureSpent": cd["azure"]["currentSpend"],
        "gcpAllocated": cd["gcp"]["allocatedBudget"],
        "gcpSpent": cd["gcp"]["currentSpend"]
    })
    print("  budget/current ✓")

    # Waste
    db.collection("waste").document("current").set(default_user["waste"])
    print("  waste/current ✓")

    # Trends
    daily = default_user["dailySpend"]
    sorted_days = sorted(daily.values(), key=lambda d: d["date"])
    avg_daily = round(sum(d["total"] for d in sorted_days) / len(sorted_days), 2)
    projected = round(avg_daily * 30, 2)
    db.collection("trends").document("current").set({
        "avgDaily": avg_daily,
        "projected": projected,
        "dailySpends": [
            {"date": d["date"][-5:].replace("-", "/"), "aws": d["aws"], "azure": d["azure"], "gcp": d["gcp"]}
            for d in sorted_days
        ]
    })
    print("  trends/current ✓")

    # Alerts
    for aid, alert in default_user["alerts"].items():
        db.collection("alerts").document(aid).set({
            "severity": alert.get("severity", "warning"),
            "title": alert["title"],
            "description": alert["message"],
            "provider": alert.get("provider", "all"),
            "timeAgo": "recently"
        })
    print(f"  alerts ({len(default_user['alerts'])} docs) ✓")

    print(f"\n✅ Done! {len(users)} users seeded + flat collections synced.")
    print("Refresh Firebase Console → Firestore → Data to verify.")


if __name__ == "__main__":
    main()
