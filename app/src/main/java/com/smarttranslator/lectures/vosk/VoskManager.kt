package com.smarttranslator.lectures.vosk

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File

class VoskManager(private val context: Context) {
    private var model: Model? = null
    private var recognizer: Recognizer? = null

    suspend fun load(modelAssetDir: String = "vosk-model-small-ru-0.22"): Result<Unit> = withContext(Dispatchers.IO) {
        if (model != null && recognizer != null) {
            Log.d("VoskDebug", "Модель уже загружена в память, используем её.")
            return@withContext Result.success(Unit)
        }

        try {
            Log.d("VoskDebug", "=== НАЧАЛО ЗАГРУЗКИ МОДЕЛИ ===")

            // ПРОВЕРКА: видим ли мы вообще папку assets
            val rootAssets = context.assets.list("")
            Log.d("VoskDebug", "Корень assets: ${rootAssets?.joinToString(", ") ?: "NULL (ПУСТО)"}")

            val modelAssets = context.assets.list(modelAssetDir)
            Log.d("VoskDebug", "Содержимое assets/$modelAssetDir: ${modelAssets?.joinToString(", ") ?: "NULL (ПУСТО)"}")

            val modelDir = File(context.filesDir, modelAssetDir)
            Log.d("VoskDebug", "Целевая папка во внутренней памяти: ${modelDir.absolutePath}")

            if (!modelDir.exists()) {
                Log.d("VoskDebug", "Модель не найдена, начинаем копирование из assets...")
                copyAssets(modelAssetDir)
                Log.d("VoskDebug", "Копирование завершено.")
            } else {
                Log.d("VoskDebug", "Модель уже есть в filesDir.")
            }

            val copiedFiles = modelDir.list()
            Log.d("VoskDebug", "Скопированные файлы: ${copiedFiles?.joinToString(", ") ?: "NULL"}")

            if (copiedFiles == null || copiedFiles.isEmpty()) {
                throw IllegalStateException("Модель не скопировалась! Папка пуста.")
            }

            Log.d("VoskDebug", "Загружаем модель...")
            model = Model(modelDir.absolutePath)
            recognizer = Recognizer(model!!, 16000f).apply {
                setPartialWords(true) // Включаем промежуточные результаты
            }
            Log.d("VoskDebug", "=== МОДЕЛЬ УСПЕШНО ЗАГРУЖЕНА ===")
            Result.success(Unit)
        } catch (e: Throwable) {
            Log.e("VoskDebug", "=== ОШИБКА ЗАГРУЗКИ МОДЕЛИ ===", e)
            Result.failure(e)
        }
    }

    private fun copyAssets(path: String) {
        val assetManager = context.assets
        val files = assetManager.list(path)

        if (files == null || files.isEmpty()) {
            // Это файл
            val file = File(context.filesDir, path)
            if (!file.exists()) {
                try {
                    file.parentFile?.mkdirs()
                    assetManager.open(path).use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.v("VoskDebug", "Скопирован файл: $path")
                } catch (e: Exception) {
                    Log.w("VoskDebug", "Не удалось открыть как файл: $path")
                }
            }
        } else {
            // Это папка
            for (filename in files) {
                copyAssets("$path/$filename")
            }
        }
    }

    fun accept(buffer: ByteArray, length: Int): Boolean {
        val rec = recognizer ?: return false
        return rec.acceptWaveForm(buffer, length)
    }

    fun getPartialResult(): String? {
        val rec = recognizer ?: return null
        val json = JSONObject(rec.partialResult) // Получаем промежуточный JSON
        return json.optString("partial").ifBlank { null }
    }

    fun getResult(): String? {
        val rec = recognizer ?: return null
        val json = JSONObject(rec.result)
        return json.optString("text").ifBlank { null }
    }

    fun close() {
        runCatching { recognizer?.close() }
        runCatching { model?.close() }
        recognizer = null
        model = null
    }
}