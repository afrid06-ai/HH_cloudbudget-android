package com.cloudbudget.app

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.cloudbudget.app.ui.alerts.AlertsFragment
import com.cloudbudget.app.ui.budget.BudgetFragment
import com.cloudbudget.app.ui.dashboard.DashboardFragment
import com.cloudbudget.app.ui.trends.TrendsFragment
import com.cloudbudget.app.ui.waste.WasteFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
