package com.example.katzu.util

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

/**
 * Katzu Multi-Tiered Haptic Symphony System.
 * Provides subtle, mechanical, spring-like tactile feedback across interactions.
 */
object KatzuHaptics {

    private fun getVibrator(context: Context): Vibrator? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun vibrateWithAttributes(v: Vibrator, effect: VibrationEffect) {
        try {
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_TOUCH)
                v.vibrate(effect, attributes)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()
                v.vibrate(effect, audioAttributes)
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(effect)
            }
        } catch (_: Throwable) {
            try {
                @Suppress("DEPRECATION")
                v.vibrate(effect)
            } catch (_: Throwable) {}
        }
    }

    /**
     * Light mechanical tick for navigation tabs, back button, chips, and audio play icons.
     */
    fun tick(haptic: HapticFeedback? = null, context: Context? = null) {
        try {
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Throwable) {}

        if (context != null) {
            try {
                val v = getVibrator(context) ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrateWithAttributes(v, VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrateWithAttributes(v, VibrationEffect.createOneShot(16, 170))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(16)
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Tactile bounce on CTA button or card press.
     */
    fun press(haptic: HapticFeedback? = null, context: Context? = null) {
        try {
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Throwable) {}

        if (context != null) {
            try {
                val v = getVibrator(context) ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrateWithAttributes(v, VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrateWithAttributes(v, VibrationEffect.createOneShot(24, 210))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(24)
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Audio recording trigger when touching the mic button.
     */
    fun micStart(haptic: HapticFeedback? = null, context: Context? = null) {
        try {
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Throwable) {}

        if (context != null) {
            try {
                val v = getVibrator(context) ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrateWithAttributes(v, VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrateWithAttributes(v, VibrationEffect.createOneShot(42, 230))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(42)
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Audio recording completion when releasing the mic.
     */
    fun micStop(haptic: HapticFeedback? = null, context: Context? = null) {
        try {
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Throwable) {}

        if (context != null) {
            try {
                val v = getVibrator(context) ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrateWithAttributes(v, VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrateWithAttributes(v, VibrationEffect.createOneShot(20, 160))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(20)
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Card flip tactile snap at exactly 90 degrees.
     */
    fun cardFlipSnap(haptic: HapticFeedback? = null, context: Context? = null) {
        try {
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Throwable) {}

        if (context != null) {
            try {
                val v = getVibrator(context) ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrateWithAttributes(v, VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrateWithAttributes(v, VibrationEffect.createOneShot(22, 190))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(22)
                }
            } catch (_: Throwable) {}
        }
    }

    fun cardFlip(haptic: HapticFeedback? = null, context: Context? = null) = cardFlipSnap(haptic, context)

    /**
     * Double-tap of delight for correct quiz answer or notably good sentence praise.
     */
    fun success(haptic: HapticFeedback? = null, context: Context? = null) {
        try {
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Throwable) {}

        if (context != null) {
            try {
                val v = getVibrator(context) ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrateWithAttributes(v, VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 20, 50, 30)
                    val amplitudes = intArrayOf(0, 180, 0, 240)
                    vibrateWithAttributes(v, VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(40)
                }
            } catch (_: Throwable) {}
        }
    }

    /**
     * Gentle pulse when noticing a correction or wrong answer.
     */
    fun correction(haptic: HapticFeedback? = null, context: Context? = null) {
        try {
            haptic?.performHapticFeedback(HapticFeedbackType.LongPress)
        } catch (_: Throwable) {}

        if (context != null) {
            try {
                val v = getVibrator(context) ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = longArrayOf(0, 30, 60, 35)
                    val amplitudes = intArrayOf(0, 140, 0, 160)
                    vibrateWithAttributes(v, VibrationEffect.createWaveform(timings, amplitudes, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(50)
                }
            } catch (_: Throwable) {}
        }
    }

    fun error(haptic: HapticFeedback? = null, context: Context? = null) = correction(haptic, context)
}
