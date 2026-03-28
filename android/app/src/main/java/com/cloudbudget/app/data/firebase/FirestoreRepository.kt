package com.cloudbudget.app.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

object FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    // ─── Dashboard ───
    data class DashboardData(
        val totalSpend: Double = 0.0,
        val awsSpend: Double = 0.0,
        val azureSpend: Double = 0.0,
        val gcpSpend: Double = 0.0,
        val awsChange: Double = 0.0,
        val azureChange: Double = 0.0,
        val gcpChange: Double = 0.0,
        val cloudsConnected: Int = 0,
        val overBudget: Boolean = false
    )

    fun dashboardFlow(): Flow<DashboardData> = callbackFlow {
        val reg = db.collection("dashboard").document("current")
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null || !snap.exists()) {
                    trySend(DashboardData())
                    return@addSnapshotListener
                }
                val d = snap.data ?: return@addSnapshotListener
                trySend(DashboardData(
                    totalSpend = (d["totalSpend"] as? Number)?.toDouble() ?: 0.0,
                    awsSpend = (d["awsSpend"] as? Number)?.toDouble() ?: 0.0,
                    azureSpend = (d["azureSpend"] as? Number)?.toDouble() ?: 0.0,
                    gcpSpend = (d["gcpSpend"] as? Number)?.toDouble() ?: 0.0,
                    awsChange = (d["awsChange"] as? Number)?.toDouble() ?: 0.0,
                    azureChange = (d["azureChange"] as? Number)?.toDouble() ?: 0.0,
                    gcpChange = (d["gcpChange"] as? Number)?.toDouble() ?: 0.0,
                    cloudsConnected = (d["cloudsConnected"] as? Number)?.toInt() ?: 3,
                    overBudget = d["overBudget"] as? Boolean ?: false
                ))
            }
        awaitClose { reg.remove() }
    }

    // ─── Budget ───
    data class BudgetData(
        val totalBudget: Double = 500.0,
        val awsAllocated: Double = 225.0,
        val awsSpent: Double = 142.5,
        val azureAllocated: Double = 155.0,
        val azureSpent: Double = 98.3,
        val gcpAllocated: Double = 120.0,
        val gcpSpent: Double = 76.2
    )

    fun budgetFlow(): Flow<BudgetData> = callbackFlow {
        val reg = db.collection("budget").document("current")
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null || !snap.exists()) {
                    trySend(BudgetData())
                    return@addSnapshotListener
                }
                val d = snap.data ?: return@addSnapshotListener
                trySend(BudgetData(
                    totalBudget = (d["totalBudget"] as? Number)?.toDouble() ?: 500.0,
                    awsAllocated = (d["awsAllocated"] as? Number)?.toDouble() ?: 225.0,
                    awsSpent = (d["awsSpent"] as? Number)?.toDouble() ?: 142.5,
                    azureAllocated = (d["azureAllocated"] as? Number)?.toDouble() ?: 155.0,
                    azureSpent = (d["azureSpent"] as? Number)?.toDouble() ?: 98.3,
                    gcpAllocated = (d["gcpAllocated"] as? Number)?.toDouble() ?: 120.0,
                    gcpSpent = (d["gcpSpent"] as? Number)?.toDouble() ?: 76.2
                ))
            }
        awaitClose { reg.remove() }
    }

    // ─── Waste ───
    data class WasteItem(
        val provider: String = "",
        val resourceName: String = "",
        val description: String = "",
        val monthlySaving: Double = 0.0,
        val recommendation: String = ""
    )

    data class WasteData(
        val totalWaste: Double = 0.0,
        val items: List<WasteItem> = emptyList()
    )

    fun wasteFlow(): Flow<WasteData> = callbackFlow {
        val reg = db.collection("waste").document("current")
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null || !snap.exists()) {
                    trySend(WasteData())
                    return@addSnapshotListener
                }
                val d = snap.data ?: return@addSnapshotListener
                val items = (d["items"] as? List<Map<String, Any>>)?.map {
                    WasteItem(
                        provider = it["provider"] as? String ?: "",
                        resourceName = it["resourceName"] as? String ?: "",
                        description = it["description"] as? String ?: "",
                        monthlySaving = (it["monthlySaving"] as? Number)?.toDouble() ?: 0.0,
                        recommendation = it["recommendation"] as? String ?: ""
                    )
                } ?: emptyList()
                trySend(WasteData(
                    totalWaste = (d["totalWaste"] as? Number)?.toDouble() ?: 0.0,
                    items = items
                ))
            }
        awaitClose { reg.remove() }
    }

    // ─── Trends ───
    data class DailySpend(val date: String = "", val aws: Double = 0.0, val azure: Double = 0.0, val gcp: Double = 0.0)
    data class TrendsData(val dailySpends: List<DailySpend> = emptyList(), val avgDaily: Double = 0.0, val projected: Double = 0.0)

    fun trendsFlow(): Flow<TrendsData> = callbackFlow {
        val reg = db.collection("trends").document("current")
            .addSnapshotListener { snap, e ->
                if (e != null || snap == null || !snap.exists()) {
                    trySend(TrendsData())
                    return@addSnapshotListener
                }
                val d = snap.data ?: return@addSnapshotListener
                val spends = (d["dailySpends"] as? List<Map<String, Any>>)?.map {
                    DailySpend(
                        date = it["date"] as? String ?: "",
                        aws = (it["aws"] as? Number)?.toDouble() ?: 0.0,
                        azure = (it["azure"] as? Number)?.toDouble() ?: 0.0,
                        gcp = (it["gcp"] as? Number)?.toDouble() ?: 0.0
                    )
                } ?: emptyList()
                trySend(TrendsData(
                    dailySpends = spends,
                    avgDaily = (d["avgDaily"] as? Number)?.toDouble() ?: 0.0,
                    projected = (d["projected"] as? Number)?.toDouble() ?: 0.0
                ))
            }
        awaitClose { reg.remove() }
    }

    // ─── Alerts ───
    data class AlertItem(
        val severity: String = "ok",
        val title: String = "",
        val description: String = "",
        val provider: String = "",
        val timeAgo: String = ""
    )

    fun alertsFlow(): Flow<List<AlertItem>> = callbackFlow {
        val reg = db.collection("alerts")
            .addSnapshotListener { snaps, e ->
                if (e != null || snaps == null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val alerts = snaps.documents.mapNotNull { doc ->
                    val d = doc.data ?: return@mapNotNull null
                    AlertItem(
                        severity = d["severity"] as? String ?: "ok",
                        title = d["title"] as? String ?: "",
                        description = d["description"] as? String ?: "",
                        provider = d["provider"] as? String ?: "",
                        timeAgo = d["timeAgo"] as? String ?: ""
                    )
                }
                trySend(alerts)
            }
        awaitClose { reg.remove() }
    }

    // ─── Seed Demo Data ───
    suspend fun seedIfEmpty() {
        val dashDoc = db.collection("dashboard").document("current").get().await()
        if (dashDoc.exists()) return

        // Dashboard
        db.collection("dashboard").document("current").set(mapOf(
            "totalSpend" to 317.0,
            "awsSpend" to 142.5,
            "azureSpend" to 98.3,
            "gcpSpend" to 76.2,
            "awsChange" to 2.0,
            "azureChange" to 8.0,
            "gcpChange" to -1.0,
            "cloudsConnected" to 3,
            "overBudget" to false
        )).await()

        // Budget
        db.collection("budget").document("current").set(mapOf(
            "totalBudget" to 500.0,
            "awsAllocated" to 225.0,
            "awsSpent" to 142.5,
            "azureAllocated" to 155.0,
            "azureSpent" to 98.3,
            "gcpAllocated" to 120.0,
            "gcpSpent" to 76.2
        )).await()

        // Waste
        db.collection("waste").document("current").set(mapOf(
            "totalWaste" to 52.9,
            "items" to listOf(
                mapOf("provider" to "aws", "resourceName" to "EC2 t2.medium", "description" to "Running 24/7 — avg CPU below 5%", "monthlySaving" to 28.5, "recommendation" to "Resize to t2.micro or stop instance"),
                mapOf("provider" to "azure", "resourceName" to "Standard_D2s VM", "description" to "Idle VM — 0 active connections in 7 days", "monthlySaving" to 14.2, "recommendation" to "Deallocate VM or enable auto-shutdown"),
                mapOf("provider" to "gcp", "resourceName" to "n1-standard-2 Instance", "description" to "Dev instance left running over weekend", "monthlySaving" to 10.2, "recommendation" to "Enable auto-shutdown schedules")
            )
        )).await()

        // Trends
        db.collection("trends").document("current").set(mapOf(
            "avgDaily" to 4.75,
            "projected" to 142.5,
            "dailySpends" to listOf(
                mapOf("date" to "Mon", "aws" to 3.2, "azure" to 2.1, "gcp" to 1.5),
                mapOf("date" to "Tue", "aws" to 4.1, "azure" to 2.5, "gcp" to 1.8),
                mapOf("date" to "Wed", "aws" to 3.8, "azure" to 3.0, "gcp" to 1.6),
                mapOf("date" to "Thu", "aws" to 5.2, "azure" to 2.8, "gcp" to 2.0),
                mapOf("date" to "Fri", "aws" to 3.5, "azure" to 2.3, "gcp" to 1.9),
                mapOf("date" to "Sat", "aws" to 2.9, "azure" to 2.7, "gcp" to 1.7),
                mapOf("date" to "Sun", "aws" to 3.3, "azure" to 2.5, "gcp" to 1.8)
            )
        )).await()

        // Alerts
        db.collection("alerts").document("alert1").set(mapOf(
            "severity" to "critical", "title" to "EC2 Cost Spike Detected",
            "description" to "EC2 spend up 67% in 24 hours: \$18 above daily average",
            "provider" to "aws", "timeAgo" to "2h ago"
        )).await()
        db.collection("alerts").document("alert2").set(mapOf(
            "severity" to "warning", "title" to "Approaching Budget Limit",
            "description" to "Azure used 63% of its \$155.00 allocated budget",
            "provider" to "azure", "timeAgo" to "6h ago"
        )).await()
        db.collection("alerts").document("alert3").set(mapOf(
            "severity" to "ok", "title" to "GCP Spend On Track",
            "description" to "GCP spent \$76.20 of \$120.00 budget",
            "provider" to "gcp", "timeAgo" to "1d ago"
        )).await()
    }
}
