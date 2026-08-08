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

    /** A completion plus the model that actually worked, which may not be the
     * one asked for if auto-recovery kicked in. */
    data class ResolvedCompletion(val result: AiResult, val modelUsed: String)

    /**
     * Runs a completion and, if the model turns out to be unavailable, finds one
     * that works and retries once.
     *
     * Provider catalogues change constantly and model availability varies by
     * account and region, so a hardcoded default will eventually 404 for
     * somebody. Making the user hunt for a valid model id is the wrong answer —
     * the app can just ask the provider and pick one.
     */
    suspend fun completeAutoRecovering(
        provider: AiProvider,
        apiKey: String,
        model: String,
        systemPrompt: String,
        userPrompt: String,
    ): ResolvedCompletion {
        val first = complete(provider, apiKey, model, systemPrompt, userPrompt)
        if (first is AiResult.Success) return ResolvedCompletion(first, model)

        val failure = first as AiResult.Failure
        if (!isModelProblem(failure.message)) return ResolvedCompletion(first, model)

        val listed = listModels(provider, apiKey)
        if (listed !is ModelListResult.Success) return ResolvedCompletion(first, model)

        val replacement = pickBestModel(provider, listed.models, exclude = model)
            ?: return ResolvedCompletion(first, model)

        val second = complete(provider, apiKey, replacement, systemPrompt, userPrompt)
        return if (second is AiResult.Success) {
            ResolvedCompletion(second, replacement)
        } else {
            // Report the original failure — it describes the model the user chose.
            ResolvedCompletion(first, model)
        }
    }

    /**
     * Picks a sensible chat model from whatever the provider offers. Filters out
     * things that can't answer a chat prompt at all (embeddings, image, audio),
     * then prefers small/fast/free variants.
     */
    fun pickBestModel(provider: AiProvider, available: List<String>, exclude: String? = null): String? {
        val unusable = listOf(
            "embed", "embedding", "tts", "whisper", "imagen", "veo", "aqa",
            "guard", "moderation", "rerank", "vision-only", "image-generation",
        )
        val candidates = available
            .filter { it != exclude }
            .filterNot { id -> unusable.any { id.lowercase().contains(it) } }
        if (candidates.isEmpty()) return null

        fun score(id: String): Int {
            val lower = id.lowercase()
            var s = 0
            // Free tiers first — this app is built around costing nothing.
            if (provider == AiProvider.OPENROUTER && lower.endsWith(":free")) s += 100
            if (lower.contains("flash")) s += 40
            if (lower.contains("instant") || lower.contains("mini") || lower.contains("lite")) s += 20
            if (lower.contains("chat") || lower.contains("instruct") || lower.contains("versatile")) s += 15
            if (lower.contains("latest")) s += 10
            // Preview and experimental builds are likelier to disappear.
            if (lower.contains("preview") || lower.contains("exp")) s -= 25
            if (lower.contains("thinking") || lower.contains("reasoner")) s -= 10
            return s
        }
        return candidates.maxByOrNull { score(it) }
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

        // The provider's own message is always appended when present — swallowing
        // it cost several rounds of guessing at what "not found" actually meant.
        val friendly = when (code) {
            400 -> "The provider rejected the request (HTTP 400)."
            401, 403 -> "Key rejected (HTTP $code). Check it's correct and still active."
            402 -> "This model needs credit on your account. Try a free model instead."
            404 -> "That model isn't available to your key (HTTP 404)."
            429 -> "Rate limited. Free tiers cap requests — wait a moment and retry."
            in 500..599 -> "The provider is having problems (HTTP $code). Try again shortly."
            else -> "Request failed (HTTP $code)."
        }
        return if (detail.isNullOrBlank()) friendly else "$friendly\n\n$detail"
    }

    /** True when a failure looks like the model id was the problem, not the key. */
    fun isModelProblem(message: String): Boolean {
        val m = message.lowercase()
        return m.contains("404") ||
            m.contains("not found") ||
            m.contains("does not exist") ||
            m.contains("not supported") ||
            m.contains("no endpoints found")
    }
}
