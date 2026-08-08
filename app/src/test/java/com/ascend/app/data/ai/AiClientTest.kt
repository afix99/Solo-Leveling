package com.ascend.app.data.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AiClientTest {

    private val client = AiClient()

    // ---- Recognising a model problem vs a key problem --------------------

    @Test
    fun `model problems are recognised across provider phrasings`() {
        listOf(
            "That model isn't available to your key (HTTP 404).",
            "models/gemini-2.5-flash is not found for API version v1beta",
            "The model `foo` does not exist",
            "is not supported for generateContent",
            "No endpoints found for deepseek/deepseek-chat-v3-0324:free.",
        ).forEach {
            assertTrue("should be treated as a model problem: $it", client.isModelProblem(it))
        }
    }

    @Test
    fun `key and quota problems are not mistaken for model problems`() {
        listOf(
            "Key rejected (HTTP 401). Check it's correct and still active.",
            "Rate limited. Free tiers cap requests — wait a moment and retry.",
            "No internet connection.",
            "The provider is having problems (HTTP 503). Try again shortly.",
        ).forEach {
            assertFalse("should not trigger a model swap: $it", client.isModelProblem(it))
        }
    }

    // ---- Picking a replacement model --------------------------------------

    @Test
    fun `returns null when nothing usable is offered`() {
        val onlyEmbeddings = listOf("text-embedding-004", "embedding-001", "imagen-3.0")
        assertNull(client.pickBestModel(AiProvider.GEMINI, onlyEmbeddings))
        assertNull(client.pickBestModel(AiProvider.GEMINI, emptyList()))
    }

    @Test
    fun `never re-picks the model that just failed`() {
        val models = listOf("gemini-2.5-flash")
        assertNull(client.pickBestModel(AiProvider.GEMINI, models, exclude = "gemini-2.5-flash"))
    }

    @Test
    fun `filters out models that cannot answer a chat prompt`() {
        val mixed = listOf(
            "text-embedding-004",
            "imagen-3.0-generate",
            "veo-2.0",
            "gemini-flash-latest",
            "whisper-large-v3",
        )
        val picked = client.pickBestModel(AiProvider.GEMINI, mixed)
        assertEquals("gemini-flash-latest", picked)
    }

    @Test
    fun `prefers free models on OpenRouter`() {
        val models = listOf(
            "anthropic/claude-opus-4",
            "google/gemma-4-26b-a4b-it:free",
            "openai/gpt-5",
        )
        val picked = client.pickBestModel(AiProvider.OPENROUTER, models)
        assertNotNull(picked)
        assertTrue("a paid model was chosen over a free one", picked!!.endsWith(":free"))
    }

    @Test
    fun `prefers flash variants on Gemini`() {
        val models = listOf("gemini-2.5-pro", "gemini-2.5-flash", "gemini-1.0-ultra")
        assertEquals("gemini-2.5-flash", client.pickBestModel(AiProvider.GEMINI, models))
    }

    @Test
    fun `avoids preview and experimental builds when a stable one exists`() {
        val models = listOf("gemini-2.5-flash-preview-09-2025", "gemini-flash-latest")
        assertEquals("gemini-flash-latest", client.pickBestModel(AiProvider.GEMINI, models))
    }

    @Test
    fun `picks a working chat model from a realistic Gemini catalogue`() {
        // Shaped like what the models endpoint actually returns.
        val realistic = listOf(
            "aqa",
            "embedding-001",
            "gemini-2.0-flash",
            "gemini-2.5-flash",
            "gemini-2.5-pro",
            "imagen-3.0-generate-002",
            "text-embedding-004",
        )
        val picked = client.pickBestModel(AiProvider.GEMINI, realistic)
        assertNotNull(picked)
        assertTrue("should pick a gemini chat model, got $picked", picked!!.startsWith("gemini"))
    }

    @Test
    fun `still returns something when only unglamorous options exist`() {
        val models = listOf("some-random-model-v1")
        assertEquals("some-random-model-v1", client.pickBestModel(AiProvider.GROQ, models))
    }

    // ---- Key detection ------------------------------------------------------

    @Test
    fun `detects provider from key prefix including Google's newer format`() {
        assertEquals(AiProvider.GEMINI, AiProvider.detectFromKey("AIzaSyExample"))
        assertEquals(AiProvider.GEMINI, AiProvider.detectFromKey("AQ.Ab8RN6KSpVXaCZKn"))
        assertEquals(AiProvider.OPENROUTER, AiProvider.detectFromKey("sk-or-v1-abc"))
        assertEquals(AiProvider.GROQ, AiProvider.detectFromKey("gsk_abc"))
        assertEquals(AiProvider.DEEPSEEK, AiProvider.detectFromKey("sk-abc123"))
    }

    @Test
    fun `openrouter keys are not mistaken for deepseek despite the shared prefix`() {
        assertEquals(AiProvider.OPENROUTER, AiProvider.detectFromKey("sk-or-v1-something"))
    }

    @Test
    fun `unknown or blank keys detect nothing`() {
        assertNull(AiProvider.detectFromKey(""))
        assertNull(AiProvider.detectFromKey("   "))
        assertNull(AiProvider.detectFromKey("hunter2"))
    }

    @Test
    fun `every provider declares a models endpoint on the same host as its api`() {
        AiProvider.entries.forEach {
            assertTrue("${it.name} models endpoint must be https", it.modelsEndpoint.startsWith("https://"))
            assertTrue("${it.name} endpoint must be https", it.endpoint.startsWith("https://"))
        }
    }
}
