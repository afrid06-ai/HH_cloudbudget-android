package com.cloudbudget.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.cloudbudget.app.BuildConfig
import com.cloudbudget.app.MainActivity
import com.cloudbudget.app.R
import com.cloudbudget.app.data.DemoPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

/**
 * Sign up creates a **Firebase Authentication** user (visible in Console → Authentication)
 * and a Firestore profile at `users/{uid}` (no password stored in Firestore).
 * Sign in uses Firebase Auth; legacy email/password on `users/user_001` is still accepted for older seeds.
 */
class LoginActivity : AppCompatActivity() {

    private val auth get() = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
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
            startActivity(
                Intent(this, ForgotPasswordActivity::class.java)
                    .putExtra(ForgotPasswordActivity.EXTRA_PREFILL_EMAIL, etEmail.text?.toString().orEmpty().trim())
            )
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

    private fun resetPrimaryButton() {
        btnPrimary.isEnabled = true
        btnPrimary.setText(if (isSignUpMode) R.string.create_account else R.string.sign_in)
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

        btnPrimary.isEnabled = false
        btnPrimary.text = getString(R.string.auth_creating_account)

        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: run {
                    resetPrimaryButton()
                    Toast.makeText(this, R.string.auth_signup_failed, Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val profile = mapOf(
                    "displayName" to name,
                    "email" to email.lowercase(),
                    "createdAt" to com.google.firebase.Timestamp.now()
                )
                db.collection("users").document(uid).set(profile, SetOptions.merge())
                    .addOnSuccessListener {
                        DemoPreferences.registerDemoUser(this, email, pass, name)
                        Toast.makeText(this, R.string.account_created_demo, Toast.LENGTH_SHORT).show()
                        resetPrimaryButton()
                        startActivity(Intent(this, CloudCredentialsActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        resetPrimaryButton()
                        Toast.makeText(
                            this,
                            getString(R.string.auth_profile_save_failed, e.message ?: ""),
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                resetPrimaryButton()
                val msg = when {
                    e.message?.contains("already in use", ignoreCase = true) == true ->
                        getString(R.string.auth_email_in_use)
                    else -> getString(R.string.auth_signup_failed_msg, e.message ?: "")
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            }
    }

    private fun doSignIn() {
        val email = etEmail.text?.toString().orEmpty().trim()
        val pass = etPassword.text?.toString().orEmpty()

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, R.string.error_fill_login, Toast.LENGTH_SHORT).show()
            return
        }

        btnPrimary.isEnabled = false
        btnPrimary.text = getString(R.string.auth_signing_in)

        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { result ->
                val uid = result.user?.uid ?: run {
                    resetPrimaryButton()
                    Toast.makeText(this, R.string.error_wrong_credentials, Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                db.collection("users").document(uid).get()
                    .addOnSuccessListener { doc ->
                        resetPrimaryButton()
                        val data = doc.data
                        completeLoginWithProfileData(data, email, pass)
                    }
                    .addOnFailureListener { e ->
                        resetPrimaryButton()
                        Toast.makeText(this, getString(R.string.auth_login_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                when (e) {
                    is FirebaseAuthInvalidUserException ->
                        tryLegacyFirestoreSignIn(email, pass)
                    is FirebaseAuthInvalidCredentialsException -> {
                        resetPrimaryButton()
                        Toast.makeText(this, R.string.error_wrong_credentials, Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        resetPrimaryButton()
                        Toast.makeText(this, getString(R.string.auth_login_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
                    }
                }
            }
    }

    /** Older hackathon seeds used email/password fields on `users/user_001` only (no Auth user). */
    private fun tryLegacyFirestoreSignIn(email: String, pass: String) {
        db.collection("users").document(BuildConfig.FIRESTORE_USER_ID).get()
            .addOnSuccessListener { doc ->
                resetPrimaryButton()
                if (!doc.exists()) {
                    Toast.makeText(this, R.string.error_register_first, Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val data = doc.data ?: run {
                    Toast.makeText(this, R.string.error_register_first, Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                val savedEmail = (data["email"] as? String)?.lowercase() ?: ""
                val savedPass = data["password"] as? String ?: ""
                if (savedEmail.isEmpty() || savedPass.isEmpty()) {
                    Toast.makeText(this, R.string.error_register_first, Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                if (email.lowercase() != savedEmail || pass != savedPass) {
                    Toast.makeText(this, R.string.error_wrong_credentials, Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }
                completeLoginWithProfileData(data, email, pass)
            }
            .addOnFailureListener { e ->
                resetPrimaryButton()
                Toast.makeText(this, getString(R.string.auth_login_failed, e.message ?: ""), Toast.LENGTH_SHORT).show()
            }
    }

    private fun resolveDisplayName(data: Map<String, Any>?, email: String): String {
        val fromDoc = (data?.get("displayName") as? String).orEmpty().trim()
        if (fromDoc.isNotEmpty()) return fromDoc
        val localName = DemoPreferences.getRegisteredName(this).trim()
        if (localName.isNotEmpty()) return localName
        return email.substringBefore("@").ifEmpty { "User" }
    }

    private fun completeLoginWithProfileData(data: Map<String, Any>?, email: String, pass: String) {
        val savedEmail = (data?.get("email") as? String)?.lowercase() ?: email.lowercase()
        val savedName = resolveDisplayName(data, email)
        DemoPreferences.setLoggedIn(this, true)
        DemoPreferences.registerDemoUser(this, savedEmail, pass, savedName)
        val hasCloudData = data?.containsKey("cloudData") == true
        if (hasCloudData) {
            DemoPreferences.setCredentialsSetupDone(this, true)
            startActivity(
                Intent(this, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
        } else {
            startActivity(Intent(this, CloudCredentialsActivity::class.java))
        }
        finish()
    }

    private fun goMain() {
        DemoPreferences.setLoggedIn(this, true)
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        finish()
    }
}
