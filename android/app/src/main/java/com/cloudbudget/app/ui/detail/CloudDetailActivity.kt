package com.cloudbudget.app.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cloudbudget.app.R

class CloudDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_detail)

        val provider = intent.getStringExtra(EXTRA_PROVIDER)?.lowercase().orEmpty()

        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnOpenConsole).setOnClickListener {
            Toast.makeText(this, R.string.open_provider_console, Toast.LENGTH_SHORT).show()
        }

        val (title, spendHint, subtitle) = when (provider) {
            "aws" -> Triple("AWS", "$142.50", getString(R.string.cloud_detail_aws_body))
            "azure" -> Triple("Azure", "$98.30", getString(R.string.cloud_detail_azure_body))
            "gcp" -> Triple("GCP", "$76.20", getString(R.string.cloud_detail_gcp_body))
            else -> Triple(provider.uppercase(), "—", getString(R.string.manage_clouds_body))
        }

        findViewById<TextView>(R.id.tvScreenTitle).text = title
        findViewById<TextView>(R.id.tvCloudSpend).text = spendHint
        findViewById<TextView>(R.id.tvCloudSubtitle).text = subtitle

        val spendColor = when (provider) {
            "aws" -> ContextCompat.getColor(this, R.color.tertiary)
            "azure" -> ContextCompat.getColor(this, R.color.primary)
            "gcp" -> ContextCompat.getColor(this, R.color.secondary_dim)
            else -> ContextCompat.getColor(this, R.color.on_surface)
        }
        findViewById<TextView>(R.id.tvCloudSpend).setTextColor(spendColor)
    }

    companion object {
        const val EXTRA_PROVIDER = "provider"

        fun intent(ctx: Context, provider: String) =
            Intent(ctx, CloudDetailActivity::class.java).putExtra(EXTRA_PROVIDER, provider)
    }
}
