package com.smarttranslator.lectures.summarizer

import kotlinx.coroutines.delay

interface Summarizer {
    val name: String
    suspend fun summarize(text: String): Result<String>
}

class StubSummarizer : Summarizer {
    override val name = "Stub"
    override suspend fun summarize(text: String): Result<String> {
        delay(1500)
        if (text.isBlank()) return Result.success("")
        val snippet = text.split(Regex("\\s+")).take(15).joinToString(" ")
        return Result.success("• $snippet…")
    }
}