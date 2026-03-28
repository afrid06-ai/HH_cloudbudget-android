package com.cloudbudget.app.ui.budget

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cloudbudget.app.R

class BudgetFragment : Fragment() {

    private val viewModel: BudgetViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set progress bar widths (63% for demo)
        listOf(R.id.awsProgressBar, R.id.azureProgressBar, R.id.gcpProgressBar).forEach { id ->
            val bar = view.findViewById<View>(id)
            bar.post {
                val parent = bar.parent as View
                val params = bar.layoutParams
                params.width = (parent.width * 0.63).toInt()
                bar.layoutParams = params
            }
        }

        viewModel.budgets.observe(viewLifecycleOwner) { budgets ->
            // Update with real data when available
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }

        viewModel.loadBudgets()
    }
}
