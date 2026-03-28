package com.cloudbudget.app.ui.util

import android.content.Intent
import android.view.View
import androidx.fragment.app.Fragment
import com.cloudbudget.app.R
import com.cloudbudget.app.ui.settings.ProfileActivity
import com.cloudbudget.app.ui.settings.SettingsActivity

/** Wires ⚙ and profile avatar on tab screens that use [R.id.btnHeaderSettings] / [R.id.btnHeaderProfile]. */
fun Fragment.bindStitchHeader(view: View) {
    view.findViewById<View>(R.id.btnHeaderSettings)?.setOnClickListener {
        startActivity(Intent(requireContext(), SettingsActivity::class.java))
    }
    view.findViewById<View>(R.id.btnHeaderProfile)?.setOnClickListener {
        startActivity(Intent(requireContext(), ProfileActivity::class.java))
    }
}
