package com.cloudbudget.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.cloudbudget.app.MainActivity
import com.cloudbudget.app.R
import com.cloudbudget.app.data.DemoPreferences

/** Stitch: login_screen — demo auth only; no backend calls. */
class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<TextView>(R.id.btnSignIn).setOnClickListener {
            val email = findViewById<android.widget.EditText>(R.id.etEmail).text?.toString().orEmpty().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, R.string.enter_email_demo, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            startActivity(Intent(this, CloudCredentialsActivity::class.java))
            finish()
        }
        findViewById<TextView>(R.id.btnSkipToApp).setOnClickListener { goMain() }
        findViewById<TextView>(R.id.btnGoogle).setOnClickListener {
            Toast.makeText(this, R.string.continue_with_google, Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.btnForgot).setOnClickListener {
            Toast.makeText(this, R.string.forgot_password, Toast.LENGTH_SHORT).show()
        }
        findViewById<TextView>(R.id.btnRegister).setOnClickListener {
            Toast.makeText(this, R.string.no_account_register, Toast.LENGTH_SHORT).show()
        }
    }

    private fun goMain() {
        DemoPreferences.setLoggedIn(this, true)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
