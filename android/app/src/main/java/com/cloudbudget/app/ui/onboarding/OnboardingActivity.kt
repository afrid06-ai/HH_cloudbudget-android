package com.cloudbudget.app.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cloudbudget.app.R
import com.cloudbudget.app.data.DemoPreferences
import com.cloudbudget.app.ui.auth.LoginActivity

class OnboardingActivity : AppCompatActivity() {

    private var currentPage = 0
    private val titles = arrayOf(
        "All Your Clouds. One Place.",
        "Smart Budget Alerts",
        "Detect Waste Instantly"
    )
    private val descriptions = arrayOf(
        "Stop switching between AWS, Azure and GCP dashboards. CloudBudget shows all your cloud spending in one unified view.",
        "Set budgets per cloud and get instant alerts when spending approaches your limits. Never be surprised by a bill again.",
        "Our AI scans your infrastructure for idle resources, oversized instances, and unused storage — saving you money every day."
    )
    private val icons = arrayOf("📱", "🔔", "🔥")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        val title = findViewById<TextView>(R.id.onboardTitle)
        val desc = findViewById<TextView>(R.id.onboardDesc)
        val icon = findViewById<TextView>(R.id.illustrationIcon)
        val btnNext = findViewById<TextView>(R.id.btnNext)
        val btnSkip = findViewById<TextView>(R.id.btnSkip)
        val dot1 = findViewById<View>(R.id.dot1)
        val dot2 = findViewById<View>(R.id.dot2)
        val dot3 = findViewById<View>(R.id.dot3)
        val dots = arrayOf(dot1, dot2, dot3)

        fun updatePage() {
            title.text = titles[currentPage]
            desc.text = descriptions[currentPage]
            icon.text = icons[currentPage]

            dots.forEachIndexed { i, dot ->
                if (i == currentPage) {
                    dot.layoutParams.width = resources.getDimensionPixelSize(R.dimen.dot_active_width)
                    dot.setBackgroundResource(R.drawable.bg_onboarding_dot_active)
                } else {
                    dot.layoutParams.width = resources.getDimensionPixelSize(R.dimen.dot_inactive_width)
                    dot.setBackgroundResource(R.drawable.bg_onboarding_dot_inactive)
                }
                dot.requestLayout()
            }

            btnNext.text = if (currentPage == 2) "Get Started  →" else "Next  →"
        }

        btnNext.setOnClickListener {
            if (currentPage < 2) {
                currentPage++
                updatePage()
            } else {
                goToMain()
            }
        }

        btnSkip.setOnClickListener { goToMain() }

        updatePage()
    }

    private fun goToMain() {
        DemoPreferences.setOnboardingDone(this, true)
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
