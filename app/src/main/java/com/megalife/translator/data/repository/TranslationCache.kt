package com.megalife.translator.data.repository

import android.content.Context
import com.megalife.translator.data.local.AppDatabase
import com.megalife.translator.data.model.CachedTranslation
import java.security.MessageDigest

/**
 * Persistent cache of already-translated text, so re-scanning the same page or
 * re-typing the same phrase doesn't get billed again.
 */
class TranslationCache(context: Context) {

    private val dao = AppDatabase.getInstance(context).translationCacheDao()

    /** Returns sourceText -> translatedText for whichever of [texts] are cached. */
    suspend fun lookup(texts: List<String>, from: String, to: String): Map<String, String> {
        if (texts.isEmpty()) return emptyMap()
        val keyToText = texts.associateBy({ keyFor(it, from, to) }, { it })
        val hits = dao.getByKeys(keyToText.keys.toList())
        return hits.mapNotNull { hit ->
            keyToText[hit.cacheKey]?.let { it to hit.translatedText }
        }.toMap()
    }

    /** Stores [translations] (sourceText -> translatedText), then trims the cache. */
    suspend fun store(translations: Map<String, String>, from: String, to: String) {
        if (translations.isEmpty()) return
        val entries = translations.map { (source, translated) ->
            CachedTranslation(
                cacheKey = keyFor(source, from, to),
                translatedText = translated
            )
        }
        dao.insertAll(entries)

        dao.deleteExpired(System.currentTimeMillis() - TTL_MILLIS)
        val count = dao.getCount()
        if (count > MAX_ENTRIES) {
            dao.deleteOldest(count - MAX_ENTRIES)
        }
    }

    suspend fun clear() = dao.deleteAll()

    private fun keyFor(text: String, from: String, to: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$from|$to|$text".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val MAX_ENTRIES = 500
        private const val TTL_MILLIS = 30L * 24 * 60 * 60 * 1000 // 30 days
    }
}
