package com.smarttranslator.lectures.summarizer

import ai.nobodywho.Chat
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class QwenSummarizer(
    private val modelPath: String = "hf://Qwen/Qwen2.5-0.5B-Instruct-GGUF/qwen2.5-0.5b-instruct-q4_k_m.gguf",
    private val customInstruction: String = "Составь краткий конспект:",
    private val onProgress: (Float) -> Unit = {}
) : Summarizer {

    override val name = "Qwen2.5-0.5B"

    private var chatInstance: Chat? = null
    private val mutex = Mutex()

    private suspend fun getChat(): Chat = mutex.withLock {
        chatInstance ?: Chat.fromPath(modelPath) { downloaded, total ->
            if (total > 0UL) {
                onProgress((downloaded.toDouble() / total.toDouble()).toFloat())
            }
        }.also { chatInstance = it }
    }

    override suspend fun summarize(text: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val chat = getChat()

            // Используем инструкцию, которую передали из настроек
            val prompt = "<|im_start|>system\n$customInstruction<|im_end|>\n<|im_start|>user\n$text<|im_end|>\n<|im_start|>assistant\n"

            val response = chat.ask(prompt).completed()
            Result.success(response.trim())
        } catch (e: Exception) {
            Log.e("QwenDebug", "Ошибка генерации", e)
            Result.failure(e)
        }
    }
}