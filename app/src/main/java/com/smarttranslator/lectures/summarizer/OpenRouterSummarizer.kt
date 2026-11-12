package com.smarttranslator.lectures.summarizer

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class OpenRouterSummarizer(
    private val apiKey: String,
    private val modelName: String = "openrouter/free",
    private val customInstruction: String
) : Summarizer {

    override val name = "OpenRouter API"

    override suspend fun summarize(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Добавляем таймауты, чтобы запрос не висел вечно
            val client = OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            val prompt = "$customInstruction\n\nТекст:\n$text"

            val jsonBody = JSONObject().apply {
                put("model", modelName)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            val request = Request.Builder()
                .url("https://openrouter.ai/api/v1/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .addHeader("HTTP-Referer", "https://github.com/METEOPuT/LectureAssistant")
                .addHeader("X-Title", "LectureAssistant")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            Log.d("OpenRouter", "Отправка запроса...")
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful) {
                // ВОТ ЗДЕСЬ МЫ ТЕПЕРЬ УВИДИМ ОШИБКУ В ЛОГАХ
                Log.e("OpenRouter", "Ошибка API ${response.code}: $responseBody")
                return@withContext Result.failure(Exception("Ошибка API: ${response.code}"))
            }

            if (responseBody == null) {
                return@withContext Result.failure(Exception("Пустой ответ от сервера"))
            }

            val jsonResponse = JSONObject(responseBody)
            val content = jsonResponse.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")

            Log.d("OpenRouter", "Успешный ответ получен.")
            Result.success(content.trim())
        } catch (e: Exception) {
            Log.e("OpenRouter", "Сетевая ошибка", e)
            Result.failure(e)
        }
    }
}