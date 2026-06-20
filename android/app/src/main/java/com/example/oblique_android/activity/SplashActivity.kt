package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.oblique_android.prefs.TokenManager
import com.example.oblique_android.services.Prefs
import com.example.oblique_android.utils.OnboardingRouter
import kotlinx.coroutines.launch
import javax.crypto.AEADBadTagException

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Prefs.init(this)

        lifecycleScope.launch {
            try {
                val next = OnboardingRouter.nextSuspend(this@SplashActivity, validateToken = true)
                startActivity(Intent(this@SplashActivity, next))
            } catch (e: AEADBadTagException) {
                Log.e("SplashActivity", "Corrupted encrypted prefs, resetting auth...", e)
                TokenManager.getInstance(this@SplashActivity).clear()
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            } catch (e: Exception) {
                Log.e("SplashActivity", "Unexpected init error", e)
                TokenManager.getInstance(this@SplashActivity).clear()
                startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
            } finally {
                finish()
            }
        }
    }
}
