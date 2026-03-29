package com.cloudbudget.app

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.cloudbudget.app.data.DemoPreferences
import com.cloudbudget.app.ui.alerts.AlertsFragment
import com.cloudbudget.app.ui.auth.CloudCredentialsActivity
import com.cloudbudget.app.ui.auth.LoginActivity
import com.cloudbudget.app.ui.budget.BudgetFragment
import com.cloudbudget.app.ui.dashboard.DashboardFragment
import com.cloudbudget.app.ui.settings.ManageCloudsActivity
import com.cloudbudget.app.ui.settings.ProfileActivity
import com.cloudbudget.app.ui.settings.SettingsActivity
import com.cloudbudget.app.ui.trends.TrendsFragment
import com.cloudbudget.app.ui.waste.WasteFragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        navView = findViewById(R.id.navView)

        navView.apply {
            getHeaderView(0).findViewById<TextView>(R.id.navHeaderEmail)?.text =
                DemoPreferences.getRegisteredEmail(this@MainActivity).ifEmpty {
                    getString(R.string.drawer_guest_hint)
                }

            setNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.nav_drawer_profile ->
                        startActivity(Intent(this@MainActivity, ProfileActivity::class.java))
                    R.id.nav_drawer_settings ->
                        startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                    R.id.nav_drawer_manage_clouds ->
                        startActivity(Intent(this@MainActivity, ManageCloudsActivity::class.java))
                    R.id.nav_drawer_credentials ->
                        startActivity(Intent(this@MainActivity, CloudCredentialsActivity::class.java))
                    R.id.nav_drawer_logout -> {
                        drawerLayout.closeDrawer(GravityCompat.START)
                        performLogout()
                        return@setNavigationItemSelectedListener true
                    }
                }
                drawerLayout.closeDrawer(GravityCompat.START)
                true
            }
        }

        syncLogoutMenuVisibility()

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNav)

        if (savedInstanceState == null) {
            loadFragment(DashboardFragment())
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> loadFragment(DashboardFragment())
                R.id.nav_budget -> loadFragment(BudgetFragment())
                R.id.nav_waste -> loadFragment(WasteFragment())
                R.id.nav_trends -> loadFragment(TrendsFragment())
                R.id.nav_alerts -> loadFragment(AlertsFragment())
                else -> false
            }
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                        drawerLayout.closeDrawer(GravityCompat.START)
                    } else {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        navView.getHeaderView(0).findViewById<TextView>(R.id.navHeaderEmail)?.text =
            DemoPreferences.getRegisteredEmail(this).ifEmpty {
                getString(R.string.drawer_guest_hint)
            }
        syncLogoutMenuVisibility()
    }

    /**
     * Show Log out only for accounts created via sign in / sign up.
     * "Skip to app" sets [DemoPreferences.isLoggedIn] but not [DemoPreferences.hasRegisteredUser] — no logout for that demo-only session.
     */
    private fun syncLogoutMenuVisibility() {
        val show = DemoPreferences.hasRegisteredUser(this)
        navView.menu.findItem(R.id.nav_drawer_logout)?.isVisible = show
    }

    private fun performLogout() {
        DemoPreferences.clearSession(this)
        Toast.makeText(this, R.string.logged_out_message, Toast.LENGTH_SHORT).show()
        startActivity(
            Intent(this, LoginActivity::class.java).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            )
        )
        finish()
    }

    fun openDrawer() {
        drawerLayout.openDrawer(GravityCompat.START)
    }

    /** Switches bottom navigation and loads the matching fragment (used by dashboard quick actions). */
    fun selectTab(@IdRes menuItemId: Int) {
        findViewById<BottomNavigationView>(R.id.bottomNav).selectedItemId = menuItemId
    }

    private fun loadFragment(fragment: Fragment): Boolean {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .commit()
        return true
    }
}
