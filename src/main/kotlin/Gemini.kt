package com.aiyu

import com.google.genai.Client

class GeminiHelper private constructor() {
    private val client: Client = Client()

    fun generateResponse(prompt: String): String? {
        val response = client
            .models
            .generateContent(
                "gemini-2.5-flash",
                prompt,
                null
            )
        return response.text()
    }

    companion object {
        @Volatile
        private var INSTANCE: GeminiHelper? = null

        fun getInstance(): GeminiHelper {
            return INSTANCE ?: synchronized(this) {
                val instance = GeminiHelper()
                INSTANCE = instance
                instance
            }
        }
    }
}