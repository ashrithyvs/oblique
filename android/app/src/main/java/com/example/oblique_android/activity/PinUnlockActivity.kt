package com.example.oblique_android.activity

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.oblique_android.R
import com.example.oblique_android.services.PINManager
import com.example.oblique_android.utils.PinKeypadController
import com.example.oblique_android.utils.TempUnlockManager
import com.example.oblique_android.utils.setupWindowInsets

class PinUnlockActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_VERIFY_FOR_RESET = "verify_for_reset"
    }

    private lateinit var keypad: PinKeypadController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pin_unlock)
        setupWindowInsets(R.id.rootPinUnlock)

        val blockedApp = intent.getStringExtra("blockedApp")
        val verifyForReset = intent.getBooleanExtra(EXTRA_VERIFY_FOR_RESET, false)

        if (verifyForReset) {
            findViewById<TextView>(R.id.tvPinTitle).setText(R.string.pin_reset_verify_title)
            findViewById<TextView>(R.id.tvPinSubtitle).setText(R.string.pin_reset_verify_subtitle)
        } else {
            findViewById<TextView>(R.id.tvPinTitle).setText(R.string.pin_unlock_title)
            findViewById<TextView>(R.id.tvPinSubtitle).setText(R.string.pin_unlock_subtitle)
        }

        keypad = PinKeypadController(findViewById(R.id.pinScreenRoot)) { entered ->
            if (!PINManager.isPinSet(applicationContext)) {
                Toast.makeText(this, R.string.pin_not_set, Toast.LENGTH_SHORT).show()
                keypad.clear()
                return@PinKeypadController
            }

            if (PINManager.verifyPin(applicationContext, entered)) {
                if (verifyForReset) {
                    startActivity(
                        Intent(this, PinSetupActivity::class.java).apply {
                            putExtra(PinSetupActivity.EXTRA_RESET_PIN, true)
                        }
                    )
                    finish()
                    return@PinKeypadController
                }

                if (blockedApp != null) {
                    TempUnlockManager.setTempUnlock(this, blockedApp, 5 * 60 * 1000L)
                }
                Toast.makeText(this, R.string.pin_unlocked, Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, R.string.pin_wrong, Toast.LENGTH_SHORT).show()
                keypad.clear()
            }
        }
    }
}
