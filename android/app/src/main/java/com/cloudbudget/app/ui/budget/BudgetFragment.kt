package com.cloudbudget.app.ui.budget

import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cloudbudget.app.R
import com.cloudbudget.app.data.firebase.FirestoreRepository
import com.cloudbudget.app.ui.util.bindDrawerMenu

class BudgetFragment : Fragment() {

    private val viewModel: BudgetViewModel by viewModels()
    private var currentData: FirestoreRepository.BudgetData? = null

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
            currentData = data
            tvBudgetAmount.text = "$${String.format("%.2f", data.totalBudget)}"

            setProgress(awsBar, data.awsSpent, data.awsAllocated)
            setProgress(azureBar, data.azureSpent, data.azureAllocated)
            setProgress(gcpBar, data.gcpSpent, data.gcpAllocated)

            // Update allocated labels
            view.findViewById<TextView>(R.id.tvAwsAllocated)?.text =
                "Allocated: $${String.format("%.2f", data.awsAllocated)}"
            view.findViewById<TextView>(R.id.tvAzureAllocated)?.text =
                "Allocated: $${String.format("%.2f", data.azureAllocated)}"
            view.findViewById<TextView>(R.id.tvGcpAllocated)?.text =
                "Allocated: $${String.format("%.2f", data.gcpAllocated)}"

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

        view.findViewById<TextView>(R.id.btnEditBudget).setOnClickListener {
            showEditBudgetDialog()
        }

        view.findViewById<TextView>(R.id.btnAutoAllocate).setOnClickListener {
            autoAllocate()
        }

        bindDrawerMenu(view)
    }

    private fun showEditBudgetDialog() {
        val ctx = requireContext()
        val data = currentData ?: FirestoreRepository.BudgetData()

        val layout = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        fun addField(label: String, value: Double): EditText {
            val tv = TextView(ctx).apply {
                text = label
                setTextColor(Color.parseColor("#A7AABB"))
                textSize = 12f
            }
            val et = EditText(ctx).apply {
                setText(String.format("%.2f", value))
                inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
                setSelection(text.length)
                setPadding(20, 16, 20, 16)
            }
            layout.addView(tv)
            layout.addView(et)
            return et
        }

        val etTotal = addField("Total Monthly Budget ($)", data.totalBudget)
        val etAws = addField("AWS Budget ($)", data.awsAllocated)
        val etAzure = addField("Azure Budget ($)", data.azureAllocated)
        val etGcp = addField("GCP Budget ($)", data.gcpAllocated)

        AlertDialog.Builder(ctx)
            .setTitle("Edit Budget")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val total = etTotal.text.toString().toDoubleOrNull() ?: 0.0
                val aws = etAws.text.toString().toDoubleOrNull() ?: 0.0
                val azure = etAzure.text.toString().toDoubleOrNull() ?: 0.0
                val gcp = etGcp.text.toString().toDoubleOrNull() ?: 0.0

                if (total <= 0) {
                    Toast.makeText(ctx, "Total budget must be greater than 0", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                FirestoreRepository.saveBudget(total, aws, azure, gcp)
                Toast.makeText(ctx, "Budget saved!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun autoAllocate() {
        val data = currentData ?: return
        val total = data.totalBudget
        if (total <= 0) {
            Toast.makeText(requireContext(), "Set a total budget first", Toast.LENGTH_SHORT).show()
            return
        }

        // Split based on current spend ratios
        val totalSpent = data.awsSpent + data.azureSpent + data.gcpSpent
        if (totalSpent <= 0) {
            // Equal split
            val each = total / 3.0
            FirestoreRepository.saveBudget(total, each, each, each)
        } else {
            // Proportional to spend + 20% buffer
            val awsPct = data.awsSpent / totalSpent
            val azurePct = data.azureSpent / totalSpent
            val gcpPct = data.gcpSpent / totalSpent
            FirestoreRepository.saveBudget(total, total * awsPct, total * azurePct, total * gcpPct)
        }
        Toast.makeText(requireContext(), "Budget auto-allocated based on spending!", Toast.LENGTH_SHORT).show()
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
            pct >= 90 -> { statusView.text = "OVER BUDGET"; statusView.setTextColor(Color.parseColor("#FF716C")); statusView.setBackgroundResource(R.drawable.bg_chip_error) }
            pct >= 70 -> { statusView.text = "MONITOR"; statusView.setTextColor(Color.parseColor("#FFAC52")); statusView.setBackgroundResource(R.drawable.bg_chip_tertiary) }
            else -> { statusView.text = "ON TRACK"; statusView.setTextColor(Color.parseColor("#00FEB1")); statusView.setBackgroundResource(R.drawable.bg_chip_secondary) }
        }
    }
}
