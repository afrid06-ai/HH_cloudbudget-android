package com.cloudbudget.app.ui.alerts

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbudget.app.R
import com.cloudbudget.app.data.firebase.FirestoreRepository
import com.cloudbudget.app.ui.detail.AlertDetailActivity
import com.cloudbudget.app.ui.util.bindStitchHeader
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
    private var lastAlerts: List<FirestoreRepository.AlertItem> = emptyList()
    /** null = All, else aws | azure | gcp */
    private var filterProvider: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_alerts, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[AlertsViewModel::class.java]

        val alertsContainer = view.findViewById<LinearLayout>(R.id.alertsContainer)
        val tvActiveCount = view.findViewById<TextView>(R.id.tvActiveCount)

        val filterAll = view.findViewById<TextView>(R.id.filterAll)
        val filterAws = view.findViewById<TextView>(R.id.filterAws)
        val filterAzure = view.findViewById<TextView>(R.id.filterAzure)
        val filterGcp = view.findViewById<TextView>(R.id.filterGcp)

        fun styleFilters(selected: String?) {
            val selBg = R.drawable.bg_gradient_button
            val unselBg = R.drawable.bg_glass_card_soft
            val selCol = ContextCompat.getColor(requireContext(), R.color.on_primary_fixed)
            val unselCol = ContextCompat.getColor(requireContext(), R.color.on_surface_variant)
            fun style(tv: TextView, key: String?) {
                val on = selected == key
                tv.setBackgroundResource(if (on) selBg else unselBg)
                tv.setTextColor(if (on) selCol else unselCol)
            }
            style(filterAll, null)
            style(filterAws, "aws")
            style(filterAzure, "azure")
            style(filterGcp, "gcp")
        }

        fun filtered(): List<FirestoreRepository.AlertItem> {
            val p = filterProvider ?: return lastAlerts
            return lastAlerts.filter { it.provider.equals(p, ignoreCase = true) }
        }

        fun render() {
            val list = filtered()
            tvActiveCount?.text = "${list.size} ACTIVE"
            alertsContainer?.removeAllViews()
            list.forEach { alertsContainer?.addView(buildAlertCard(it)) }
        }

        filterAll.setOnClickListener { filterProvider = null; styleFilters(filterProvider); render() }
        filterAws.setOnClickListener { filterProvider = "aws"; styleFilters(filterProvider); render() }
        filterAzure.setOnClickListener { filterProvider = "azure"; styleFilters(filterProvider); render() }
        filterGcp.setOnClickListener { filterProvider = "gcp"; styleFilters(filterProvider); render() }

        viewModel.alerts.observe(viewLifecycleOwner) { alerts ->
            lastAlerts = alerts
            render()
        }

        styleFilters(filterProvider)
        bindStitchHeader(view)
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

        card.setOnClickListener {
            startActivity(
                AlertDetailActivity.intent(
                    requireContext(),
                    title = alert.title,
                    description = alert.description,
                    severity = alert.severity,
                    provider = alert.provider,
                    timeAgo = alert.timeAgo
                )
            )
        }

        return card
    }
}
