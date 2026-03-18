package com.megalife.translator.util

import android.content.Context
import android.speech.tts.TextToSpeech
import com.megalife.translator.data.model.Language
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = TextToSpeech(context, this)
    private var isInitialized = false

    override fun onInit(status: Int) {
        isInitialized = status == TextToSpeech.SUCCESS
    }

    fun speak(text: String, language: Language, speed: Float = 1.0f) {
        if (!isInitialized || text.isBlank()) return

        val locale = when (language) {
            Language.ENGLISH -> Locale.ENGLISH
            Language.HEBREW -> Locale("he", "IL")
            Language.YIDDISH -> Locale("yi")
            Language.SPANISH -> Locale("es", "ES")
            Language.RUSSIAN -> Locale("ru", "RU")
        }

        tts?.language = locale
        tts?.setSpeechRate(speed)
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "translation_utterance")
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
