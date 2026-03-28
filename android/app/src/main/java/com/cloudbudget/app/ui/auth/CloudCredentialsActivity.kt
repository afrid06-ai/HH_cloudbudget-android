package com.cloudbudget.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cloudbudget.app.MainActivity
import com.cloudbudget.app.R
import com.cloudbudget.app.data.DemoPreferences

/** Stitch: cloud_credentials_setup — UI only; keys are not sent to a server in this demo. */
class CloudCredentialsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cloud_credentials)

        findViewById<TextView>(R.id.tvScreenTitle).setText(R.string.title_cloud_credentials)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.btnContinue).setOnClickListener { goMain() }
        findViewById<TextView>(R.id.btnSkipCredentials).setOnClickListener { goMain() }
    }

    private fun goMain() {
        DemoPreferences.setLoggedIn(this, true)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
