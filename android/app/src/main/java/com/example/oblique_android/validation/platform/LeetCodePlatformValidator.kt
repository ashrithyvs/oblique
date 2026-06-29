package com.example.oblique_android.validation.platform

import android.content.Context
import android.util.Log
import com.example.oblique_android.utils.NetworkUtils
import com.example.oblique_android.utils.PlatformConstants
import com.example.oblique_android.utils.ValidationConstants
import com.example.oblique_android.utils.ValidationDevLogger
import com.example.oblique_android.validation.ValidationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class LeetCodePlatformValidator(
    private val context: Context? = null,
    private val client: OkHttpClient = defaultClient(),
) : PlatformGoalValidator {

    override val platformKey: String = PlatformConstants.KEY_LEETCODE

    override suspend fun validate(context: PlatformValidationContext): PlatformValidationResult {
        val username = context.username
        if (username.isNullOrBlank()) {
            return PlatformValidationResult.Failure(
                ValidationConstants.ERROR_MISSING_USERNAME,
                "LeetCode username not configured",
            )
        }

        if (context.windowStartMs <= 0L || context.windowEndMs <= 0L ||
            context.windowStartMs >= context.windowEndMs
        ) {
            return PlatformValidationResult.Failure(
                ValidationConstants.ERROR_INVALID_TIME_RANGE,
                "Invalid validation window",
            )
        }

        if (this.context != null && !NetworkUtils.hasInternet(this.context)) {
            return PlatformValidationResult.Failure(
                ValidationConstants.ERROR_NO_INTERNET,
                "No internet connection",
            )
        }

        val result = fetchSolvedInWindow(username, context.windowStartMs, context.windowEndMs)
            ?: return PlatformValidationResult.Failure(
                ValidationConstants.ERROR_FETCH_SUBMISSION_FAILED,
                "Failed to fetch LeetCode submissions",
            )

        val error = result.rawEvidence?.get("error") as? String
        if (error != null) {
            return PlatformValidationResult.Failure(error)
        }

        val currentValue = result.totalSolved
        val computedProgress = context.goal.computedProgress(currentValue)
        return if (computedProgress == context.goal.progress) {
            PlatformValidationResult.NoChange(currentValue, result.rawEvidence)
        } else {
            PlatformValidationResult.Success(currentValue, result.rawEvidence)
        }
    }

    override suspend fun verifyUser(username: String): PlatformUserVerification = withContext(Dispatchers.IO) {
        if (username.isBlank()) {
            return@withContext PlatformUserVerification.Invalid("Username is required")
        }
        if (context != null && !NetworkUtils.hasInternet(context)) {
            return@withContext PlatformUserVerification.Error(
                ValidationConstants.ERROR_NO_INTERNET,
                "No internet connection",
            )
        }
        try {
            if (fetchMatchedUsername(username) != null) {
                PlatformUserVerification.Valid
            } else {
                PlatformUserVerification.Invalid("LeetCode user not found")
            }
        } catch (e: Exception) {
            Log.w(TAG, "verifyUser failed for ${redactUsername(username)}: ${e.message}")
            PlatformUserVerification.Error(
                ValidationConstants.ERROR_VERIFY_USER_FAILED,
                e.message,
            )
        }
    }

    override     suspend fun fetchBaselineCount(
        username: String,
        windowStartMs: Long,
        windowEndMs: Long,
    ): Int? = fetchSolvedInWindow(username, windowStartMs, windowEndMs)?.totalSolved

    suspend fun fetchTimestampedSubmissions(username: String): List<com.example.oblique_android.validation.TimestampedSubmission>? =
        withContext(Dispatchers.IO) {
            if (username.isBlank()) return@withContext null
            if (context != null && !NetworkUtils.hasInternet(context)) return@withContext null
            try {
                fetchRecentSubmissions(username).map {
                    com.example.oblique_android.validation.TimestampedSubmission(it.timestamp, it.titleSlug)
                }
            } catch (e: Exception) {
                Log.w(TAG, "fetchTimestampedSubmissions failed: ${e.message}")
                null
            }
        }

    private fun fetchMatchedUsername(username: String): String? {
        val query = """
            query matchedUser(${'$'}username: String!) {
              matchedUser(username: ${'$'}username) {
                username
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply { put("username", username) }
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
            if (!res.isSuccessful) throw IOException("HTTP ${res.code}")
            val data = JSONObject(bodyStr).optJSONObject("data")
            return data?.optJSONObject("matchedUser")?.optString("username")?.takeIf { it.isNotBlank() }
        }
    }

    private suspend fun fetchSolvedInWindow(
        username: String,
        fromMs: Long,
        toMs: Long,
    ): ValidationResult? = withContext(Dispatchers.IO) {
        val redacted = redactUsername(username)
        var lastError: String? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                val submissions = fetchRecentSubmissions(username)
                if (submissions.isEmpty()) {
                    Log.w(TAG, "No submissions for user=$redacted")
                    return@withContext ValidationResult(
                        totalSolved = 0,
                        byDifficulty = emptyMap(),
                        rawEvidence = mapOf(
                            "platform" to platformKey,
                            "from" to fromMs,
                            "to" to toMs,
                            "submissionCount" to 0,
                        ),
                    )
                }

                val slugSet = submissions.map { it.titleSlug }.distinct()
                val slugToDifficulty = fetchDifficulties(slugSet)

                val startSec = fromMs / 1000
                val endSec = toMs / 1000
                var total = 0
                var easy = 0
                var medium = 0
                var hard = 0

                for (sub in submissions) {
                    if (sub.timestamp in startSec..endSec) {
                        total++
                        when (slugToDifficulty[sub.titleSlug]?.lowercase(Locale.ROOT)) {
                            "easy" -> easy++
                            "medium" -> medium++
                            "hard" -> hard++
                        }
                    }
                }

                Log.i(TAG, "user=$redacted solved $total in window (E:$easy M:$medium H:$hard)")

                return@withContext ValidationResult(
                    totalSolved = total,
                    byDifficulty = mapOf("easy" to easy, "medium" to medium, "hard" to hard),
                    rawEvidence = mapOf(
                        "platform" to platformKey,
                        "from" to fromMs,
                        "to" to toMs,
                        "submissionCount" to submissions.size,
                        "slugsQueried" to slugSet.size,
                        "difficultyFetched" to slugToDifficulty.size,
                    ),
                ).also { vr ->
                    ValidationDevLogger.logExternalApi(
                        method = "POST",
                        url = BASE_URL,
                        payload = """{"query":"recentAcSubmissionList","variables":{"username":"${redactUsername(username)}","limit":${PAGE_SIZE * MAX_SUBMISSION_PAGES}}}""",
                        responseSummary = "solvedInWindow=$total submissionsFetched=${submissions.size} " +
                            "(E:$easy M:$medium H:$hard) evidence=${vr.rawEvidence}",
                    )
                }
            } catch (e: Exception) {
                lastError = e.message
                Log.w(TAG, "Attempt ${attempt + 1}/$MAX_RETRIES failed for user=$redacted: ${e.message}")
                if (attempt < MAX_RETRIES - 1) {
                    Thread.sleep(RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }

        Log.e(TAG, "All retries failed for user=$redacted: $lastError")
        null
    }

    /**
     * Fetches recent AC submissions in one request. LeetCode's `recentAcSubmissionList`
     * accepts only `username` and `limit` — there is no offset/cursor for paging.
     */
    private fun fetchRecentSubmissions(username: String): List<Submission> {
        val maxSubmissions = PAGE_SIZE * MAX_SUBMISSION_PAGES
        return fetchSubmissionPage(username, maxSubmissions)
    }

    private fun fetchSubmissionPage(username: String, limit: Int): List<Submission> {
        val query = """
            query recentAcSubmissions(${'$'}username: String!, ${'$'}limit: Int!) {
              recentAcSubmissionList(username: ${'$'}username, limit: ${'$'}limit) {
                id
                titleSlug
                timestamp
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply {
            put("username", username)
            put("limit", limit)
        }

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
            if (!res.isSuccessful) throw IOException("HTTP ${res.code}")

            val data = JSONObject(bodyStr).optJSONObject("data")
            val arr = data?.optJSONArray("recentAcSubmissionList") ?: return emptyList()

            return buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(
                        Submission(
                            titleSlug = obj.optString("titleSlug", ""),
                            timestamp = obj.optLong("timestamp", 0L),
                        ),
                    )
                }
            }
        }
    }

    private suspend fun fetchDifficulties(slugs: List<String>): Map<String, String> {
        val slugToDifficulty = mutableMapOf<String, String>()
        coroutineScope {
            slugs.map { slug ->
                async {
                    try {
                        fetchDifficulty(slug)
                            ?.takeIf { it.isNotBlank() }
                            ?.let { slugToDifficulty[slug] = it }
                    } catch (e: Exception) {
                        Log.w(TAG, "Difficulty fetch failed for slug=$slug: ${e.message}")
                    }
                }
            }.awaitAll()
        }
        return slugToDifficulty
    }

    private fun fetchDifficulty(slug: String): String? {
        val query = """
            query questionDifficulty(${'$'}titleSlug: String!) {
              question(titleSlug: ${'$'}titleSlug) {
                difficulty
              }
            }
        """.trimIndent()

        val variables = JSONObject().apply { put("titleSlug", slug) }
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
            if (!res.isSuccessful) return null
            val bodyStr = res.body?.string() ?: return null
            val data = JSONObject(bodyStr).optJSONObject("data")
            return data?.optJSONObject("question")?.optString("difficulty")?.takeIf { it.isNotEmpty() }
        }
    }

    private data class Submission(val titleSlug: String, val timestamp: Long)

    companion object {
        private const val TAG = "LeetCodePlatformValidator"
        private const val BASE_URL = "https://leetcode.com/graphql"
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val PAGE_SIZE = 50
        private const val MAX_SUBMISSION_PAGES = 3
        private const val MAX_RETRIES = 2
        private const val RETRY_DELAY_MS = 500L

        fun redactUsername(username: String): String {
            if (username.length <= 2) return "***"
            return username.first() + "***" + username.last()
        }

        private fun defaultClient(): OkHttpClient =
            OkHttpClient.Builder()
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .connectTimeout(10, TimeUnit.SECONDS)
                .build()
    }
}
