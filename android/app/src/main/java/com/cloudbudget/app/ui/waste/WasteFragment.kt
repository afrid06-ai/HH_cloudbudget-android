package com.cloudbudget.app.ui.waste

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.cloudbudget.app.R
import com.cloudbudget.app.data.firebase.FirestoreRepository

class WasteFragment : Fragment() {

    private val viewModel: WasteViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_waste, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvWasteAmount = view.findViewById<TextView>(R.id.tvWasteAmount)
        val wasteContainer = view.findViewById<LinearLayout>(R.id.wasteCardsContainer)

        viewModel.waste.observe(viewLifecycleOwner) { data ->
            tvWasteAmount.text = "$${String.format("%.2f", data.totalWaste)}"

            wasteContainer?.removeAllViews()
            data.items.forEach { item ->
                wasteContainer?.addView(buildWasteCard(item))
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildWasteCard(item: FirestoreRepository.WasteItem): View {
        val ctx = requireContext()
        val dp = { v: Int -> (v * ctx.resources.displayMetrics.density).toInt() }

        val card = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            val bgRes = when (item.provider.lowercase()) {
                "aws" -> R.drawable.bg_card_aws
                "azure" -> R.drawable.bg_card_azure
                else -> R.drawable.bg_card_gcp
            }
            setBackgroundResource(bgRes)
            setPadding(dp(20), dp(20), dp(20), dp(20))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                bottomMargin = dp(12)
            }
        }

        // Header row
        val header = LinearLayout(ctx).apply { gravity = android.view.Gravity.CENTER_VERTICAL }
        val providerChip = TextView(ctx).apply {
            text = item.provider.uppercase()
            textSize = 10f; setTextColor(Color.WHITE); setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(8), dp(2), dp(8), dp(2))
            val chipBg = when (item.provider.lowercase()) {
                "aws" -> R.drawable.bg_chip_error
                "azure" -> R.drawable.bg_chip_tertiary
                else -> R.drawable.bg_chip_secondary
            }
            setBackgroundResource(chipBg)
        }
        header.addView(providerChip)
        header.addView(View(ctx).apply { layoutParams = LinearLayout.LayoutParams(0, 0, 1f) })
        header.addView(TextView(ctx).apply {
            text = "Save $${String.format("%.2f", item.monthlySaving)}"
            textSize = 13f; setTextColor(Color.parseColor("#00FEB1")); setTypeface(null, android.graphics.Typeface.BOLD)
        })
        card.addView(header)

        // Resource name
        card.addView(TextView(ctx).apply {
            text = item.resourceName; textSize = 16f
            setTextColor(Color.parseColor("#E8EAF0")); setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(0, dp(8), 0, 0)
        })

        // Description
        card.addView(TextView(ctx).apply {
            text = item.description; textSize = 12f; setTextColor(Color.parseColor("#A7AABB"))
            setPadding(0, dp(4), 0, 0)
        })

        // Recommendation
        card.addView(TextView(ctx).apply {
            text = "💡 ${item.recommendation}"; textSize = 12f; setTextColor(Color.parseColor("#7A8099"))
            setPadding(0, dp(8), 0, 0)
        })

        return card
    }
}
