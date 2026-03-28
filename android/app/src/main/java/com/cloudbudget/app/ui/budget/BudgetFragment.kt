package com.cloudbudget.app.ui.budget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cloudbudget.app.R
import com.cloudbudget.app.ui.util.bindStitchHeader

class BudgetFragment : Fragment() {

    private val viewModel: BudgetViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvBudgetAmount = view.findViewById<TextView>(R.id.tvBudgetAmount)
        val awsBar = view.findViewById<View>(R.id.awsProgressBar)
        val azureBar = view.findViewById<View>(R.id.azureProgressBar)
        val gcpBar = view.findViewById<View>(R.id.gcpProgressBar)

        viewModel.budget.observe(viewLifecycleOwner) { data ->
            tvBudgetAmount.text = "$${String.format("%.2f", data.totalBudget)}"

            setProgress(awsBar, data.awsSpent, data.awsAllocated)
            setProgress(azureBar, data.azureSpent, data.azureAllocated)
            setProgress(gcpBar, data.gcpSpent, data.gcpAllocated)

            // Update text fields dynamically
            updateAllocationCard(view, R.id.tvAwsBudgetSpent, R.id.tvAwsRemaining, R.id.tvAwsStatus,
                "AWS", data.awsSpent, data.awsAllocated)
            updateAllocationCard(view, R.id.tvAzureBudgetSpent, R.id.tvAzureRemaining, R.id.tvAzureStatus,
                "Azure", data.azureSpent, data.azureAllocated)
            updateAllocationCard(view, R.id.tvGcpBudgetSpent, R.id.tvGcpRemaining, R.id.tvGcpStatus,
                "GCP", data.gcpSpent, data.gcpAllocated)
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        view.findViewById<TextView>(R.id.btnAutoAllocate).setOnClickListener {
            Toast.makeText(requireContext(), R.string.demo_auto_allocate, Toast.LENGTH_LONG).show()
        }

        bindStitchHeader(view)
    }

    private fun setProgress(bar: View, spent: Double, allocated: Double) {
        bar.post {
            val parent = bar.parent as View
            val pct = if (allocated > 0) (spent / allocated).coerceIn(0.0, 1.0) else 0.0
            val params = bar.layoutParams
            params.width = (parent.width * pct).toInt()
            bar.layoutParams = params
        }
    }

    private fun updateAllocationCard(view: View, spentId: Int, remainingId: Int, statusId: Int,
                                     provider: String, spent: Double, allocated: Double) {
        val pct = if (allocated > 0) ((spent / allocated) * 100).toInt() else 0
        val remaining = allocated - spent

        view.findViewById<TextView>(spentId)?.text = "Spent: $${String.format("%.2f", spent)}"
        view.findViewById<TextView>(remainingId)?.text = "$${String.format("%.2f", remaining)} remaining · ${pct}% used"

        val statusView = view.findViewById<TextView>(statusId) ?: return
        when {
            pct >= 90 -> { statusView.text = "🔴 OVER BUDGET"; statusView.setBackgroundResource(R.drawable.bg_chip_error) }
            pct >= 70 -> { statusView.text = "⚠️ MONITOR"; statusView.setBackgroundResource(R.drawable.bg_chip_tertiary) }
            else -> { statusView.text = "✅ ON TRACK"; statusView.setBackgroundResource(R.drawable.bg_chip_secondary) }
        }
    }
}
