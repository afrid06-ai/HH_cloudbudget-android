package com.cloudbudget.app.ui.trends

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cloudbudget.app.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter

class TrendsFragment : Fragment() {

    private val viewModel: TrendsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lineChart = view.findViewById<LineChart>(R.id.lineChart)
        val tvAvgDaily = view.findViewById<TextView>(R.id.tvAvgDaily)
        val tvProjected = view.findViewById<TextView>(R.id.tvProjected)

        setupLineChart(lineChart)

        viewModel.trends.observe(viewLifecycleOwner) { data ->
            tvAvgDaily.text = "$${String.format("%.2f", data.avgDaily)}"
            tvProjected.text = "$${String.format("%.2f", data.projected)}"

            if (data.dailySpends.isNotEmpty()) {
                val labels = data.dailySpends.map { it.date }
                val awsEntries = data.dailySpends.mapIndexed { i, s -> Entry(i.toFloat(), s.aws.toFloat()) }
                val azureEntries = data.dailySpends.mapIndexed { i, s -> Entry(i.toFloat(), s.azure.toFloat()) }
                val gcpEntries = data.dailySpends.mapIndexed { i, s -> Entry(i.toFloat(), s.gcp.toFloat()) }

                val awsSet = makeLineSet(awsEntries, "AWS", "#FF9900")
                val azureSet = makeLineSet(azureEntries, "Azure", "#0089D6")
                val gcpSet = makeLineSet(gcpEntries, "GCP", "#34A853")

                lineChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                lineChart.data = LineData(awsSet, azureSet, gcpSet)
                lineChart.animateX(1000)
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun makeLineSet(entries: List<Entry>, label: String, color: String): LineDataSet {
        return LineDataSet(entries, label).apply {
            this.color = Color.parseColor(color); lineWidth = 2.5f
            setDrawCircles(true); setCircleColor(Color.parseColor(color)); circleRadius = 3f
            setDrawValues(false); mode = LineDataSet.Mode.CUBIC_BEZIER
            circleHoleColor = Color.parseColor("#0A0E1A")
        }
    }

    private fun setupLineChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true); isDragEnabled = true; setScaleEnabled(false)
            setBackgroundColor(Color.TRANSPARENT); setDrawGridBackground(false)
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.parseColor("#7A8099"); setDrawGridLines(false)
                setDrawAxisLine(false); textSize = 10f; granularity = 1f
            }
            axisLeft.apply {
                textColor = Color.parseColor("#7A8099"); setDrawGridLines(true)
                gridColor = Color.parseColor("#1A1F2F"); setDrawAxisLine(false); textSize = 10f
            }
            axisRight.isEnabled = false
        }
    }
}
