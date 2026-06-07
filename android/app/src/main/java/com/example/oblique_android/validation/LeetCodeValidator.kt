package com.example.oblique_android.validation

import android.content.Context
import android.util.Log
import com.example.oblique_android.utils.NetworkUtils
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Calls LeetCode GraphQL API to check solved problem count in a given time window.
 * Implements client-side join between submission list and question difficulty queries.
 * Fully validates inputs, ensures network reliability, and returns structured ValidationResult.
 */
class LeetCodeValidator(private val context: Context) {

    companion object {
        private const val TAG = "LeetCodeValidator"
        private const val BASE_URL = "https://leetcode.com/graphql"
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }

    private val client by lazy {
        OkHttpClient.Builder()
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Validates solved count between [fromMs] and [toMs] timestamps for the given LeetCode username.
     * Performs client-side join: submissions + question difficulties.
     */
    suspend fun validate(username: String?, fromMs: Long, toMs: Long): ValidationResult =
        withContext(Dispatchers.IO) {

            // ---------- 🔍 Input validation ----------
            if (username.isNullOrBlank()) {
                Log.w(TAG, "Validation skipped: username missing for LeetCode platform")
                return@withContext ValidationResult(
                    totalSolved = 0,
                    byDifficulty = emptyMap(),
                    rawEvidence = mapOf("error" to "MISSING_USERNAME" as Any)
                )
            }

            if (fromMs <= 0L || toMs <= 0L || fromMs >= toMs) {
                Log.w(TAG, "Invalid time range: from=$fromMs to=$toMs")
                return@withContext ValidationResult(
                    totalSolved = 0,
                    byDifficulty = emptyMap(),
                    rawEvidence = mapOf("error" to "INVALID_TIME_RANGE" as Any)
                )
            }

            if (!NetworkUtils.hasInternet(context)) {
                Log.w(TAG, "Skipping LeetCode validation: no internet connection")
                return@withContext ValidationResult(
                    totalSolved = 0,
                    byDifficulty = emptyMap(),
                    rawEvidence = mapOf("error" to "NO_INTERNET" as Any)
                )
            }

            // ---------- ⚙️ Step 1: Fetch recent accepted submissions ----------
            val submissionQuery = """
                query recentAcSubmissions(${'$'}username: String!, ${'$'}limit: Int!) {
                  recentAcSubmissionList(username: ${'$'}username, limit: ${'$'}limit) {
                    id
                    titleSlug
                    timestamp
                  }
                }
            """.trimIndent()

            val submissionVars = JSONObject().apply {
                put("username", username)
                put("limit", 50) // limit to avoid excessive load
            }

            val submissions = try {
                fetchSubmissions(submissionQuery, submissionVars)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch submissions: ${e.message}")
                return@withContext ValidationResult(
                    totalSolved = 0,
                    byDifficulty = emptyMap(),
                    rawEvidence = mapOf("error" to "FETCH_SUBMISSION_FAILED" as Any)
                )
            }

            if (submissions.isEmpty()) {
                Log.w(TAG, "No submissions found for user=$username")
                return@withContext ValidationResult(
                    totalSolved = 0,
                    byDifficulty = emptyMap(),
                    rawEvidence = mapOf("error" to "EMPTY_SUBMISSION_LIST" as Any)
                )
            }

            // ---------- ⚙️ Step 2: Fetch difficulty for each unique slug ----------
            val slugSet = submissions.mapNotNull { it.titleSlug }.distinct()
            val slugToDifficulty = mutableMapOf<String, String>()

            coroutineScope {
                slugSet.map { slug ->
                    async {
                        try {
                            val difficulty = fetchDifficulty(slug)
                            if (!difficulty.isNullOrBlank()) {
                                slugToDifficulty[slug] = difficulty
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed to fetch difficulty for $slug: ${e.message}")
                        }
                    }
                }.awaitAll()
            }

            // ---------- ⚙️ Step 3: Combine and compute solved count ----------
            val startSec = fromMs / 1000
            val endSec = toMs / 1000
            var total = 0; var easy = 0; var medium = 0; var hard = 0

            for (sub in submissions) {
                val ts = sub.timestamp
                if (ts in startSec..endSec) {
                    total++
                    val diff = slugToDifficulty[sub.titleSlug]?.lowercase(Locale.ROOT)
                    when (diff) {
                        "easy" -> easy++
                        "medium" -> medium++
                        "hard" -> hard++
                        else -> {}
                    }
                }
            }

            Log.i(TAG, "✅ $username solved $total problems in window (E:$easy M:$medium H:$hard)")

            // ---------- ✅ Return structured result ----------
            return@withContext ValidationResult(
                totalSolved = total,
                byDifficulty = mapOf(
                    "easy" to easy,
                    "medium" to medium,
                    "hard" to hard
                ),
                rawEvidence = mapOf(
                    "username" to username,
                    "from" to fromMs,
                    "to" to toMs,
                    "submissionCount" to submissions.size,
                    "slugsQueried" to slugSet.size,
                    "difficultyFetched" to slugToDifficulty.size
                )
            )
        }

    // =========================== HELPER METHODS ===========================

    private fun fetchSubmissions(query: String, variables: JSONObject): List<Submission> {
        val payload = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }.toString().toRequestBody(JSON)

        val request = Request.Builder()
            .url(BASE_URL)
            .post(payload)
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { res ->
            val bodyStr = res.body?.string() ?: "{}"
            if (!res.isSuccessful) throw IOException("HTTP ${res.code} ${res.message}")

            val data = JSONObject(bodyStr).optJSONObject("data")
            val arr = data?.optJSONArray("recentAcSubmissionList") ?: return emptyList()

            val list = mutableListOf<Submission>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    Submission(
                        titleSlug = obj.optString("titleSlug", ""),
                        timestamp = obj.optLong("timestamp", 0L)
                    )
                )
            }
            return list
        }
    }

    private fun fetchDifficulty(slug: String): String? {
        val query = """
            query questionDifficulty(${'$'}titleSlug: String!) {
              question(titleSlug: ${'$'}titleSlug) {
                difficulty
              }
            }
        """.trimIndent()

        val vars = JSONObject().apply { put("titleSlug", slug) }

        val payload = JSONObject().apply {
            put("query", query)
            put("variables", vars)
        }.toString().toRequestBody(JSON)

        val req = Request.Builder()
            .url(BASE_URL)
            .post(payload)
            .addHeader("Accept", "application/json")
            .build()

        client.newCall(req).execute().use { res ->
            val bodyStr = res.body?.string() ?: "{}"
            if (!res.isSuccessful) return null

            val data = JSONObject(bodyStr).optJSONObject("data")
            val q = data?.optJSONObject("question")
            return q?.optString("difficulty", null)
        }
    }

    // Simple data structure for parsed submissions
    private data class Submission(
        val titleSlug: String,
        val timestamp: Long
    )
}
