package com.cloudbudget.app.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cloudbudget.app.R
import com.cloudbudget.app.data.DemoPreferences
import com.cloudbudget.app.ui.auth.LoginActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<TextView>(R.id.tvScreenTitle).setText(R.string.title_settings)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val rowLogOut = findViewById<TextView>(R.id.rowLogOut)
        val showLogout = DemoPreferences.hasRegisteredUser(this)
        rowLogOut.visibility = if (showLogout) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.labelDangerZone).visibility =
            if (showLogout) View.VISIBLE else View.GONE

        findViewById<TextView>(R.id.rowProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<TextView>(R.id.rowManageClouds).setOnClickListener {
            startActivity(Intent(this, ManageCloudsActivity::class.java))
        }
        findViewById<TextView>(R.id.rowNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationPreferencesActivity::class.java))
        }
        findViewById<TextView>(R.id.rowSecurity).setOnClickListener {
            startActivity(Intent(this, SecuritySettingsActivity::class.java))
        }
        rowLogOut.setOnClickListener {
            DemoPreferences.clearSession(this)
            startActivity(
                Intent(this, LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
        }
    }
}

