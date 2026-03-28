package com.cloudbudget.app.ui.settings

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cloudbudget.app.R

class ManageCloudsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_clouds)

        findViewById<TextView>(R.id.tvScreenTitle).setText(R.string.title_manage_clouds)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
    }
}
