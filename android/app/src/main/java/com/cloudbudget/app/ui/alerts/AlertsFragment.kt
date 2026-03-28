package com.cloudbudget.app.ui.alerts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbudget.app.R
import com.cloudbudget.app.data.firebase.FirestoreRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class AlertsViewModel : ViewModel() {
    private val _alerts = MutableLiveData<List<FirestoreRepository.AlertItem>>()
    val alerts: LiveData<List<FirestoreRepository.AlertItem>> = _alerts
    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    init { loadAlerts() }

    fun loadAlerts() {
        viewModelScope.launch {
            FirestoreRepository.alertsFlow()
                .catch { _error.value = it.message }
                .collect { _alerts.value = it }
        }
    }
}

class AlertsFragment : Fragment() {

    private lateinit var viewModel: AlertsViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_alerts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[AlertsViewModel::class.java]

        val alertsContainer = view.findViewById<LinearLayout>(R.id.alertsContainer)
        val tvActiveCount = view.findViewById<TextView>(R.id.tvActiveCount)

        viewModel.alerts.observe(viewLifecycleOwner) { alerts ->
            tvActiveCount?.text = "${alerts.size} ACTIVE"
            alertsContainer?.removeAllViews()
            alerts.forEach { alert ->
                alertsContainer?.addView(buildAlertCard(alert))
            }
        }
    }

    private fun buildAlertCard(alert: FirestoreRepository.AlertItem): View {
        val ctx = requireContext()
        val dp = { v: Int -> (v * ctx.resources.displayMetrics.density).toInt() }

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val bgRes = if (alert.severity == "critical") R.drawable.bg_alert_critical else R.drawable.bg_glass_card
            setBackgroundResource(bgRes)
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
        }

        // Header
        val header = LinearLayout(ctx).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        val (icon, chipColor, chipBg) = when (alert.severity) {
            "critical" -> Triple("🔴 CRITICAL", Color.parseColor("#FF716C"), R.drawable.bg_chip_error)
            "warning" -> Triple("🟡 WARNING", Color.parseColor("#FFAC52"), R.drawable.bg_chip_tertiary)
            else -> Triple("🟢 OK", Color.parseColor("#00FEB1"), R.drawable.bg_chip_secondary)
        }
        header.addView(TextView(ctx).apply {
            text = icon; textSize = 10f; setTextColor(chipColor)
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(8), dp(2), dp(8), dp(2))
            setBackgroundResource(chipBg)
        })
        header.addView(View(ctx).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })
        header.addView(TextView(ctx).apply {
            text = alert.timeAgo; textSize = 11f; setTextColor(Color.parseColor("#7A8099"))
        })
        card.addView(header)

        card.addView(TextView(ctx).apply {
            text = alert.title; textSize = 16f
            setTextColor(Color.parseColor("#E8EAF0")); setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(8), 0, 0)
        })
        card.addView(TextView(ctx).apply {
            text = alert.description; textSize = 13f; setTextColor(Color.parseColor("#A7AABB"))
            setPadding(0, dp(4), 0, 0)
        })
        card.addView(TextView(ctx).apply {
            text = "View Details →"; textSize = 13f; setTextColor(Color.parseColor("#61CDFF"))
            setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(8), 0, 0)
        })

        return card
    }
}
