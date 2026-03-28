package com.cloudbudget.app.ui.settings

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import com.cloudbudget.app.R
import com.cloudbudget.app.data.DemoPreferences

class NotificationPreferencesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_preferences)

        findViewById<TextView>(R.id.tvScreenTitle).setText(R.string.title_notifications)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        val swBudget = findViewById<SwitchCompat>(R.id.switchBudget)
        val swWaste = findViewById<SwitchCompat>(R.id.switchWaste)
        val swAnomaly = findViewById<SwitchCompat>(R.id.switchAnomaly)

        swBudget.isChecked = DemoPreferences.getNotifBudget(this)
        swWaste.isChecked = DemoPreferences.getNotifWaste(this)
        swAnomaly.isChecked = DemoPreferences.getNotifAnomaly(this)

        swBudget.setOnCheckedChangeListener { _, v -> DemoPreferences.setNotifBudget(this, v) }
        swWaste.setOnCheckedChangeListener { _, v -> DemoPreferences.setNotifWaste(this, v) }
        swAnomaly.setOnCheckedChangeListener { _, v -> DemoPreferences.setNotifAnomaly(this, v) }
    }
}
