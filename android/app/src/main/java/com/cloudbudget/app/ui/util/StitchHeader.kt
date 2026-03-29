package com.cloudbudget.app.ui.util

import android.view.View
import androidx.fragment.app.Fragment
import com.cloudbudget.app.MainActivity
import com.cloudbudget.app.R

/** Hamburger on tab screens opens the main navigation drawer (Profile, Settings, …). */
fun Fragment.bindDrawerMenu(view: View) {
    view.findViewById<View>(R.id.btnOpenDrawer)?.setOnClickListener {
        (activity as? MainActivity)?.openDrawer()
    }
}
