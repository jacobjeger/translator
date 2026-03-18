package com.megalife.translator.filter

object ContentFilter {

    // Medical and religious terms that should NEVER be blocked
    private val medicalReligiousWhitelist = setOf(
        // English medical
        "breast", "breasts", "breast cancer", "testicular", "prostate", "cervical",
        "vaginal", "rectal", "anal", "penile", "erectile", "uterine", "ovarian",
        "circumcision", "fertility", "pregnancy", "childbirth", "midwife",
        "gynecologist", "urologist", "mastectomy", "mammogram",
        // Hebrew medical
        "שד", "שדיים", "סרטן השד", "ערמונית", "צוואר הרחם", "רחם", "שחלות",
        "מילה", "ברית מילה", "פוריות", "הריון", "לידה", "מיילדת",
        // Religious terms
        "niddah", "mikvah", "mikveh", "taharat hamishpacha", "tzniut", "modesty",
        "נידה", "מקווה", "טהרת המשפחה", "צניעות",
        "תהילים", "תורה", "תלמוד", "הלכה",
        // Yiddish medical/religious
        "מקוה", "נדה", "צניעות"
    )

    // Profanity blocklist - terms in all supported languages
    private val profanityList = setOf(
        // English
        "fuck", "shit", "damn", "bitch", "asshole", "bastard", "crap",
        "dick", "piss", "slut", "whore", "cock", "cunt",
        "motherfucker", "bullshit", "goddamn",
        // Hebrew
        "זונה", "כוס", "זין", "חרא", "מניאק", "בן זונה", "שרמוטה",
        // Spanish
        "mierda", "puta", "coño", "joder", "cabrón", "pendejo", "chingar",
        "verga", "culo", "maricón",
        // Russian
        "блядь", "сука", "хуй", "пизда", "ебать", "мудак",
        "дерьмо", "жопа",
        // Yiddish
        "שמאק", "דרעק", "חזיר"
    )

    // Explicit sexual content blocklist
    private val explicitContentList = setOf(
        // English
        "porn", "pornography", "xxx", "orgasm", "masturbat",
        "erotic", "erotica", "nude", "nudity", "naked",
        "sexual intercourse", "oral sex", "anal sex",
        "strip club", "stripper", "escort service",
        "hentai", "fetish", "bondage", "bdsm",
        // Hebrew
        "פורנו", "פורנוגרפיה", "סקס", "אורגזמה", "אוננות",
        "ארוטי", "עירום", "יחסי מין",
        // Spanish
        "porno", "pornografía", "orgasmo", "masturbación",
        "erótico", "desnudo", "relaciones sexuales",
        // Russian
        "порно", "порнография", "оргазм", "мастурбация",
        "эротика", "обнаженный",
        // Yiddish
        "פארנא", "נאקעט"
    )

    fun isBlocked(text: String): Boolean {
        val lowerText = text.lowercase().trim()

        // Check if the text is purely medical/religious — if so, never block
        if (isWhitelisted(lowerText)) return false

        // Check profanity
        for (term in profanityList) {
            if (lowerText.contains(term.lowercase())) {
                // Verify it's not part of a whitelisted phrase
                if (!isPartOfWhitelistedPhrase(lowerText, term.lowercase())) {
                    return true
                }
            }
        }

        // Check explicit content
        for (term in explicitContentList) {
            if (lowerText.contains(term.lowercase())) {
                if (!isPartOfWhitelistedPhrase(lowerText, term.lowercase())) {
                    return true
                }
            }
        }

        return false
    }

    private fun isWhitelisted(text: String): Boolean {
        return medicalReligiousWhitelist.any { whiteTerm ->
            text.contains(whiteTerm.lowercase())
        }
    }

    private fun isPartOfWhitelistedPhrase(fullText: String, blockedTerm: String): Boolean {
        // Check if the blocked term appears only as part of a whitelisted phrase
        for (whiteTerm in medicalReligiousWhitelist) {
            if (whiteTerm.lowercase().contains(blockedTerm) && fullText.contains(whiteTerm.lowercase())) {
                return true
            }
        }
        return false
    }
}
