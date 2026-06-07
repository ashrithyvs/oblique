package com.example.oblique_android.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

object NetworkUtils {
    fun hasInternet(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        val connected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        Log.v("NetworkUtils", "hasInternet=$connected")
        return connected
    }
}
