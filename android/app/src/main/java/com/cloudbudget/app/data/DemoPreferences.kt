package com.cloudbudget.app.data

import android.content.Context

/** Local prefs for hackathon demo flows (no server). */
object DemoPreferences {
    private const val PREF = "cloudbudget_demo"

    private const val KEY_LOGGED_IN = "logged_in"
    private const val KEY_ONBOARDING_DONE = "onboarding_done"
    private const val KEY_NOTIF_BUDGET = "notif_budget"
    private const val KEY_NOTIF_WASTE = "notif_waste"
    private const val KEY_NOTIF_ANOMALY = "notif_anomaly"

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

    fun clearSession(ctx: Context) {
        prefs(ctx).edit().putBoolean(KEY_LOGGED_IN, false).apply()
    }
}
