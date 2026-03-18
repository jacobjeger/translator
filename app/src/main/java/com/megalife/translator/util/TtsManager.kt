package com.megalife.translator.util

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsManager(context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech = TextToSpeech(context, this)
    private var isReady = false

    override fun onInit(status: Int) {
        isReady = status == TextToSpeech.SUCCESS
    }

    fun speak(text: String, languageCode: String, speed: Float = 1.0f) {
        if (!isReady || text.isBlank()) return

        val locale = when (languageCode) {
            "he" -> Locale("he", "IL")
            "yi" -> Locale("yi")
            "es" -> Locale("es", "ES")
            "ru" -> Locale("ru", "RU")
            else -> Locale.ENGLISH
        }

        tts.language = locale
        tts.setSpeechRate(speed)
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "translation_tts")
    }

    fun stop() {
        if (isReady) tts.stop()
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
