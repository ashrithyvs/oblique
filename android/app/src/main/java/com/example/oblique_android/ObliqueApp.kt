package com.example.oblique_android

import android.app.Application
import android.util.Log
import androidx.work.Configuration
import com.example.oblique_android.services.Prefs
import com.example.oblique_android.utils.AppTime

class ObliqueApp : Application(), Configuration.Provider {

    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        AppTime.init(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setMinimumLoggingLevel(Log.INFO)
            .build()
}
