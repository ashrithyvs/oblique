package com.example.oblique_android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class PermissionsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        val btnGrant: Button = findViewById(R.id.btnGrant)
        btnGrant.setOnClickListener {
            // TODO: trigger actual permission requests here
            if (PermissionUtils.hasRequiredPermissions(this)) {
                startActivity(Intent(this, PinSetupActivity::class.java))
                finish()
            }
        }
    }
}
