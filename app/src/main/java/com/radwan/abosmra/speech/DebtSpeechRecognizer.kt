package com.radwan.abosmra.speech

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

enum class DebtSpeechError {
    NO_MATCH,
    TIMEOUT,
    BUSY,
    PERMISSION,
    UNAVAILABLE,
    OTHER
}

class DebtSpeechRecognizer(context: Context) {
    private val appContext = context.applicationContext
    private var recognizer: SpeechRecognizer? = null
    private var active = false

    data class Callbacks(
        val onReady: () -> Unit = {},
        val onSpeechStarted: () -> Unit = {},
        val onPartial: (String) -> Unit = {},
        val onFinal: (List<String>) -> Unit = {},
        val onEndOfSpeech: () -> Unit = {},
        val onError: (DebtSpeechError) -> Unit = {}
    )

    fun isRecognitionAvailable(): Boolean =
        SpeechRecognizer.isRecognitionAvailable(appContext)

    fun start(callbacks: Callbacks): Boolean {
        if (active) return false
        if (!isRecognitionAvailable()) {
            callbacks.onError(DebtSpeechError.UNAVAILABLE)
            return false
        }

        destroyRecognizerOnly()
        val speechRecognizer = runCatching {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
            ) {
                SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
            } else {
                SpeechRecognizer.createSpeechRecognizer(appContext)
            }
        }.getOrElse {
            callbacks.onError(DebtSpeechError.UNAVAILABLE)
            return false
        }

        recognizer = speechRecognizer
        active = true
        speechRecognizer.setRecognitionListener(listener(callbacks))

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-IQ")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
        }

        return runCatching {
            speechRecognizer.startListening(intent)
            true
        }.getOrElse {
            active = false
            callbacks.onError(DebtSpeechError.UNAVAILABLE)
            destroyRecognizerOnly()
            false
        }
    }

    fun stopListening() {
        if (active) runCatching { recognizer?.stopListening() }
    }

    fun cancel() {
        active = false
        runCatching { recognizer?.cancel() }
    }

    fun destroy() {
        active = false
        destroyRecognizerOnly()
    }

    private fun destroyRecognizerOnly() {
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun listener(callbacks: Callbacks): RecognitionListener =
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = callbacks.onReady()
            override fun onBeginningOfSpeech() = callbacks.onSpeechStarted()
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = callbacks.onEndOfSpeech()

            override fun onError(error: Int) {
                if (!active) return
                active = false
                callbacks.onError(mapError(error))
            }

            override fun onResults(results: Bundle?) {
                if (!active) return
                active = false
                val alternatives = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    .orEmpty()
                    .filter { it.isNotBlank() }
                if (alternatives.isEmpty()) {
                    callbacks.onError(DebtSpeechError.NO_MATCH)
                } else {
                    callbacks.onFinal(alternatives)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (!active) return
                partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.takeIf { it.isNotBlank() }
                    ?.let(callbacks.onPartial)
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }

    private fun mapError(error: Int): DebtSpeechError =
        when (error) {
            SpeechRecognizer.ERROR_NO_MATCH -> DebtSpeechError.NO_MATCH
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> DebtSpeechError.TIMEOUT
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> DebtSpeechError.BUSY
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> DebtSpeechError.PERMISSION
            else -> DebtSpeechError.OTHER
        }
}
