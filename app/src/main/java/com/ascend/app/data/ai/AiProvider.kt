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
) {
    OPENROUTER(
        displayName = "OpenRouter",
        endpoint = "https://openrouter.ai/api/v1/chat/completions",
        defaultModel = "deepseek/deepseek-chat-v3-0324:free",
        signupUrl = "https://openrouter.ai/keys",
        notes = "Free tier, many models behind one key. Models ending in :free cost nothing.",
    ),
    DEEPSEEK(
        displayName = "DeepSeek",
        endpoint = "https://api.deepseek.com/chat/completions",
        defaultModel = "deepseek-chat",
        signupUrl = "https://platform.deepseek.com/api_keys",
        notes = "Very cheap rather than free. Needs credit on the account.",
    ),
    GROQ(
        displayName = "Groq",
        endpoint = "https://api.groq.com/openai/v1/chat/completions",
        defaultModel = "llama-3.3-70b-versatile",
        signupUrl = "https://console.groq.com/keys",
        notes = "Free tier with generous daily limits. Fastest responses.",
    ),
    GEMINI(
        displayName = "Google Gemini",
        endpoint = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions",
        defaultModel = "gemini-2.5-flash",
        signupUrl = "https://aistudio.google.com/apikey",
        notes = "Free tier, no card needed. Key issued instantly from AI Studio.",
    ),
}
