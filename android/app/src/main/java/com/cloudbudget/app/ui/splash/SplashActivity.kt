package com.cloudbudget.app.ui.splash

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.cloudbudget.app.R
import com.cloudbudget.app.MainActivity
import com.cloudbudget.app.data.DemoPreferences
import com.cloudbudget.app.ui.auth.CloudCredentialsActivity
import com.cloudbudget.app.ui.auth.LoginActivity
import com.cloudbudget.app.ui.onboarding.OnboardingActivity

class SplashActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val cloudAzure = findViewById<TextView>(R.id.cloudAzure)
        val cloudAws = findViewById<TextView>(R.id.cloudAws)
        val cloudGcp = findViewById<TextView>(R.id.cloudGcp)
        val tvAppName = findViewById<TextView>(R.id.tvAppName)
        val tvTagline = findViewById<TextView>(R.id.tvTagline)
        val splashGlow = findViewById<View>(R.id.splashGlow)
        val footerLayout = findViewById<View>(R.id.footerLayout)
        val progressBar = findViewById<View>(R.id.splashProgress)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val particleContainer = findViewById<FrameLayout>(R.id.particleContainer)

        // Seed Firestore demo data in background
        Thread {
            try { com.cloudbudget.app.data.firebase.FirestoreRepository.seedIfEmpty() } catch (_: Exception) {}
        }.start()

        // Spawn floating particles
        spawnParticles(particleContainer)

        // Phase 1: Glow fades in (0ms)
        splashGlow.animate().alpha(0.8f).setDuration(1200).setStartDelay(100).start()

        // Phase 2: Clouds fly in with bounce (200ms stagger)
        cloudAzure.translationY = -80f
        cloudAzure.animate().alpha(1f).translationY(0f).setDuration(800)
            .setStartDelay(300).setInterpolator(OvershootInterpolator(1.5f)).start()

        cloudAws.translationX = -60f
        cloudAws.animate().alpha(1f).translationX(0f).setDuration(700)
            .setStartDelay(500).setInterpolator(OvershootInterpolator(1.2f)).start()

        cloudGcp.translationX = 60f
        cloudGcp.animate().alpha(1f).translationX(0f).setDuration(700)
            .setStartDelay(600).setInterpolator(OvershootInterpolator(1.2f)).start()

        // Cloud pulsing glow animation
        handler.postDelayed({
            pulseView(cloudAzure)
            pulseView(cloudAws)
            pulseView(cloudGcp)
        }, 1200)

        // Phase 3: Text fades in
        tvAppName.translationY = 30f
        tvAppName.animate().alpha(1f).translationY(0f).setDuration(600).setStartDelay(900).start()

        tvTagline.animate().alpha(1f).setDuration(500).setStartDelay(1200).start()

        // Phase 4: Footer fades in
        footerLayout.animate().alpha(1f).setDuration(500).setStartDelay(1000).start()

        // Phase 5: Progress bar with gradient animation
        progressBar.post {
            val parent = progressBar.parent as View
            val targetWidth = parent.width
            val animator = ValueAnimator.ofInt(0, targetWidth).apply {
                duration = 2200
                startDelay = 800
                interpolator = AccelerateDecelerateInterpolator()
                addUpdateListener {
                    val params = progressBar.layoutParams
                    params.width = it.animatedValue as Int
                    progressBar.layoutParams = params
                }
            }
            animator.start()
        }

        // Phase 6: Status text color cycling
        animateStatusText(tvStatus)

        // Navigate after animations complete (demo: resume session if already logged in)
        handler.postDelayed({
            when {
                DemoPreferences.isLoggedIn(this) && !DemoPreferences.hasCompletedCredentialsSetup(this) ->
                    startActivity(
                        Intent(this, CloudCredentialsActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                DemoPreferences.isLoggedIn(this) ->
                    startActivity(
                        Intent(this, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                DemoPreferences.hasCompletedOnboarding(this) ->
                    startActivity(Intent(this, LoginActivity::class.java))
                else ->
                    startActivity(Intent(this, OnboardingActivity::class.java))
            }
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        }, 3500)
    }

    private fun pulseView(view: View) {
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1f, 1.08f, 1f).apply { repeatCount = ValueAnimator.INFINITE; duration = 2500 }
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1f, 1.08f, 1f).apply { repeatCount = ValueAnimator.INFINITE; duration = 2500 }
        AnimatorSet().apply { playTogether(scaleX, scaleY); start() }
    }

    private fun animateStatusText(tv: TextView) {
        val colors = intArrayOf(
            Color.parseColor("#717584"),
            Color.parseColor("#61CDFF"),
            Color.parseColor("#00FEB1"),
            Color.parseColor("#FFAC52"),
            Color.parseColor("#61CDFF")
        )
        val messages = arrayOf(
            "Connecting to your clouds\u2026",
            "Syncing AWS data\u2026",
            "Syncing Azure data\u2026",
            "Syncing GCP data\u2026",
            "Almost ready\u2026"
        )
        for (i in colors.indices) {
            handler.postDelayed({
                tv.text = messages[i]
                tv.animate().alpha(0f).setDuration(150).withEndAction {
                    tv.setTextColor(colors[i])
                    tv.animate().alpha(1f).setDuration(200).start()
                }.start()
            }, (800 + i * 600).toLong())
        }
    }

    private fun spawnParticles(container: FrameLayout) {
        val colors = intArrayOf(
            Color.parseColor("#61CDFF"),
            Color.parseColor("#00FEB1"),
            Color.parseColor("#FFAC52"),
            Color.WHITE
        )
        val random = java.util.Random()

        for (i in 0 until 20) {
            val dot = View(this).apply {
                val size = (2 + random.nextInt(4))
                layoutParams = FrameLayout.LayoutParams(size * 2, size * 2).apply {
                    leftMargin = random.nextInt(resources.displayMetrics.widthPixels)
                    topMargin = random.nextInt(resources.displayMetrics.heightPixels)
                }
                setBackgroundColor(colors[random.nextInt(colors.size)])
                alpha = 0.1f + random.nextFloat() * 0.4f
            }
            container.addView(dot)

            // Floating animation
            val duration = (3000 + random.nextInt(4000)).toLong()
            val driftY = (-30 + random.nextInt(60)).toFloat()
            val driftX = (-20 + random.nextInt(40)).toFloat()

            dot.animate()
                .translationYBy(driftY)
                .translationXBy(driftX)
                .alpha(0f)
                .setDuration(duration)
                .setStartDelay(random.nextInt(2000).toLong())
                .withEndAction {
                    dot.translationX = 0f
                    dot.translationY = 0f
                    dot.alpha = 0.1f + random.nextFloat() * 0.3f
                    dot.animate()
                        .translationYBy(-driftY)
                        .translationXBy(-driftX)
                        .alpha(0f)
                        .setDuration(duration)
                        .start()
                }
                .start()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
