package com.cloudbudget.app.ui.dashboard

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.cloudbudget.app.R
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
        val swipeRefresh = view.findViewById<SwipeRefreshLayout>(R.id.swipeRefresh)

        setupPieChart(pieChart)
        loadDefaultChart(pieChart)

        viewModel.dashboard.observe(viewLifecycleOwner) { data ->
            tvTotalSpend.text = "$${String.format("%.2f", data.total_spend)}"

            if (data.over_budget) {
                tvBudgetRemaining.text = "OVER BUDGET"
                tvBudgetRemaining.setTextColor(Color.parseColor("#FF716C"))
            } else {
                tvBudgetRemaining.text = "Updated just now"
                tvBudgetRemaining.setTextColor(Color.parseColor("#A7AABB"))
            }

            val entries = data.breakdown.map { PieEntry(it.amount.toFloat(), it.provider.uppercase()) }
            val dataSet = PieDataSet(entries, "").apply {
                colors = listOf(
                    Color.parseColor("#FF9900"),
                    Color.parseColor("#0089D6"),
                    Color.parseColor("#34A853")
                )
                setDrawValues(false)
            }
            pieChart.data = PieData(dataSet)
            pieChart.animateY(1000, Easing.EaseInOutQuad)
            pieChart.invalidate()
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

        viewModel.loadDashboard()
    }

    private fun setupPieChart(chart: PieChart) {
        chart.apply {
            setUsePercentValues(false)
            description.isEnabled = false
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 55f
            transparentCircleRadius = 58f
            setTransparentCircleColor(Color.parseColor("#0A0E1A"))
            setTransparentCircleAlpha(180)
            setDrawCenterText(true)
            centerText = "$317.00\nThis Month"
            setCenterTextColor(Color.parseColor("#E2E4F6"))
            setCenterTextSize(14f)
            legend.isEnabled = false
            setDrawEntryLabels(false)
            setTouchEnabled(false)
        }
    }

    private fun loadDefaultChart(chart: PieChart) {
        val entries = listOf(
            PieEntry(142.5f, "AWS"),
            PieEntry(98.3f, "Azure"),
            PieEntry(76.2f, "GCP")
        )
        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf(
                Color.parseColor("#FF9900"),
                Color.parseColor("#0089D6"),
                Color.parseColor("#34A853")
            )
            setDrawValues(false)
            sliceSpace = 3f
        }
        chart.data = PieData(dataSet)
        chart.animateY(1200, Easing.EaseInOutQuad)
    }
}
