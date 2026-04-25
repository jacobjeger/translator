package com.megalife.translator.filter

class ContentFilter {

    /**
     * Medical and religious terms that must never be blocked regardless of context.
     * These override any blocklist matches.
     */
    private val whitelistedTerms = setOf(
        // Medical English
        "breast cancer", "breast exam", "breast milk", "breastfeeding",
        "testicular", "prostate", "cervical", "vaginal infection",
        "sexual health", "sexually transmitted", "reproductive",
        "circumcision", "fertility", "infertility", "menstruation",
        "pregnancy", "prenatal", "gynecology", "obstetrics", "urology",
        "anatomy", "puberty", "hormone", "estrogen", "testosterone",
        // Medical Hebrew
        "סרטן השד", "בדיקת שד", "הנקה", "פוריות", "עקרות",
        "מחזור", "הריון", "גינקולוגיה", "אורולוגיה", "מילה",
        // Religious English
        "virgin mary", "immaculate conception", "holy seed",
        "circumcision", "brit milah", "niddah", "mikveh", "mikvah",
        "taharat hamishpacha", "family purity",
        // Religious Hebrew
        "ברית מילה", "נידה", "מקווה", "טהרת המשפחה",
        "קדושה", "ערווה", "צניעות",
        // Religious Yiddish
        "ברית", "מקוה", "נידה", "טהרה"
    )

    /**
     * Profanity blocklist covering English, Hebrew, Yiddish, Spanish, Russian.
     * These are common profane words that should not be translated.
     */
    private val profanityBlocklist = setOf(
        // English
        "fuck", "shit", "damn", "bitch", "asshole", "bastard",
        "crap", "dick", "piss", "cunt", "motherfucker", "bullshit",
        "goddam", "goddamn",
        // Spanish
        "mierda", "puta", "puto", "joder", "jodido", "coño", "cabrón",
        "pendejo", "chingar", "chingada", "pinche", "verga", "culero",
        "maricón", "carajo", "cojones", "gilipollas", "hijo de puta",
        "hijoputa", "huevón", "güevón", "boludo", "pelotudo", "mamón",
        "mierdoso", "cagar", "culiao", "culiado",
        // Russian (transliterated and cyrillic)
        "блядь", "сука", "хуй", "пизда", "ебать", "дерьмо",
        "blyad", "suka", "khuy", "pizda", "yebat",
        // Hebrew
        "זונה", "חרא", "מניאק", "בן זונה",
        // Yiddish
        "חזיר", "שמאק", "דרעק"
    )

    /**
     * Explicit sexual content blocklist covering all supported languages.
     */
    private val explicitBlocklist = setOf(
        // English
        "porn", "pornography", "xxx", "hentai", "nude", "nudes",
        "naked", "sex video", "sex tape", "erotic", "orgasm",
        "masturbat", "dildo", "vibrator", "blowjob", "handjob",
        "threesome", "orgy", "fetish", "bondage", "stripper",
        "escort service", "prostitut", "hooker", "brothel",
        // Spanish
        "pornografía", "porno", "desnudo", "sexo oral",
        "prostitución", "burdel",
        // Russian
        "порно", "порнография", "проститу", "эротик",
        "оргазм", "стриптиз", "бордель",
        // Hebrew
        "פורנו", "פורנוגרפיה", "זנות", "עירום",
        // Yiddish
        "פּאָרנאָ"
    )

    /**
     * Check if text contains blocked content.
     * Returns true if content should be blocked.
     * Medical and religious terms are never blocked.
     */
    fun isBlocked(text: String): Boolean {
        val lowerText = text.lowercase().trim()

        for (term in whitelistedTerms) {
            if (lowerText.contains(term.lowercase())) {
                return false
            }
        }

        for (term in profanityBlocklist) {
            if (lowerText.contains(term.lowercase())) {
                return true
            }
        }

        for (term in explicitBlocklist) {
            if (lowerText.contains(term.lowercase())) {
                return true
            }
        }

        return false
    }
}
