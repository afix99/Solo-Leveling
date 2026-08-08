package com.ascend.app.data.ai

/**
 * Every provider here speaks the OpenAI chat-completions shape, which is why
 * one client covers all of them. Gemini is included via Google's
 * OpenAI-compatible endpoint rather than its native API for the same reason.
 *
 * No key is bundled with the app — the user supplies their own, and it never
 * leaves the device except as an Authorization header to the provider they
 * chose.
 */
enum class AiProvider(
    val displayName: String,
    val endpoint: String,
    val defaultModel: String,
    val signupUrl: String,
    val notes: String,
    /** How this provider's keys begin, used to catch a key pasted under the
     * wrong provider before it fails with a confusing 401. */
    val keyPrefixes: List<String>,
    /** Offered as chips so a model ID never has to be typed by hand. These are
     * only a starting suggestion — catalogues change constantly, so the real
     * list is fetched from the provider with the user's key. */
    val commonModels: List<String>,
    /** OpenAI-shaped models endpoint, used to replace the suggestions above
     * with what this specific key can actually call. */
    val modelsEndpoint: String,
) {
    OPENROUTER(
        displayName = "OpenRouter",
        endpoint = "https://openrouter.ai/api/v1/chat/completions",
        defaultModel = "deepseek/deepseek-chat-v3-0324:free",
        signupUrl = "https://openrouter.ai/keys",
        notes = "Free tier, many models behind one key. Models ending in :free cost nothing.",
        keyPrefixes = listOf("sk-or-"),
        commonModels = listOf(
            "deepseek/deepseek-chat-v3-0324:free",
            "meta-llama/llama-3.3-70b-instruct:free",
            "google/gemma-3-27b-it:free",
            "qwen/qwen3-235b-a22b:free",
        ),
        modelsEndpoint = "https://openrouter.ai/api/v1/models",
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        endpoint = "https://api.deepseek.com/chat/completions",
        defaultModel = "deepseek-chat",
        signupUrl = "https://platform.deepseek.com/api_keys",
        notes = "Very cheap rather than free. Needs credit on the account.",
        keyPrefixes = listOf("sk-"),
        commonModels = listOf("deepseek-chat", "deepseek-reasoner"),
        modelsEndpoint = "https://api.deepseek.com/models",
    ),
    GROQ(
        displayName = "Groq",
        endpoint = "https://api.groq.com/openai/v1/chat/completions",
        defaultModel = "llama-3.3-70b-versatile",
        signupUrl = "https://console.groq.com/keys",
        notes = "Free tier with generous daily limits. Fastest responses.",
        keyPrefixes = listOf("gsk_"),
        commonModels = listOf(
            "llama-3.3-70b-versatile",
            "llama-3.1-8b-instant",
            "openai/gpt-oss-120b",
        ),
        modelsEndpoint = "https://api.groq.com/openai/v1/models",
    ),
    GEMINI(
        displayName = "Google Gemini",
        endpoint = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
        defaultModel = "gemini-2.5-flash",
        signupUrl = "https://aistudio.google.com/apikey",
        notes = "Free tier, no card needed. Key issued instantly from AI Studio.",
        keyPrefixes = listOf("AIza", "AQ."),
        commonModels = listOf("gemini-2.5-flash", "gemini-2.5-pro", "gemini-2.0-flash"),
        modelsEndpoint = "https://generativelanguage.googleapis.com/v1beta/openai/models",
    ),
    ;

    fun looksLikeMyKey(key: String): Boolean =
        keyPrefixes.any { key.trim().startsWith(it) }

    companion object {
        /**
         * Best guess at which provider a key belongs to. DeepSeek's plain "sk-"
         * is checked last because OpenRouter's "sk-or-" also starts with it.
         */
        fun detectFromKey(key: String): AiProvider? {
            val trimmed = key.trim()
            if (trimmed.isBlank()) return null
            return entries.firstOrNull { it != DEEPSEEK && it.looksLikeMyKey(trimmed) }
                ?: DEEPSEEK.takeIf { it.looksLikeMyKey(trimmed) }
        }
    }
}
