package com.cloudbudget.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cloudbudget.app.MainActivity
import com.cloudbudget.app.R
import com.cloudbudget.app.data.DemoPreferences

/** Login + sign up (demo: credentials stored locally in SharedPreferences only). */
class LoginActivity : AppCompatActivity() {

    private var isSignUpMode = false

    private lateinit var tvSubtitle: TextView
    private lateinit var tabSignIn: TextView
    private lateinit var tabSignUp: TextView
    private lateinit var labelName: TextView
    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var labelConfirm: TextView
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnForgot: TextView
    private lateinit var btnPrimary: TextView
    private lateinit var tvFooterAuth: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        tvSubtitle = findViewById(R.id.tvAuthSubtitle)
        tabSignIn = findViewById(R.id.tabSignIn)
        tabSignUp = findViewById(R.id.tabSignUp)
        labelName = findViewById(R.id.labelName)
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        labelConfirm = findViewById(R.id.labelConfirmPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnForgot = findViewById(R.id.btnForgot)
        btnPrimary = findViewById(R.id.btnPrimaryAuth)
        tvFooterAuth = findViewById(R.id.tvFooterAuth)

        tabSignIn.setOnClickListener { setSignUpMode(false) }
        tabSignUp.setOnClickListener { setSignUpMode(true) }

        tvFooterAuth.setOnClickListener {
            setSignUpMode(!isSignUpMode)
        }

        btnPrimary.setOnClickListener {
            if (isSignUpMode) doSignUp() else doSignIn()
        }

        findViewById<TextView>(R.id.btnSkipToApp).setOnClickListener { goMain() }
        findViewById<TextView>(R.id.btnGoogle).setOnClickListener {
            Toast.makeText(this, R.string.continue_with_google, Toast.LENGTH_SHORT).show()
        }
        btnForgot.setOnClickListener {
            Toast.makeText(this, R.string.forgot_password, Toast.LENGTH_SHORT).show()
        }

        setSignUpMode(false)
    }

    private fun setSignUpMode(signUp: Boolean) {
        isSignUpMode = signUp
        val onPrimary = ContextCompat.getColor(this, R.color.on_primary_container)
        val variant = ContextCompat.getColor(this, R.color.on_surface_variant)

        if (signUp) {
            tabSignUp.setBackgroundResource(R.drawable.bg_nav_pill)
            tabSignUp.setTextColor(onPrimary)
            tabSignIn.setBackgroundResource(R.drawable.bg_glass_card_soft)
            tabSignIn.setTextColor(variant)
            tvSubtitle.setText(R.string.sign_up_subtitle)
            labelName.visibility = View.VISIBLE
            etName.visibility = View.VISIBLE
            labelConfirm.visibility = View.VISIBLE
            etConfirmPassword.visibility = View.VISIBLE
            btnForgot.visibility = View.GONE
            btnPrimary.setText(R.string.create_account)
            tvFooterAuth.setText(R.string.already_have_account)
        } else {
            tabSignIn.setBackgroundResource(R.drawable.bg_nav_pill)
            tabSignIn.setTextColor(onPrimary)
            tabSignUp.setBackgroundResource(R.drawable.bg_glass_card_soft)
            tabSignUp.setTextColor(variant)
            tvSubtitle.setText(R.string.sign_in_subtitle)
            labelName.visibility = View.GONE
            etName.visibility = View.GONE
            labelConfirm.visibility = View.GONE
            etConfirmPassword.visibility = View.GONE
            btnForgot.visibility = View.VISIBLE
            btnPrimary.setText(R.string.sign_in)
            tvFooterAuth.setText(R.string.no_account_register)
        }
    }

    private fun doSignUp() {
        val name = etName.text?.toString().orEmpty().trim()
        val email = etEmail.text?.toString().orEmpty().trim()
        val pass = etPassword.text?.toString().orEmpty()
        val pass2 = etConfirmPassword.text?.toString().orEmpty()

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_all_signup, Toast.LENGTH_SHORT).show()
            return
        }
        if (pass.length < 6) {
            Toast.makeText(this, R.string.error_password_short, Toast.LENGTH_SHORT).show()
            return
        }
        if (pass != pass2) {
            Toast.makeText(this, R.string.error_password_mismatch, Toast.LENGTH_SHORT).show()
            return
        }

        DemoPreferences.registerDemoUser(this, email, pass, name)
        Toast.makeText(this, R.string.account_created_demo, Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, CloudCredentialsActivity::class.java))
        finish()
    }

    private fun doSignIn() {
        val email = etEmail.text?.toString().orEmpty().trim()
        val pass = etPassword.text?.toString().orEmpty()

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_login, Toast.LENGTH_SHORT).show()
            return
        }
        if (!DemoPreferences.hasRegisteredUser(this)) {
            Toast.makeText(this, R.string.error_register_first, Toast.LENGTH_SHORT).show()
            return
        }
        if (!DemoPreferences.validateDemoLogin(this, email, pass)) {
            Toast.makeText(this, R.string.error_wrong_credentials, Toast.LENGTH_SHORT).show()
            return
        }
        startActivity(Intent(this, CloudCredentialsActivity::class.java))
        finish()
    }

    private fun goMain() {
        DemoPreferences.setLoggedIn(this, true)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
