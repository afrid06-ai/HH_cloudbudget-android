package com.cloudbudget.app.data.firebase

import com.cloudbudget.app.BuildConfig
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

@Suppress("UNCHECKED_CAST")
object FirestoreRepository {

    private val db = FirebaseFirestore.getInstance()

    private fun userId(): String = BuildConfig.FIRESTORE_USER_ID

    private fun userRef() = db.collection("users").document(userId())

    // ─── User Profile ───
    data class UserProfile(
        val displayName: String = "",
        val email: String = "",
        val userId: String = ""
    )

    fun profileFlow(): Flow<UserProfile> = callbackFlow {
        val reg = userRef().addSnapshotListener { snap, e ->
            if (e != null) { trySend(UserProfile()); return@addSnapshotListener }
            val d = snap?.takeIf { it.exists() }?.data
            trySend(UserProfile(
                displayName = (d?.get("displayName") as? String) ?: "",
                email = (d?.get("email") as? String) ?: "",
                userId = userId()
            ))
        }
        awaitClose { reg.remove() }
    }

    fun updateProfile(name: String, email: String) {
        userRef().update(
            mapOf("displayName" to name, "email" to email)
        )
    }

    fun saveProfileOnSignup(name: String, email: String) {
        userRef().set(
            mapOf("displayName" to name, "email" to email),
            com.google.firebase.firestore.SetOptions.merge()
        )
    }

    fun saveBudget(total: Double, aws: Double, azure: Double, gcp: Double) {
        userRef().get().addOnSuccessListener { snap ->
            val data = snap.data?.toMutableMap() ?: mutableMapOf()
            data["totalBudget"] = total

            val cloudData = (data["cloudData"] as? MutableMap<String, Any>)
                ?: mutableMapOf<String, Any>()

            fun updateProvider(key: String, allocated: Double) {
                val provider = (cloudData[key] as? MutableMap<String, Any>)
                    ?: mutableMapOf<String, Any>()
                provider["allocatedBudget"] = allocated
                cloudData[key] = provider
            }

            updateProvider("aws", aws)
            updateProvider("azure", azure)
            updateProvider("gcp", gcp)
            data["cloudData"] = cloudData

            // Also update legacy budget collection for backward compat
            db.collection("budget").document("current").set(
                mapOf(
                    "totalBudget" to total,
                    "awsAllocated" to aws,
                    "azureAllocated" to azure,
                    "gcpAllocated" to gcp
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )

            userRef().set(data, com.google.firebase.firestore.SetOptions.merge())
        }
    }

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
        var legacyReg: ListenerRegistration? = null
        val reg = userRef().addSnapshotListener { snap, e ->
            if (e != null) {
                trySend(DashboardData())
                return@addSnapshotListener
            }
            val d = snap?.takeIf { it.exists() }?.data
            if (d != null && d["cloudData"] != null) {
                legacyReg?.remove()
                legacyReg = null
                trySend(parseUserDashboard(d))
            } else {
                if (legacyReg == null) {
                    legacyReg = db.collection("dashboard").document("current")
                        .addSnapshotListener { ls, _ ->
                            val m = ls?.takeIf { it.exists() }?.data
                            if (m != null) trySend(parseLegacyDashboard(m))
                            else trySend(DashboardData())
                        }
                }
            }
        }
        awaitClose {
            reg.remove()
            legacyReg?.remove()
        }
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
        var legacyReg: ListenerRegistration? = null
        val reg = userRef().addSnapshotListener { snap, e ->
            if (e != null) {
                trySend(BudgetData())
                return@addSnapshotListener
            }
            val d = snap?.takeIf { it.exists() }?.data
            if (d != null && d["cloudData"] != null) {
                legacyReg?.remove()
                legacyReg = null
                trySend(parseUserBudget(d))
            } else {
                if (legacyReg == null) {
                    legacyReg = db.collection("budget").document("current")
                        .addSnapshotListener { ls, _ ->
                            val m = ls?.takeIf { it.exists() }?.data
                            if (m != null) trySend(parseLegacyBudget(m))
                            else trySend(BudgetData())
                        }
                }
            }
        }
        awaitClose {
            reg.remove()
            legacyReg?.remove()
        }
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
        var legacyReg: ListenerRegistration? = null
        val reg = userRef().addSnapshotListener { snap, e ->
            if (e != null) {
                trySend(WasteData())
                return@addSnapshotListener
            }
            val d = snap?.takeIf { it.exists() }?.data
            if (d != null && d["cloudData"] != null) {
                legacyReg?.remove()
                legacyReg = null
                trySend(parseUserWaste(d))
            } else {
                if (legacyReg == null) {
                    legacyReg = db.collection("waste").document("current")
                        .addSnapshotListener { ls, _ ->
                            val m = ls?.takeIf { it.exists() }?.data
                            if (m != null) trySend(parseLegacyWaste(m))
                            else trySend(WasteData())
                        }
                }
            }
        }
        awaitClose {
            reg.remove()
            legacyReg?.remove()
        }
    }

    // ─── Trends ───
    data class DailySpend(val date: String = "", val aws: Double = 0.0, val azure: Double = 0.0, val gcp: Double = 0.0)
    data class TrendsData(val dailySpends: List<DailySpend> = emptyList(), val avgDaily: Double = 0.0, val projected: Double = 0.0)

    fun trendsFlow(): Flow<TrendsData> = callbackFlow {
        var legacyReg: ListenerRegistration? = null
        val reg = userRef().addSnapshotListener { snap, e ->
            if (e != null) {
                trySend(TrendsData())
                return@addSnapshotListener
            }
            val d = snap?.takeIf { it.exists() }?.data
            if (d != null && d["cloudData"] != null) {
                legacyReg?.remove()
                legacyReg = null
                trySend(parseUserTrends(d))
            } else {
                if (legacyReg == null) {
                    legacyReg = db.collection("trends").document("current")
                        .addSnapshotListener { ls, _ ->
                            val m = ls?.takeIf { it.exists() }?.data
                            if (m != null) trySend(parseLegacyTrends(m))
                            else trySend(TrendsData())
                        }
                }
            }
        }
        awaitClose {
            reg.remove()
            legacyReg?.remove()
        }
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
        var legacyReg: ListenerRegistration? = null
        val reg = userRef().addSnapshotListener { snap, e ->
            if (e != null) {
                trySend(emptyList())
                return@addSnapshotListener
            }
            val d = snap?.takeIf { it.exists() }?.data
            if (d != null && d["cloudData"] != null) {
                legacyReg?.remove()
                legacyReg = null
                trySend(parseUserAlerts(d))
            } else {
                if (legacyReg == null) {
                    legacyReg = db.collection("alerts")
                        .addSnapshotListener { snaps, _ ->
                            if (snaps == null) {
                                trySend(emptyList())
                                return@addSnapshotListener
                            }
                            val list = snaps.documents.mapNotNull { doc ->
                                val m = doc.data ?: return@mapNotNull null
                                AlertItem(
                                    severity = m["severity"] as? String ?: "ok",
                                    title = m["title"] as? String ?: "",
                                    description = m["description"] as? String ?: "",
                                    provider = m["provider"] as? String ?: "",
                                    timeAgo = m["timeAgo"] as? String ?: ""
                                )
                            }
                            trySend(list)
                        }
                }
            }
        }
        awaitClose {
            reg.remove()
            legacyReg?.remove()
        }
    }

    // ─── Parsers: nested users/{id} ───
    private fun parseUserDashboard(root: Map<String, Any>): DashboardData {
        val cloudData = root["cloudData"] as? Map<String, Any> ?: return DashboardData()
        fun spend(key: String) =
            ((cloudData[key] as? Map<String, Any>)?.get("currentSpend") as? Number)?.toDouble() ?: 0.0
        val aws = spend("aws")
        val azure = spend("azure")
        val gcp = spend("gcp")
        val total = aws + azure + gcp
        val totalBudget = (root["totalBudget"] as? Number)?.toDouble() ?: 500.0
        val anyCloudOver = listOf("aws", "azure", "gcp").any { pk ->
            ((cloudData[pk] as? Map<String, Any>)?.get("overBudget") as? Boolean) == true
        }
        val (aCh, azCh, gCh) = pctFromDailyMap(root["dailySpend"] as? Map<String, Any>)
        val connected = listOf("aws", "azure", "gcp").count { spend(it) > 0 || cloudData[it] != null }
        return DashboardData(
            totalSpend = total,
            awsSpend = aws,
            azureSpend = azure,
            gcpSpend = gcp,
            awsChange = aCh,
            azureChange = azCh,
            gcpChange = gCh,
            cloudsConnected = if (connected == 0) 3 else connected,
            overBudget = total > totalBudget || anyCloudOver
        )
    }

    private fun pctFromDailyMap(daily: Map<String, Any>?): Triple<Double, Double, Double> {
        if (daily == null || daily.size < 2) return Triple(0.0, 0.0, 0.0)
        val sorted = daily.entries.sortedBy { it.key }
        val prev = sorted[sorted.size - 2].value as? Map<String, Any> ?: return Triple(0.0, 0.0, 0.0)
        val last = sorted[sorted.size - 1].value as? Map<String, Any> ?: return Triple(0.0, 0.0, 0.0)
        fun pct(k: String): Double {
            val a = (prev[k] as? Number)?.toDouble() ?: 0.0
            val b = (last[k] as? Number)?.toDouble() ?: 0.0
            if (a == 0.0) return 0.0
            return ((b - a) / a) * 100.0
        }
        return Triple(pct("aws"), pct("azure"), pct("gcp"))
    }

    private fun parseUserBudget(root: Map<String, Any>): BudgetData {
        val cloudData = root["cloudData"] as? Map<String, Any> ?: return BudgetData()
        fun slice(key: String): Pair<Double, Double> {
            val m = cloudData[key] as? Map<String, Any> ?: return 0.0 to 0.0
            val alloc = (m["allocatedBudget"] as? Number)?.toDouble() ?: 0.0
            val cur = (m["currentSpend"] as? Number)?.toDouble() ?: 0.0
            return alloc to cur
        }
        val (aa, as_) = slice("aws")
        val (ba, bs) = slice("azure")
        val (ga, gs) = slice("gcp")
        return BudgetData(
            totalBudget = (root["totalBudget"] as? Number)?.toDouble() ?: 500.0,
            awsAllocated = aa,
            awsSpent = as_,
            azureAllocated = ba,
            azureSpent = bs,
            gcpAllocated = ga,
            gcpSpent = gs
        )
    }

    private fun parseUserWaste(root: Map<String, Any>): WasteData {
        val w = root["wasteInsights"] as? Map<String, Any> ?: return WasteData()
        val items = (w["items"] as? List<Map<String, Any>>)?.map {
            WasteItem(
                provider = it["provider"] as? String ?: "",
                resourceName = it["resourceName"] as? String ?: "",
                description = it["description"] as? String ?: "",
                monthlySaving = (it["monthlySaving"] as? Number)?.toDouble() ?: 0.0,
                recommendation = it["recommendation"] as? String ?: ""
            )
        } ?: emptyList()
        return WasteData(
            totalWaste = (w["totalWaste"] as? Number)?.toDouble() ?: 0.0,
            items = items
        )
    }

    private fun parseUserTrends(root: Map<String, Any>): TrendsData {
        val daily = root["dailySpend"] as? Map<String, Any> ?: return TrendsData()
        val rows = daily.entries.sortedBy { it.key }.mapNotNull { (_, v) ->
            val m = v as? Map<String, Any> ?: return@mapNotNull null
            val rawDate = m["date"] as? String ?: ""
            val label = if (rawDate.length >= 10) {
                try {
                    val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                    val outFmt = SimpleDateFormat("EEE", Locale.US)
                    outFmt.format(inFmt.parse(rawDate.take(10))!!)
                } catch (_: Exception) {
                    rawDate
                }
            } else rawDate
            DailySpend(
                date = label,
                aws = (m["aws"] as? Number)?.toDouble() ?: 0.0,
                azure = (m["azure"] as? Number)?.toDouble() ?: 0.0,
                gcp = (m["gcp"] as? Number)?.toDouble() ?: 0.0
            )
        }
        val totals = rows.map { it.aws + it.azure + it.gcp }
        val avg = if (totals.isEmpty()) 0.0 else totals.sum() / totals.size
        return TrendsData(
            dailySpends = rows,
            avgDaily = avg,
            projected = avg * 30
        )
    }

    private fun parseUserAlerts(root: Map<String, Any>): List<AlertItem> {
        val raw = root["alerts"] as? Map<String, Any> ?: return emptyList()
        return raw.mapNotNull { (_, v) ->
            val m = v as? Map<String, Any> ?: return@mapNotNull null
            val typ = m["type"] as? String ?: ""
            val sev = when (typ) {
                "over_budget" -> "critical"
                "spend_spike" -> "warning"
                "budget_warning" -> "warning"
                else -> "ok"
            }
            val title = m["title"] as? String ?: ""
            val prov = when {
                title.contains("Azure", ignoreCase = true) -> "azure"
                title.contains("AWS", ignoreCase = true) -> "aws"
                title.contains("GCP", ignoreCase = true) -> "gcp"
                else -> "all"
            }
            AlertItem(
                severity = sev,
                title = title,
                description = m["message"] as? String ?: "",
                provider = prov,
                timeAgo = m["createdAt"] as? String ?: ""
            )
        }.sortedByDescending { it.timeAgo }
    }

    // ─── Legacy flat collections ───
    private fun parseLegacyDashboard(d: Map<String, Any>) = DashboardData(
        totalSpend = (d["totalSpend"] as? Number)?.toDouble() ?: 0.0,
        awsSpend = (d["awsSpend"] as? Number)?.toDouble() ?: 0.0,
        azureSpend = (d["azureSpend"] as? Number)?.toDouble() ?: 0.0,
        gcpSpend = (d["gcpSpend"] as? Number)?.toDouble() ?: 0.0,
        awsChange = (d["awsChange"] as? Number)?.toDouble() ?: 0.0,
        azureChange = (d["azureChange"] as? Number)?.toDouble() ?: 0.0,
        gcpChange = (d["gcpChange"] as? Number)?.toDouble() ?: 0.0,
        cloudsConnected = (d["cloudsConnected"] as? Number)?.toInt() ?: 3,
        overBudget = d["overBudget"] as? Boolean ?: false
    )

    private fun parseLegacyBudget(d: Map<String, Any>) = BudgetData(
        totalBudget = (d["totalBudget"] as? Number)?.toDouble() ?: 500.0,
        awsAllocated = (d["awsAllocated"] as? Number)?.toDouble() ?: 225.0,
        awsSpent = (d["awsSpent"] as? Number)?.toDouble() ?: 142.5,
        azureAllocated = (d["azureAllocated"] as? Number)?.toDouble() ?: 155.0,
        azureSpent = (d["azureSpent"] as? Number)?.toDouble() ?: 98.3,
        gcpAllocated = (d["gcpAllocated"] as? Number)?.toDouble() ?: 120.0,
        gcpSpent = (d["gcpSpent"] as? Number)?.toDouble() ?: 76.2
    )

    private fun parseLegacyWaste(d: Map<String, Any>): WasteData {
        val items = (d["items"] as? List<Map<String, Any>>)?.map {
            WasteItem(
                provider = it["provider"] as? String ?: "",
                resourceName = it["resourceName"] as? String ?: "",
                description = it["description"] as? String ?: "",
                monthlySaving = (it["monthlySaving"] as? Number)?.toDouble() ?: 0.0,
                recommendation = it["recommendation"] as? String ?: ""
            )
        } ?: emptyList()
        return WasteData(
            totalWaste = (d["totalWaste"] as? Number)?.toDouble() ?: 0.0,
            items = items
        )
    }

    private fun parseLegacyTrends(d: Map<String, Any>): TrendsData {
        val spends = (d["dailySpends"] as? List<Map<String, Any>>)?.map {
            DailySpend(
                date = it["date"] as? String ?: "",
                aws = (it["aws"] as? Number)?.toDouble() ?: 0.0,
                azure = (it["azure"] as? Number)?.toDouble() ?: 0.0,
                gcp = (it["gcp"] as? Number)?.toDouble() ?: 0.0
            )
        } ?: emptyList()
        return TrendsData(
            dailySpends = spends,
            avgDaily = (d["avgDaily"] as? Number)?.toDouble() ?: 0.0,
            projected = (d["projected"] as? Number)?.toDouble() ?: 0.0
        )
    }

    /** Seeds flat collections only when no rich user profile exists (avoids fighting backend seed). */
    fun seedIfEmpty() {
        val userSnap = com.google.android.gms.tasks.Tasks.await(userRef().get())
        if (userSnap.exists() && userSnap.data?.containsKey("cloudData") == true) return

        val dashDoc = com.google.android.gms.tasks.Tasks.await(
            db.collection("dashboard").document("current").get()
        )
        if (dashDoc.exists()) return

        com.google.android.gms.tasks.Tasks.await(
            db.collection("dashboard").document("current").set(
                mapOf(
                    "totalSpend" to 317.0,
                    "awsSpend" to 142.5,
                    "azureSpend" to 98.3,
                    "gcpSpend" to 76.2,
                    "awsChange" to 2.0,
                    "azureChange" to 8.0,
                    "gcpChange" to -1.0,
                    "cloudsConnected" to 3,
                    "overBudget" to false
                )
            )
        )
        com.google.android.gms.tasks.Tasks.await(
            db.collection("budget").document("current").set(
                mapOf(
                    "totalBudget" to 500.0,
                    "awsAllocated" to 225.0,
                    "awsSpent" to 142.5,
                    "azureAllocated" to 155.0,
                    "azureSpent" to 98.3,
                    "gcpAllocated" to 120.0,
                    "gcpSpent" to 76.2
                )
            )
        )
        com.google.android.gms.tasks.Tasks.await(
            db.collection("waste").document("current").set(
                mapOf(
                    "totalWaste" to 52.9,
                    "items" to listOf(
                        mapOf("provider" to "aws", "resourceName" to "EC2 t2.medium", "description" to "Running 24/7 — avg CPU below 5%", "monthlySaving" to 28.5, "recommendation" to "Resize to t2.micro or stop instance"),
                        mapOf("provider" to "azure", "resourceName" to "Standard_D2s VM", "description" to "Idle VM — 0 active connections in 7 days", "monthlySaving" to 14.2, "recommendation" to "Deallocate VM or enable auto-shutdown"),
                        mapOf("provider" to "gcp", "resourceName" to "n1-standard-2 Instance", "description" to "Dev instance left running over weekend", "monthlySaving" to 10.2, "recommendation" to "Enable auto-shutdown schedules")
                    )
                )
            )
        )
        com.google.android.gms.tasks.Tasks.await(
            db.collection("trends").document("current").set(
                mapOf(
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
                )
            )
        )
        com.google.android.gms.tasks.Tasks.await(
            db.collection("alerts").document("alert1").set(
                mapOf(
                    "severity" to "critical", "title" to "EC2 Cost Spike Detected",
                    "description" to "EC2 spend up 67% in 24 hours: \$18 above daily average",
                    "provider" to "aws", "timeAgo" to "2h ago"
                )
            )
        )
        com.google.android.gms.tasks.Tasks.await(
            db.collection("alerts").document("alert2").set(
                mapOf(
                    "severity" to "warning", "title" to "Approaching Budget Limit",
                    "description" to "Azure used 63% of its \$155.00 allocated budget",
                    "provider" to "azure", "timeAgo" to "6h ago"
                )
            )
        )
        com.google.android.gms.tasks.Tasks.await(
            db.collection("alerts").document("alert3").set(
                mapOf(
                    "severity" to "ok", "title" to "GCP Spend On Track",
                    "description" to "GCP spent \$76.20 of \$120.00 budget",
                    "provider" to "gcp", "timeAgo" to "1d ago"
                )
            )
        )
    }
}
