package com.cloudbudget.app.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cloudbudget.app.R

class AlertDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alert_detail)

        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val description = intent.getStringExtra(EXTRA_DESCRIPTION).orEmpty()
        val severity = intent.getStringExtra(EXTRA_SEVERITY).orEmpty()
        val provider = intent.getStringExtra(EXTRA_PROVIDER).orEmpty()
        val timeAgo = intent.getStringExtra(EXTRA_TIME_AGO).orEmpty()

        findViewById<TextView>(R.id.tvScreenTitle).setText(R.string.title_alert_detail)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvAlertTitle).text = title
        findViewById<TextView>(R.id.tvAlertDescription).text = description
        findViewById<TextView>(R.id.tvSeverity).text = severity.uppercase()
        findViewById<TextView>(R.id.tvAlertMeta).text = listOf(provider, timeAgo).filter { it.isNotBlank() }.joinToString(" · ")

        findViewById<TextView>(R.id.btnAcknowledge).setOnClickListener { finish() }
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_SEVERITY = "severity"
        const val EXTRA_PROVIDER = "provider"
        const val EXTRA_TIME_AGO = "time_ago"

        fun intent(
            ctx: Context,
            title: String,
            description: String,
            severity: String,
            provider: String,
            timeAgo: String
        ) = Intent(ctx, AlertDetailActivity::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_DESCRIPTION, description)
            putExtra(EXTRA_SEVERITY, severity)
            putExtra(EXTRA_PROVIDER, provider)
            putExtra(EXTRA_TIME_AGO, timeAgo)
        }
    }
}
