package com.megalife.translator.data.model

data class LanguagePair(
    val sourceLanguage: Language,
    val targetLanguage: Language
) {
    val displayName: String
        get() = "${sourceLanguage.displayName} → ${targetLanguage.displayName}"

    val reversedDisplayName: String
        get() = "${targetLanguage.displayName} → ${sourceLanguage.displayName}"

    fun reversed(): LanguagePair = LanguagePair(targetLanguage, sourceLanguage)

    companion object {
        val ALL_PAIRS = listOf(
            LanguagePair(Language.ENGLISH, Language.HEBREW),
            LanguagePair(Language.ENGLISH, Language.YIDDISH),
            LanguagePair(Language.ENGLISH, Language.SPANISH),
            LanguagePair(Language.ENGLISH, Language.RUSSIAN),
            LanguagePair(Language.HEBREW, Language.YIDDISH),
            LanguagePair(Language.HEBREW, Language.RUSSIAN),
            // Reversed pairs
            LanguagePair(Language.HEBREW, Language.ENGLISH),
            LanguagePair(Language.YIDDISH, Language.ENGLISH),
            LanguagePair(Language.SPANISH, Language.ENGLISH),
            LanguagePair(Language.RUSSIAN, Language.ENGLISH),
            LanguagePair(Language.YIDDISH, Language.HEBREW),
            LanguagePair(Language.RUSSIAN, Language.HEBREW)
        )

        val DEFAULT = ALL_PAIRS[0]
    }
}

enum class Language(val code: String, val displayName: String, val isRtl: Boolean) {
    ENGLISH("en", "English", false),
    HEBREW("he", "Hebrew", true),
    YIDDISH("yi", "Yiddish", true),
    SPANISH("es", "Spanish", false),
    RUSSIAN("ru", "Russian", false);

    companion object {
        fun fromCode(code: String): Language? = entries.find { it.code == code }
    }
}
