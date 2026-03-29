package com.cloudbudget.app.data

import android.content.Context
import com.google.firebase.auth.FirebaseAuth

/** Local prefs for hackathon demo flows (no server). */
object DemoPreferences {
    private const val PREF = "cloudbudget_demo"

    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_NOTIF_BUDGET = "notif_budget"
    private const val KEY_NOTIF_WASTE = "notif_waste"
    private const val KEY_NOTIF_ANOMALY = "notif_anomaly"
    private const val KEY_CREDENTIALS_SETUP = "credentials_setup"
    /** Demo local account (not production auth — hackathon only). */
    private const val KEY_REGISTERED_EMAIL = "registered_email"
    private const val KEY_REGISTERED_PASSWORD = "registered_password"
    private const val KEY_REGISTERED_NAME = "registered_name"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun isLoggedIn(ctx: Context) = prefs(ctx).getBoolean(KEY_LOGGED_IN, false)

    fun setLoggedIn(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_LOGGED_IN, value).apply()
    }

    fun hasCompletedOnboarding(ctx: Context) = prefs(ctx).getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingDone(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()
    }

    fun getNotifBudget(ctx: Context) = prefs(ctx).getBoolean(KEY_NOTIF_BUDGET, true)
    fun getNotifWaste(ctx: Context) = prefs(ctx).getBoolean(KEY_NOTIF_WASTE, true)
    fun getNotifAnomaly(ctx: Context) = prefs(ctx).getBoolean(KEY_NOTIF_ANOMALY, true)

    fun setNotifBudget(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_NOTIF_BUDGET, v).apply()
    fun setNotifWaste(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_NOTIF_WASTE, v).apply()
    fun setNotifAnomaly(ctx: Context, v: Boolean) = prefs(ctx).edit().putBoolean(KEY_NOTIF_ANOMALY, v).apply()

    fun hasCompletedCredentialsSetup(ctx: Context) = prefs(ctx).getBoolean(KEY_CREDENTIALS_SETUP, false)

    fun setCredentialsSetupDone(ctx: Context, value: Boolean) {
        prefs(ctx).edit().putBoolean(KEY_CREDENTIALS_SETUP, value).apply()
    }

    fun clearSession(ctx: Context) {
        FirebaseAuth.getInstance().signOut()
        prefs(ctx).edit()
            .putBoolean(KEY_LOGGED_IN, false)
            .putBoolean(KEY_CREDENTIALS_SETUP, false)
            .apply()
    }

    fun hasRegisteredUser(ctx: Context): Boolean {
        val e = prefs(ctx).getString(KEY_REGISTERED_EMAIL, null)
        return !e.isNullOrBlank()
    }

    fun getRegisteredEmail(ctx: Context): String =
        prefs(ctx).getString(KEY_REGISTERED_EMAIL, "") ?: ""

    fun getRegisteredName(ctx: Context): String =
        prefs(ctx).getString(KEY_REGISTERED_NAME, "") ?: ""

    fun getRegisteredPassword(ctx: Context): String =
        prefs(ctx).getString(KEY_REGISTERED_PASSWORD, "") ?: ""

    /** Saves demo account locally. Password stored in plain text — demo only. */
    fun registerDemoUser(ctx: Context, email: String, password: String, displayName: String) {
        prefs(ctx).edit()
            .putString(KEY_REGISTERED_EMAIL, email.trim().lowercase())
            .putString(KEY_REGISTERED_PASSWORD, password)
            .putString(KEY_REGISTERED_NAME, displayName.trim())
            .apply()
    }

    fun validateDemoLogin(ctx: Context, email: String, password: String): Boolean {
        val savedEmail = prefs(ctx).getString(KEY_REGISTERED_EMAIL, null) ?: return false
        val savedPass = prefs(ctx).getString(KEY_REGISTERED_PASSWORD, null) ?: return false
        return savedEmail == email.trim().lowercase() && savedPass == password
    }
}
