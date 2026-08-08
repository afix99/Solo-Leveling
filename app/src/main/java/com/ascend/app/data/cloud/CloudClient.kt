package com.ascend.app.data.cloud

import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Outcome of a call, kept as a sealed type so the UI can distinguish a
 * misconfigured server from a network blip from an empty backup. */
sealed interface CloudResult<out T> {
    data class Ok<T>(val value: T) : CloudResult<T>
    data class Failure(val message: String, val recoverable: Boolean = true) : CloudResult<Nothing>
}

data class BackupReceipt(val daysWritten: Int, val habitsWritten: Int)

data class RestorePayload(val takenAt: String, val schemaVersion: Int, val payload: JSONObject)

/**
 * Talks to the ASCEND backup service.
 *
 * Same deliberate constraints as the AI client: HttpURLConnection and platform
 * org.json, no Retrofit, no serialization library. The payloads here are a
 * handful of shapes fully under our control, and a networking dependency would
 * be the largest thing in the app for the least benefit.
 */
class CloudClient {

    companion object {
        private const val TIMEOUT_MS = 30_000
        private const val KEY_BYTES = 24

        /**
         * Generates a Hunter Key.
         *
         * SecureRandom rather than Random: this is the only credential
         * protecting the backup, and a predictable one would let someone
         * enumerate their way into another hunter's data.
         */
        fun generateHunterKey(): String {
            val bytes = ByteArray(KEY_BYTES)
            SecureRandom().nextBytes(bytes)
            // Crockford base32. Two properties matter here: it omits I, L, O
            // and U, so no character in the set can be misread as another when
            // the key is written down; and it is exactly 32 symbols, so
            // reducing a byte modulo its length is unbiased. A 31-symbol
            // alphabet would quietly skew the key and cost entropy.
            val alphabet = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"
            return bytes.joinToString("") { b ->
                alphabet[(b.toInt() and 0xFF) % alphabet.length].toString()
            }.chunked(6).joinToString("-")
        }

        /** Trims a pasted URL into a usable origin, tolerating trailing slashes. */
        fun normaliseBaseUrl(raw: String): String {
            var url = raw.trim().removeSuffix("/")
            if (url.isEmpty()) return ""
            if (!url.startsWith("http://") && !url.startsWith("https://")) url = "https://$url"
            return url.removeSuffix("/api").removeSuffix("/")
        }
    }

    /** Confirms the URL points at a real deployment and reports DB status. */
    suspend fun health(baseUrl: String): CloudResult<Boolean> = request(
        baseUrl = baseUrl,
        path = "/api/health",
        method = "GET",
        key = null,
        body = null,
    ) { json -> json.optBoolean("databaseAttached", false) }

    /** Pushes a snapshot plus the flattened day/habit facts. */
    suspend fun backup(
        baseUrl: String,
        hunterKey: String,
        body: JSONObject,
    ): CloudResult<BackupReceipt> = request(
        baseUrl = baseUrl,
        path = "/api/sync",
        method = "POST",
        key = hunterKey,
        body = body.toString(),
    ) { json ->
        BackupReceipt(
            daysWritten = json.optInt("daysWritten"),
            habitsWritten = json.optInt("habitsWritten"),
        )
    }

    /** Fetches the most recent snapshot for restore. */
    suspend fun latestSnapshot(
        baseUrl: String,
        hunterKey: String,
    ): CloudResult<RestorePayload> = request(
        baseUrl = baseUrl,
        path = "/api/sync",
        method = "GET",
        key = hunterKey,
        body = null,
    ) { json ->
        RestorePayload(
            takenAt = json.optString("takenAt"),
            schemaVersion = json.optInt("schemaVersion"),
            payload = json.optJSONObject("payload") ?: JSONObject(),
        )
    }

    /** The server-computed analytics the coach reads. Returned raw so the
     * prompt builder can decide what is worth spending tokens on. */
    suspend fun report(baseUrl: String, hunterKey: String): CloudResult<JSONObject> = request(
        baseUrl = baseUrl,
        path = "/api/report",
        method = "GET",
        key = hunterKey,
        body = null,
    ) { it }

    private suspend fun <T> request(
        baseUrl: String,
        path: String,
        method: String,
        key: String?,
        body: String?,
        parse: (JSONObject) -> T,
    ): CloudResult<T> = withContext(Dispatchers.IO) {
        val origin = normaliseBaseUrl(baseUrl)
        if (origin.isBlank()) {
            return@withContext CloudResult.Failure("No server URL set.", recoverable = false)
        }

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(origin + path).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
                if (key != null) setRequestProperty("Authorization", "Bearer $key")
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
            }

            if (body != null) {
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            }

            val code = connection.responseCode
            val text = (if (code in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()
                ?.use(BufferedReader::readText)
                .orEmpty()

            if (code !in 200..299) {
                val message = runCatching { JSONObject(text).optString("error") }
                    .getOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?: "Server returned HTTP $code."
                // 4xx means the request itself is wrong — retrying unchanged
                // will fail identically, so the UI should say so rather than
                // offering a retry button.
                return@withContext CloudResult.Failure(message, recoverable = code >= 500)
            }

            CloudResult.Ok(parse(JSONObject(text)))
        } catch (e: Exception) {
            CloudResult.Failure(
                e.message?.takeIf { it.isNotBlank() } ?: "Could not reach the server.",
            )
        } finally {
            connection?.disconnect()
        }
    }
}
