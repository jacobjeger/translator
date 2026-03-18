package com.megalife.translator.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("translator_prefs", Context.MODE_PRIVATE)

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
        get() = prefs.getInt(KEY_TTS_SPEED, 1) // 0=slow, 1=normal, 2=fast
        set(value) = prefs.edit().putInt(KEY_TTS_SPEED, value).apply()

    var fontSizeLevel: Int
        get() = prefs.getInt(KEY_FONT_SIZE, 1) // 0=small, 1=medium, 2=large
        set(value) = prefs.edit().putInt(KEY_FONT_SIZE, value).apply()

    var historyEnabled: Boolean
        get() = prefs.getBoolean(KEY_HISTORY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HISTORY_ENABLED, value).apply()

    val fontSizeSp: Float
        get() = when (fontSizeLevel) {
            0 -> 14f
            2 -> 22f
            else -> 18f
        }

    val ttsSpeedFloat: Float
        get() = when (ttsSpeed) {
            0 -> 0.7f
            2 -> 1.5f
            else -> 1.0f
        }

    companion object {
        private const val KEY_DEFAULT_LANG_PAIR = "default_lang_pair"
        private const val KEY_INSTANT_TRANSLATE = "instant_translate"
        private const val KEY_TTS_ENABLED = "tts_enabled"
        private const val KEY_TTS_SPEED = "tts_speed"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_HISTORY_ENABLED = "history_enabled"
    }
}
