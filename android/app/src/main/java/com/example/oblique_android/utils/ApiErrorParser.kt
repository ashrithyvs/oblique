package com.example.oblique_android.utils

import org.json.JSONObject
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

object ApiErrorParser {

    fun messageFrom(ex: Exception, fallback: String): String {
        if (ex is HttpException) {
            parseMessage(ex)?.let { return it }
        }
        return when (ex) {
            is UnknownHostException, is SocketTimeoutException, is IOException ->
                "Network error. Check your connection and try again."
            else -> fallback
        }
    }

    private fun parseMessage(ex: HttpException): String? {
        val body = ex.response()?.errorBody()?.string().orEmpty()
        if (body.isBlank()) return null

        return try {
            val message = JSONObject(body).optString("message", "").trim()
            message.takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }
}
