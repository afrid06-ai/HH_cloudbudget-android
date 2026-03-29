package com.cloudbudget.app.ui.settings

import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cloudbudget.app.R
import com.cloudbudget.app.data.DemoPreferences
import com.cloudbudget.app.data.firebase.FirestoreRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvInitials: TextView
    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        findViewById<TextView>(R.id.tvScreenTitle).setText(R.string.title_profile)
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }

        tvInitials = findViewById(R.id.tvInitials)
        tvName = findViewById(R.id.tvProfileName)
        tvEmail = findViewById(R.id.tvProfileEmail)

        // Local prefs are the source of truth for name/email (user just signed up with these)
        // Firestore is updated async, so prefer local first
        lifecycleScope.launch {
            FirestoreRepository.profileFlow().collectLatest { profile ->
                val localName = DemoPreferences.getRegisteredName(this@ProfileActivity)
                val localEmail = DemoPreferences.getRegisteredEmail(this@ProfileActivity)
                val name = localName.ifEmpty { profile.displayName.ifEmpty { "Guest User" } }
                val email = localEmail.ifEmpty { profile.email.ifEmpty { "guest@cloudbudget.app" } }
                tvName.text = name
                tvEmail.text = email
                tvInitials.text = name.split(" ")
                    .filter { it.isNotEmpty() }
                    .take(2)
                    .joinToString("") { it.first().uppercase() }
                    .ifEmpty { "G" }
            }
        }

        findViewById<TextView>(R.id.btnEditProfile).setOnClickListener { showEditDialog() }
    }

    private fun showEditDialog() {
        val currentName = tvName.text.toString()
        val currentEmail = tvEmail.text.toString()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        val etName = EditText(this).apply {
            hint = "Display Name"
            setText(currentName)
            setSelection(text.length)
        }
        val etEmail = EditText(this).apply {
            hint = "Email"
            setText(currentEmail)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        layout.addView(etName)
        layout.addView(etEmail)

        AlertDialog.Builder(this)
            .setTitle("Edit Profile")
            .setView(layout)
            .setPositiveButton("Save") { _, _ ->
                val newName = etName.text.toString().trim()
                val newEmail = etEmail.text.toString().trim()
                if (newName.isNotEmpty() && newEmail.isNotEmpty()) {
                    // Update both Firestore and local
                    FirestoreRepository.updateProfile(newName, newEmail)
                    val pass = DemoPreferences.getRegisteredPassword(this)
                    DemoPreferences.registerDemoUser(this, newEmail, pass, newName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
