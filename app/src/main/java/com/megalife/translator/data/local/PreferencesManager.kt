package com.megalife.translator.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("megalife_translator_prefs", Context.MODE_PRIVATE)

    var defaultLanguagePairIndex: Int
        get() = prefs.getInt(KEY_DEFAULT_LANG_PAIR, 0)
        set(value) = prefs.edit().putInt(KEY_DEFAULT_LANG_PAIR, value).apply()

    var instantTranslateEnabled: Boolean
        get() = prefs.getBoolean(KEY_INSTANT_TRANSLATE, true)
        set(value) = prefs.edit().putBoolean(KEY_INSTANT_TRANSLATE, value).apply()

    var ttsEnabled: Boolean
        get() = prefs.getBoolean(KEY_TTS_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TTS_ENABLED, value).apply()

    var ttsSpeed: Int
        get() = prefs.getInt(KEY_TTS_SPEED, TTS_SPEED_NORMAL)
        set(value) = prefs.edit().putInt(KEY_TTS_SPEED, value).apply()

    var fontSizeOption: Int
        get() = prefs.getInt(KEY_FONT_SIZE, FONT_SIZE_MEDIUM)
        set(value) = prefs.edit().putInt(KEY_FONT_SIZE, value).apply()

    var historyEnabled: Boolean
        get() = prefs.getBoolean(KEY_HISTORY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HISTORY_ENABLED, value).apply()

    val fontSizeSp: Float
        get() = when (fontSizeOption) {
            FONT_SIZE_SMALL -> 14f
            FONT_SIZE_LARGE -> 20f
            else -> 16f
        }

    val ttsSpeedFloat: Float
        get() = when (ttsSpeed) {
            TTS_SPEED_SLOW -> 0.7f
            TTS_SPEED_FAST -> 1.5f
            else -> 1.0f
        }

    companion object {
        private const val KEY_DEFAULT_LANG_PAIR = "default_lang_pair"
        private const val KEY_INSTANT_TRANSLATE = "instant_translate"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_HISTORY_ENABLED = "history_enabled"

        const val TTS_SPEED_SLOW = 0
        const val TTS_SPEED_NORMAL = 1
        const val TTS_SPEED_FAST = 2

        const val FONT_SIZE_SMALL = 0
        const val FONT_SIZE_MEDIUM = 1
        const val FONT_SIZE_LARGE = 2
    }
}
