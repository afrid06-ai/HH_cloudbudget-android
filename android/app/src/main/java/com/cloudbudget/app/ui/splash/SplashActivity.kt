package com.cloudbudget.app.ui.splash

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import com.cloudbudget.app.R
import com.cloudbudget.app.ui.onboarding.OnboardingActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val progressBar = findViewById<View>(R.id.splashProgress)

        // Animate progress bar width
        progressBar.post {
            val parent = progressBar.parent as View
            val targetWidth = parent.width
            val animator = ObjectAnimator.ofInt(progressBar, "width", 0, targetWidth).apply {
                duration = 2500
                interpolator = AccelerateDecelerateInterpolator()
            }
            animator.addUpdateListener {
                val params = progressBar.layoutParams
                params.width = it.animatedValue as Int
                progressBar.layoutParams = params
            }
            animator.start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, OnboardingActivity::class.java))
            finish()
        }, 2800)
    }
}
