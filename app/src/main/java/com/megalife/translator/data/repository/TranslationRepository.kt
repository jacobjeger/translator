package com.megalife.translator.data.repository

import com.megalife.translator.BuildConfig
import com.megalife.translator.data.model.TranslationRequest
import com.megalife.translator.data.model.TranslationResponse
import com.megalife.translator.data.remote.RetrofitClient
import com.megalife.translator.filter.ContentFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class TranslationResult {
    data class Success(val translatedText: String) : TranslationResult()
    data class BatchSuccess(val translations: List<String>) : TranslationResult()
    data class Error(val message: String) : TranslationResult()
    object ContentBlocked : TranslationResult()
}

class TranslationRepository {

    private val api = RetrofitClient.api

    suspend fun translate(
        text: String,
        fromLanguage: String?,
        toLanguage: String
    ): TranslationResult = withContext(Dispatchers.IO) {
        // Content filter check — never send flagged content to API
        if (ContentFilter.isBlocked(text)) {
            return@withContext TranslationResult.ContentBlocked
        }

        try {
            val body = listOf(TranslationRequest(text = text))
            val response = api.translate(
                body = body,
                from = fromLanguage,
                to = toLanguage,
                key = BuildConfig.AZURE_TRANSLATOR_KEY,
                region = BuildConfig.AZURE_TRANSLATOR_REGION
            )
            val translatedText = response.firstOrNull()?.translatedText() ?: ""
            TranslationResult.Success(translatedText)
        } catch (e: Exception) {
            TranslationResult.Error(e.message ?: "Translation failed")
        }
    }

    suspend fun translateBatch(
        texts: List<String>,
        fromLanguage: String?,
        toLanguage: String
    ): TranslationResult = withContext(Dispatchers.IO) {
        // Content filter check on all blocks
        val combinedText = texts.joinToString(" ")
        if (ContentFilter.isBlocked(combinedText)) {
            return@withContext TranslationResult.ContentBlocked
        }

        try {
            val body = texts.map { TranslationRequest(text = it) }
            val response = api.translate(
                body = body,
                from = fromLanguage,
                to = toLanguage,
                key = BuildConfig.AZURE_TRANSLATOR_KEY,
                region = BuildConfig.AZURE_TRANSLATOR_REGION
            )
            val translations = response.map { it.translatedText() }
            TranslationResult.BatchSuccess(translations)
        } catch (e: Exception) {
            TranslationResult.Error(e.message ?: "Batch translation failed")
        }
    }

    suspend fun testConnection(): String = withContext(Dispatchers.IO) {
        try {
            val body = listOf(TranslationRequest(text = "Hello world"))
            val response = api.translate(
                body = body,
                to = "he",
                key = BuildConfig.AZURE_TRANSLATOR_KEY,
                region = BuildConfig.AZURE_TRANSLATOR_REGION
            )
            response.firstOrNull()?.translatedText() ?: "No response"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
