package com.megalife.translator.util

/**
 * Decides whether a piece of text is worth sending to the Translator API.
 *
 * OCR on a busy image returns page numbers, prices, dates, rule lines and stray
 * marks alongside real text. Those are billed per character like anything else
 * but translate to themselves, so they're filtered out and passed through
 * unchanged instead.
 */
object TextEligibility {

    /** Single characters carry no context worth translating and are usually OCR noise. */
    private const val MIN_LENGTH = 2

    fun isTranslatable(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.length < MIN_LENGTH) return false
        // Numbers, punctuation and symbols alone ("42", "$19.99", "—", "|||")
        // translate to themselves.
        return trimmed.any { it.isLetter() }
    }
}
