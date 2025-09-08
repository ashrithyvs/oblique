// app/src/main/java/com/example/oblique_android/ObliqueApp.kt
package com.example.oblique_android

import android.app.Application
import com.example.oblique_android.services.Prefs

class ObliqueApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)  // ✅ initialize SharedPreferences singleton
    }
}
