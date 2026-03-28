package com.cloudbudget.app.ui.dashboard

import android.graphics.Color
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.cloudbudget.app.MainActivity
import com.cloudbudget.app.R
import com.cloudbudget.app.ui.detail.CloudDetailActivity
import com.cloudbudget.app.ui.settings.ProfileActivity
import com.cloudbudget.app.ui.settings.SettingsActivity
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.animation.Easing

class DashboardFragment : Fragment() {

    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_dashboard, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val pieChart = view.findViewById<PieChart>(R.id.pieChart)
        val tvTotalSpend = view.findViewById<TextView>(R.id.tvTotalSpend)
        val tvBudgetRemaining = view.findViewById<TextView>(R.id.tvBudgetRemaining)
        val tvAwsSpend = view.findViewById<TextView>(R.id.tvAwsSpend)
        val tvAzureSpend = view.findViewById<TextView>(R.id.tvAzureSpend)
        val tvGcpSpend = view.findViewById<TextView>(R.id.tvGcpSpend)
        val tvAwsChange = view.findViewById<TextView>(R.id.tvAwsChange)
        val tvAzureChange = view.findViewById<TextView>(R.id.tvAzureChange)
        val tvGcpChange = view.findViewById<TextView>(R.id.tvGcpChange)
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

        setupPieChart(pieChart)

        viewModel.dashboard.observe(viewLifecycleOwner) { data ->
            // Hero card
            tvTotalSpend.text = "$${String.format("%.2f", data.totalSpend)}"

            if (data.overBudget) {
                tvBudgetRemaining.text = "OVER BUDGET!"
                tvBudgetRemaining.setTextColor(Color.parseColor("#FF716C"))
            } else {
                tvBudgetRemaining.text = "Updated just now"
                tvBudgetRemaining.setTextColor(Color.parseColor("#A7AABB"))
            }

            // Cloud cards
            tvAwsSpend.text = "$${String.format("%.2f", data.awsSpend)}"
            tvAzureSpend.text = "$${String.format("%.2f", data.azureSpend)}"
            tvGcpSpend.text = "$${String.format("%.2f", data.gcpSpend)}"

            formatChange(tvAwsChange, data.awsChange)
            formatChange(tvAzureChange, data.azureChange)
            formatChange(tvGcpChange, data.gcpChange)

            // Update donut chart
            updateChart(pieChart, data.awsSpend.toFloat(), data.azureSpend.toFloat(), data.gcpSpend.toFloat(), data.totalSpend)
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.loading.observe(viewLifecycleOwner) { loading ->
            swipeRefresh.isRefreshing = loading
        }

        swipeRefresh.setColorSchemeColors(Color.parseColor("#61CDFF"), Color.parseColor("#00FEB1"))
        swipeRefresh.setProgressBackgroundColorSchemeColor(Color.parseColor("#141928"))
        swipeRefresh.setOnRefreshListener { viewModel.loadDashboard() }

        view.findViewById<TextView>(R.id.btnViewTrends).setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_trends)
        }
        view.findViewById<TextView>(R.id.btnSetBudget).setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_budget)
        }

        view.findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(requireContext(), SettingsActivity::class.java))
        }
        view.findViewById<View>(R.id.btnProfileHeader).setOnClickListener {
            startActivity(Intent(requireContext(), ProfileActivity::class.java))
        }
        view.findViewById<View>(R.id.cardAws).setOnClickListener {
            startActivity(CloudDetailActivity.intent(requireContext(), "aws"))
        }
        view.findViewById<View>(R.id.cardAzure).setOnClickListener {
            startActivity(CloudDetailActivity.intent(requireContext(), "azure"))
        }
        view.findViewById<View>(R.id.cardGcp).setOnClickListener {
            startActivity(CloudDetailActivity.intent(requireContext(), "gcp"))
        }
    }

    private fun formatChange(tv: TextView, change: Double) {
        val arrow = if (change >= 0) "↑" else "↓"
        tv.text = "$arrow ${String.format("%.0f", kotlin.math.abs(change))}%"
        if (change > 5) {
            tv.setTextColor(Color.parseColor("#FF716C"))
            tv.setBackgroundResource(R.drawable.bg_chip_error)
        } else {
            tv.setTextColor(Color.parseColor("#00FEB1"))
            tv.setBackgroundResource(R.drawable.bg_chip_secondary)
        }
    }

    private fun updateChart(chart: PieChart, aws: Float, azure: Float, gcp: Float, total: Double) {
        val entries = listOf(PieEntry(aws, "AWS"), PieEntry(azure, "Azure"), PieEntry(gcp, "GCP"))
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(Color.parseColor("#FFAC52"), Color.parseColor("#61CDFF"), Color.parseColor("#00EEA6"))
            setDrawValues(false)
            sliceSpace = 3f
        }
        chart.centerText = "$${String.format("%.2f", total)}\nThis Month"
        chart.data = PieData(dataSet)
        chart.animateY(800, Easing.EaseInOutQuad)
    }

    private fun setupPieChart(chart: PieChart) {
        chart.apply {
            setUsePercentValues(false)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 55f
            transparentCircleRadius = 58f
            setTransparentCircleColor(Color.parseColor("#0E1320"))
            setTransparentCircleAlpha(180)
            setDrawCenterText(true)
            setCenterTextColor(Color.parseColor("#E2E4F6"))
            setCenterTextSize(14f)
            legend.isEnabled = false
            setDrawEntryLabels(false)
            setTouchEnabled(false)
        }
    }
}
