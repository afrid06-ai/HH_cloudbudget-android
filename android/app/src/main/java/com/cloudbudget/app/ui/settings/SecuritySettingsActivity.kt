package com.cloudbudget.app.ui.settings

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cloudbudget.app.R

class SecuritySettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_security_settings)

        findViewById<TextView>(R.id.tvScreenTitle).setText(R.string.title_security)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnChangePassword).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.change_password)
                .setMessage(R.string.security_demo_password)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        findViewById<TextView>(R.id.btnTwoFactor).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.two_factor_auth)
                .setMessage(R.string.security_demo_2fa)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
        findViewById<TextView>(R.id.btnActiveSessions).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.active_sessions)
                .setMessage(R.string.security_demo_sessions)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }
}
