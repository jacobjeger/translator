package com.megalife.translator.data.repository

import android.content.Context
import com.megalife.translator.BuildConfig
import com.megalife.translator.data.local.PreferencesManager
import com.megalife.translator.data.model.TranslationRequest
import com.megalife.translator.data.remote.RetrofitClient
import com.megalife.translator.filter.ContentFilter
import com.megalife.translator.util.TextEligibility

class TranslationRepository(context: Context) {

    private val api = RetrofitClient.api
    private val contentFilter = ContentFilter()
    private val cache = TranslationCache(context)
    private val prefs = PreferencesManager(context)

    /**
     * The cache stores the user's source text, so it follows the history setting:
     * with history off nothing is read from or written to it.
     */
    private val cachingEnabled: Boolean
        get() = prefs.historyEnabled

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

        val key = text.trim()
        if (cachingEnabled) {
            cache.lookup(listOf(key), from, to)[key]?.let {
                return TranslationResult.Success(it)
            }
        }

        return try {
            val translated = callApi(listOf(key), from, to).firstOrNull() ?: ""
            if (cachingEnabled && translated.isNotBlank()) {
                cache.store(mapOf(key to translated), from, to)
            }
            TranslationResult.Success(translated)
        } catch (e: Exception) {
            TranslationResult.Error(e.message ?: "Translation failed")
        }
    }

    /**
     * Translates a batch of OCR blocks. The returned list is aligned 1:1 with
     * [texts] so callers can index into it positionally when rendering overlays.
     *
     * Only blocks that are actually worth translating, and not already cached,
     * reach the API. Untranslatable blocks (numbers, punctuation, single
     * characters) pass through unchanged.
     */
    suspend fun translateBatch(texts: List<String>, from: String, to: String): TranslationResult {
        if (texts.isEmpty()) return TranslationResult.BatchSuccess(emptyList())

        val combinedText = texts.joinToString(" ")
        if (contentFilter.isBlocked(combinedText)) {
            return TranslationResult.ContentBlocked
        }

        val eligible = texts.map { TextEligibility.isTranslatable(it) }

        // Repeated blocks (headers, running titles) are translated once.
        val distinct = texts
            .filterIndexed { index, _ -> eligible[index] }
            .map { it.trim() }
            .distinct()

        if (distinct.isEmpty()) {
            return TranslationResult.BatchSuccess(texts)
        }

        return try {
            val cached = if (cachingEnabled) cache.lookup(distinct, from, to) else emptyMap()
            val misses = distinct.filterNot { cached.containsKey(it) }

            val fetched = if (misses.isEmpty()) {
                emptyMap()
            } else {
                val results = callApi(misses, from, to)
                misses.zip(results).filter { it.second.isNotBlank() }.toMap()
            }

            if (cachingEnabled && fetched.isNotEmpty()) {
                cache.store(fetched, from, to)
            }

            val resolved = cached + fetched
            val translations = texts.mapIndexed { index, original ->
                if (!eligible[index]) original
                else resolved[original.trim()] ?: original
            }
            TranslationResult.BatchSuccess(translations)
        } catch (e: Exception) {
            TranslationResult.Error(e.message ?: "Translation failed")
        }
    }

    /** Returns translations positionally aligned with [texts]. */
    private suspend fun callApi(texts: List<String>, from: String, to: String): List<String> {
        val response = api.translate(
            from = from,
            to = to,
            body = texts.map { TranslationRequest(it) },
            key = BuildConfig.AZURE_TRANSLATOR_KEY,
            region = BuildConfig.AZURE_TRANSLATOR_REGION
        )
        return response.map { it.translations.firstOrNull()?.text ?: "" }
    }

    suspend fun clearCache() = cache.clear()
}
