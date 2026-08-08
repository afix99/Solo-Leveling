package com.ascend.app.data.ai

import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

sealed interface AiResult {
    data class Success(val text: String) : AiResult
    data class Failure(val message: String) : AiResult
}

sealed interface ModelListResult {
    data class Success(val models: List<String>) : ModelListResult
    data class Failure(val message: String) : ModelListResult
}

/**
 * Minimal OpenAI-compatible chat client.
 *
 * Deliberately built on HttpURLConnection and org.json — both are in the
 * Android platform — so adding an AI feature doesn't drag OkHttp, Retrofit and
 * a serialization library into an app that otherwise makes no network calls.
 */
class AiClient {

    suspend fun complete(
        provider: AiProvider,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
        timeoutMillis: Int = 60_000,
    ): AiResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext AiResult.Failure("No API key set. Add one in Coach → Setup.")
        }

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(provider.endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = timeoutMillis
                readTimeout = timeoutMillis
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Authorization", "Bearer $apiKey")
                if (provider == AiProvider.OPENROUTER) {
                    // OpenRouter attributes traffic by these; harmless elsewhere.
                    setRequestProperty("HTTP-Referer", "https://github.com/afix99/Solo-Leveling")
                    setRequestProperty("X-Title", "ASCEND")
                }
            }

            // Refuse to send anything over a downgraded connection.
            if (connection !is HttpsURLConnection) {
                return@withContext AiResult.Failure("Refusing to send data over a non-HTTPS connection.")
            }

            val body = JSONObject().apply {
                put("model", model.ifBlank { provider.defaultModel })
                put("temperature", 0.7)
                put(
                    "messages",
                    JSONArray().apply {
                        put(JSONObject().put("role", "system").put("content", systemPrompt))
                        put(JSONObject().put("role", "user").put("content", userPrompt))
                    },
                )
            }.toString()

            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()

            if (code !in 200..299) {
                return@withContext AiResult.Failure(describeError(code, response))
            }

            val text = parseContent(response)
                ?: return@withContext AiResult.Failure("The provider returned an empty response.")
            AiResult.Success(text.trim())
        } catch (e: java.net.UnknownHostException) {
            AiResult.Failure("No internet connection.")
        } catch (e: java.net.SocketTimeoutException) {
            AiResult.Failure("The request timed out. Free tiers can be slow — try again.")
        } catch (e: Exception) {
            AiResult.Failure(e.message ?: "Request failed.")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Asks the provider which models this key can actually call.
     *
     * Hardcoded model ids don't survive contact with reality — catalogues turn
     * over every few weeks, and a stale id surfaces as a baffling 404. Fetching
     * the list means the picker always offers something that works.
     */
    suspend fun listModels(
        provider: AiProvider,
        apiKey: String,
        timeoutMillis: Int = 30_000,
    ): ModelListResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext ModelListResult.Failure("Save your API key first.")
        }

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(provider.modelsEndpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = timeoutMillis
                readTimeout = timeoutMillis
                setRequestProperty("Authorization", "Bearer $apiKey")
            }

            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()

            if (code !in 200..299) {
                return@withContext ModelListResult.Failure(describeError(code, response))
            }

            val json = JSONObject(response)
            // OpenAI shape is {"data":[{"id":...}]}; Gemini's native shape is
            // {"models":[{"name":"models/..."}]}. Accept either.
            val array = json.optJSONArray("data")
                ?: json.optJSONArray("models")
                ?: return@withContext ModelListResult.Failure("Unexpected response from provider.")

            val ids = buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val id = item.optString("id").ifBlank { item.optString("name") }
                    if (id.isNotBlank()) add(id.removePrefix("models/"))
                }
            }.distinct().sorted()

            if (ids.isEmpty()) {
                ModelListResult.Failure("The provider returned no models for this key.")
            } else {
                ModelListResult.Success(ids)
            }
        } catch (e: java.net.UnknownHostException) {
            ModelListResult.Failure("No internet connection.")
        } catch (e: Exception) {
            ModelListResult.Failure(e.message ?: "Could not load models.")
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseContent(response: String): String? = runCatching {
        JSONObject(response)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
    }.getOrNull()

    /** Turns provider error bodies into something a user can act on. */
    private fun describeError(code: Int, body: String): String {
        val detail = runCatching {
            val error = JSONObject(body).optJSONObject("error")
            error?.optString("message").takeUnless { it.isNullOrBlank() }
        }.getOrNull()

        return when (code) {
            400 -> detail ?: "The provider rejected the request (HTTP 400)."
            401, 403 -> "Key rejected (HTTP $code). Check it's correct and still active."
            402 -> "This model needs credit on your account. Try a free model instead."
            404 ->
                "That model isn't available to your key. Tap \"Load my models\" in Setup " +
                    "to see exactly which ones you can use."
            429 -> "Rate limited. Free tiers cap requests — wait a moment and retry."
            in 500..599 -> "The provider is having problems (HTTP $code). Try again shortly."
            else -> detail ?: "Request failed (HTTP $code)."
        }
    }
}
