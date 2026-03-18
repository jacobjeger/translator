package com.megalife.translator.data.model

data class LanguagePair(
    val sourceName: String,
    val sourceCode: String,
    val targetName: String,
    val targetCode: String
) {
    val displayName: String
        get() = "$sourceName → $targetName"

    fun swapped(): LanguagePair = LanguagePair(
        sourceName = targetName,
        sourceCode = targetCode,
        targetName = sourceName,
        targetCode = sourceCode
    )

    companion object {
        val ALL_PAIRS = listOf(
            LanguagePair("English", "en", "Hebrew", "he"),
            LanguagePair("Hebrew", "he", "English", "en"),
            LanguagePair("English", "en", "Yiddish", "yi"),
            LanguagePair("Yiddish", "yi", "English", "en"),
            LanguagePair("English", "en", "Spanish", "es"),
            LanguagePair("Spanish", "es", "English", "en"),
            LanguagePair("English", "en", "Russian", "ru"),
            LanguagePair("Russian", "ru", "English", "en"),
            LanguagePair("Hebrew", "he", "Yiddish", "yi"),
            LanguagePair("Yiddish", "yi", "Hebrew", "he"),
            LanguagePair("Hebrew", "he", "Russian", "ru"),
            LanguagePair("Russian", "ru", "Hebrew", "he")
        )

        val RTL_CODES = setOf("he", "yi", "ar")

        fun isRtl(langCode: String): Boolean = langCode in RTL_CODES
    }
}
