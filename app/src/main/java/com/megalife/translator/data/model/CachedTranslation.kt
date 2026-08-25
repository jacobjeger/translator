package com.megalife.translator.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A previously billed translation, kept so the same source text is never sent
 * to the Translator API twice. Keyed by a hash of (sourceLanguage, targetLanguage,
 * sourceText) so long OCR blocks don't bloat the index.
 *
 * This is deliberately separate from [TranslationHistory]: history is capped at 20
 * user-facing entries, while the cache is a larger billing-side store. Clearing
 * history still clears this too, since it holds the same source text.
 */
@Entity(tableName = "translation_cache")
data class CachedTranslation(
    @PrimaryKey
    val cacheKey: String,
    val translatedText: String,
    val createdAt: Long = System.currentTimeMillis()
)
