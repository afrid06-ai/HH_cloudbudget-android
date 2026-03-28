package com.cloudbudget.app.ui.settings

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cloudbudget.app.R

class ProfileActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        findViewById<TextView>(R.id.tvScreenTitle).setText(R.string.title_profile)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnEditProfile).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.edit_profile)
                .setMessage(R.string.edit_profile_demo_msg)
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }
}
