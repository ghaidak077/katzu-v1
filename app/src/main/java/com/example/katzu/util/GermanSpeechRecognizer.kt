package com.example.katzu.util

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log

/**
 * GermanSpeechRecognizer provides real Android Speech-to-Text configured for German (de-DE).
 * Handles device availability, recognition events, audio amplitude, and error codes gracefully.
 */
class GermanSpeechRecognizer(private val context: Context) {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false

    private fun destroyInternal() {
        try {
            speechRecognizer?.stopListening()
        } catch (_: Throwable) {}
        try {
            speechRecognizer?.cancel()
        } catch (_: Throwable) {}
        try {
            speechRecognizer?.destroy()
        } catch (_: Throwable) {}
        speechRecognizer = null
        isListening = false
    }

    companion object {
        private const val TAG = "GermanSpeechRecognizer"

        fun isAvailable(context: Context): Boolean {
            return SpeechRecognizer.isRecognitionAvailable(context)
        }
    }

    interface Listener {
        fun onReadyForSpeech()
        fun onBeginningOfSpeech()
        fun onRmsChanged(rmsdB: Float)
        fun onPartialResult(partialText: String)
        fun onFinalResult(recognizedText: String)
        fun onError(errorMessageAr: String, errorCode: Int)
        fun onEndOfSpeech()
    }

    fun startListening(listener: Listener) {
        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    AppLogger.w(TAG, "SpeechRecognizer isRecognitionAvailable returned false")
                    listener.onError("التعرف على الصوت غير متوفر على هذا الجهاز. يرجى استخدام لوحة المفاتيح.", -1)
                    return@post
                }

                // Cleanly release prior instance to avoid stale binder states
                destroyInternal()

                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                speechRecognizer = recognizer

                speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        listener.onReadyForSpeech()
                    }

                    override fun onBeginningOfSpeech() {
                        listener.onBeginningOfSpeech()
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        listener.onRmsChanged(rmsdB)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        isListening = false
                        listener.onEndOfSpeech()
                    }

                    override fun onError(error: Int) {
                        isListening = false
                        destroyInternal()
                        val errorName = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO (3)"
                            SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT (5)"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS (9)"
                            SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK (2)"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT (1)"
                            SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH (7)"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY (8)"
                            SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER (4)"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT (6)"
                            else -> "ERROR_CODE ($error)"
                        }
                        val message = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "خطأ في تسجيل الصوت من الميكروفون."
                            SpeechRecognizer.ERROR_CLIENT -> "حدث خطأ في خدمة الصوت. اضغط مجدداً للتحدث."
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "يتطلب التطبيق إذن الوصول إلى الميكروفون."
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "تعذر الاتصال بخدمة التعرف على الصوت. تحقق من الإنترنت وحاول مجدداً."
                            SpeechRecognizer.ERROR_NO_MATCH -> "لم أسمعك بوضوح — جرّب الاقتراب أكثر من المايك والتحدث بنبرة واضحة."
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "خدمة التعرف على الصوت مشغولة حالياً."
                            SpeechRecognizer.ERROR_SERVER -> "خطأ في خادم Google للتعرف على الصوت. حاول مجدداً."
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "لم يُسمع أي صوت — تكلّم بالقرب من المايك بعد الضغط مباشرة."
                            else -> "تعذر التعرف على الصوت ($error)."
                        }
                        AppLogger.e(TAG, "SpeechRecognizer onError exact numeric code: $error [$errorName] -> '$message'")
                        Log.w(TAG, "SpeechRecognizer error: $error [$errorName] -> $message")
                        listener.onError(message, error)
                    }

                    override fun onResults(results: Bundle?) {
                        isListening = false
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                        val recognized = matches?.firstOrNull()?.trim() ?: ""
                        val topScore = scores?.firstOrNull() ?: -1f

                        AppLogger.i(TAG, "SpeechRecognizer onResults: recognized='$recognized', confidence=$topScore")

                        if (recognized.isNotBlank()) {
                            // If confidence scores exist and fall below tolerant threshold (0.4f)
                            if (topScore in 0.0f..0.40f) {
                                AppLogger.w(TAG, "Speech confidence too low ($topScore < 0.40). Prompting retry.")
                                listener.onError("لم أسمعك بوضوح — جرّب الاقتراب أكثر من المايك والتحدث بنبرة واضحة.", SpeechRecognizer.ERROR_NO_MATCH)
                            } else {
                                listener.onFinalResult(recognized)
                            }
                        } else {
                            listener.onError("لم أسمعك بوضوح — جرّب الاقتراب أكثر من المايك والتحدث بنبرة واضحة.", SpeechRecognizer.ERROR_NO_MATCH)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim() ?: ""
                        if (partial.isNotBlank()) {
                            listener.onPartialResult(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "de-DE")
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("de-DE"))
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start speech recognizer", e)
                destroyInternal()
                listener.onError("تعذر تشغيل الميكروفون: ${e.localizedMessage}", -1)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                isListening = false
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping speech recognizer", e)
            }
        }
    }

    fun cancel() {
        mainHandler.post {
            try {
                destroyInternal()
            } catch (e: Exception) {
                Log.w(TAG, "Error cancelling speech recognizer", e)
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                destroyInternal()
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying speech recognizer", e)
            }
        }
    }
}
