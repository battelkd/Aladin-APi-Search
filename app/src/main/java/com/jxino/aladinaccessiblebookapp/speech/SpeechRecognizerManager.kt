package com.jxino.aladinaccessiblebookapp.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import java.util.Locale

class SpeechRecognizerManager(
    context: Context,
    private val onResult: (String) -> Unit,
    private val onError: (SpeechRecognitionFailure) -> Unit,
    private val onReady: () -> Unit,
) {
    private val appContext = context.applicationContext
    private var isListening = false
    private var lastPartialResult: String = ""
    private var maxRmsDb = Float.NEGATIVE_INFINITY
    private val tag = "AladinSpeech"

    private val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
        setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(tag, "onReadyForSpeech")
                onReady()
            }

            override fun onBeginningOfSpeech() {
                Log.d(tag, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {
                if (rmsdB > maxRmsDb) {
                    maxRmsDb = rmsdB
                }
            }
            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                Log.d(tag, "onEndOfSpeech")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                lastPartialResult = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                Log.d(tag, "onPartialResults=$lastPartialResult")
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit

            override fun onError(error: Int) {
                isListening = false
                val partial = lastPartialResult.trim()
                val peakRms = maxRmsDb
                lastPartialResult = ""
                maxRmsDb = Float.NEGATIVE_INFINITY
                Log.d(tag, "onError code=$error partial=$partial peakRms=$peakRms")
                if (error == SpeechRecognizer.ERROR_NO_MATCH && partial.isNotBlank()) {
                    onResult(partial)
                } else {
                    onError(speechRecognitionFailureFor(error))
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val text = matches.firstOrNull().orEmpty().ifBlank { lastPartialResult }
                val peakRms = maxRmsDb
                lastPartialResult = ""
                maxRmsDb = Float.NEGATIVE_INFINITY
                Log.d(tag, "onResults matches=$matches text=$text peakRms=$peakRms")
                if (text.isBlank()) {
                    onError(speechRecognitionFailureFor(SpeechRecognizer.ERROR_NO_MATCH))
                } else {
                    onResult(text)
                }
            }
        })
    }

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.KOREA.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, Locale.KOREA.toLanguageTag())
        putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 3000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1000L)
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            onError(
                SpeechRecognitionFailure(
                    code = -1,
                    userMessage = "이 기기에서 사용할 수 있는 Android 음성 인식 서비스가 없습니다. Google 앱 또는 Google 음성 인식 서비스를 설치하거나 실제 Android 기기에서 다시 시도해 주세요.",
                ),
            )
            return
        }
        if (isListening) {
            recognizer.cancel()
        }
        isListening = true
        lastPartialResult = ""
        maxRmsDb = Float.NEGATIVE_INFINITY
        Log.d(tag, "startListening")
        runCatching {
            recognizer.startListening(intent)
        }.onFailure { exception ->
            isListening = false
            Log.e(tag, "startListening failed", exception)
            onError(
                SpeechRecognitionFailure(
                    code = -2,
                    userMessage = "음성 인식을 시작하지 못했습니다. Google 음성 인식 서비스와 마이크 권한을 확인해 주세요.",
                ),
            )
        }
    }

    fun stopListening() {
        if (isListening) {
            Log.d(tag, "stopListening")
            recognizer.stopListening()
        }
    }

    fun destroy() {
        isListening = false
        lastPartialResult = ""
        recognizer.destroy()
    }
}
