package com.jxino.aladinaccessiblebookapp.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TtsManager(context: Context) {
    private var ready = false
    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) {
            tts.language = Locale.KOREAN
        }
    }
    }

    fun speak(message: String) {
        if (message.isBlank()) return
        if (ready) {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, "aladin-accessible-book-tts")
        }
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
