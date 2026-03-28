package com.cloudbudget.app.ui.trends

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cloudbudget.app.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class TrendsFragment : Fragment() {

    private val viewModel: TrendsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trends, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val lineChart = view.findViewById<LineChart>(R.id.lineChart)
        setupLineChart(lineChart)
        loadDemoData(lineChart)

        viewModel.trends.observe(viewLifecycleOwner) { trends ->
            // Update with real data when available
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.loadTrends()
    }

    private fun setupLineChart(chart: LineChart) {
        chart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(false)
            setBackgroundColor(Color.TRANSPARENT)
            setDrawGridBackground(false)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = Color.parseColor("#7A8099")
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textSize = 10f
            }
            axisLeft.apply {
                textColor = Color.parseColor("#7A8099")
                setDrawGridLines(true)
                gridColor = Color.parseColor("#1A1F2F")
                setDrawAxisLine(false)
                textSize = 10f
            }
            axisRight.isEnabled = false
        }
    }

    private fun loadDemoData(chart: LineChart) {
        val awsEntries = listOf(Entry(1f,3.2f), Entry(2f,4.1f), Entry(3f,3.8f), Entry(4f,5.2f), Entry(5f,3.5f), Entry(6f,2.9f), Entry(7f,3.3f))
        val azureEntries = listOf(Entry(1f,2.1f), Entry(2f,2.5f), Entry(3f,3.0f), Entry(4f,2.8f), Entry(5f,2.3f), Entry(6f,2.7f), Entry(7f,2.5f))
        val gcpEntries = listOf(Entry(1f,1.5f), Entry(2f,1.8f), Entry(3f,1.6f), Entry(4f,2.0f), Entry(5f,1.9f), Entry(6f,1.7f), Entry(7f,1.8f))

        val awsSet = LineDataSet(awsEntries, "AWS").apply {
            color = Color.parseColor("#FF9900"); lineWidth = 2.5f
            setDrawCircles(true); setCircleColor(Color.parseColor("#FF9900")); circleRadius = 3f
            setDrawValues(false); mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        val azureSet = LineDataSet(azureEntries, "Azure").apply {
            color = Color.parseColor("#0089D6"); lineWidth = 2.5f
            setDrawCircles(true); setCircleColor(Color.parseColor("#0089D6")); circleRadius = 3f
            setDrawValues(false); mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        val gcpSet = LineDataSet(gcpEntries, "GCP").apply {
            color = Color.parseColor("#34A853"); lineWidth = 2.5f
            setDrawCircles(true); setCircleColor(Color.parseColor("#34A853")); circleRadius = 3f
            setDrawValues(false); mode = LineDataSet.Mode.CUBIC_BEZIER
        }

        chart.data = LineData(awsSet, azureSet, gcpSet)
        chart.animateX(1200)
    }
}
