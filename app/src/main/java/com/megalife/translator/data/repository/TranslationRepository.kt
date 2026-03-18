package com.megalife.translator.data.repository

import com.megalife.translator.BuildConfig
import com.megalife.translator.data.model.TranslationRequest
import com.megalife.translator.data.model.TranslationResponse
import com.megalife.translator.data.remote.RetrofitClient
import com.megalife.translator.filter.ContentFilter

class TranslationRepository {

    private val api = RetrofitClient.api
    private val contentFilter = ContentFilter()

    sealed class TranslationResult {
        data class Success(val translatedText: String) : TranslationResult()
        data class BatchSuccess(val translations: List<String>) : TranslationResult()
        data class Error(val message: String) : TranslationResult()
        object ContentBlocked : TranslationResult()
    }

    suspend fun translate(text: String, from: String, to: String): TranslationResult {
        if (text.isBlank()) return TranslationResult.Success("")

        if (contentFilter.isBlocked(text)) {
            return TranslationResult.ContentBlocked
        }

        return try {
            val body = listOf(TranslationRequest(text))
            val response = api.translate(
                from = from,
                to = to,
                body = body,
                key = BuildConfig.AZURE_TRANSLATOR_KEY,
                region = BuildConfig.AZURE_TRANSLATOR_REGION
            )
            val translated = response.firstOrNull()?.translations?.firstOrNull()?.text ?: ""
            TranslationResult.Success(translated)
        } catch (e: Exception) {
            TranslationResult.Error(e.message ?: "Translation failed")
        }
    }

    suspend fun translateBatch(texts: List<String>, from: String, to: String): TranslationResult {
        if (texts.isEmpty()) return TranslationResult.BatchSuccess(emptyList())

        val combinedText = texts.joinToString(" ")
        if (contentFilter.isBlocked(combinedText)) {
            return TranslationResult.ContentBlocked
        }

        return try {
            val body = texts.map { TranslationRequest(it) }
            val response = api.translate(
                from = from,
                to = to,
                body = body,
                key = BuildConfig.AZURE_TRANSLATOR_KEY,
                region = BuildConfig.AZURE_TRANSLATOR_REGION
            )
            val translations = response.map { it.translations.firstOrNull()?.text ?: "" }
            TranslationResult.BatchSuccess(translations)
        } catch (e: Exception) {
            TranslationResult.Error(e.message ?: "Translation failed")
        }
    }
}
