package com.example.katzu.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.katzu.MainActivity
import com.example.katzu.R
import java.util.Calendar
import kotlin.random.Random

object NotificationHelper {

    const val CHANNEL_ID = "katzu_reminders_channel"
    private const val CHANNEL_NAME = "تذكيرات كاتزو اليومية"
    private const val NOTIFICATION_ID = 1001
    private const val ALARM_REQUEST_CODE = 2001

    private val KATZU_QUIPS = listOf(
        "هل نسيت الفرق بين der و die؟ 5 دقائق محادثة كفيلة بإنقاذ ماء الوجه! 🐾",
        "سلسلة الحماس في خطر! قطتك البنفسجية تنتظر لتصحيح أخطائك، 5 دقائق فقط.",
        "الألمان لا ينتظرون أحداً، وقطار المحادثة على وشك الانطلاق. هل أنت مستعد؟",
        "لا تجعل برلين تضحك على لكنتك! افتح كاتزو وتحدث جملتين على الأقل الآن.",
        "الكسل لن يجعلك تتحدث الألمانية! 5 دقائق محادثة تجعل يومك ناجحاً.",
        "تذكير ودي من كاتزو: حتى الأخطاء تبدو رائعة إذا قلتها بثقة. هيّا نتكلم!",
        "محادثة قصيرة اليوم تحميك من التردد غداً. كاتزو في انتظارك!"
    )

    private val KATZU_PRAISE_QUIPS = listOf(
        "أنت سحقت هدفك اليوم في الألمانية وتحدثت بطلاقة! كاتزو فخور بك، استرح اليوم ونلتقي غداً. 🐾🎉",
        "مستوى رائع اليوم! أنجزت محادثتك وحافظت على السلسلة. استمتع بباقي يومك كالناطقين بالألمانية.",
        "كاتزو يحييك: أثبتّ التزامك وتحدثت ببراعة اليوم! راحة مستحقة تماماً. 🌟",
        "هدف اليوم تم إتقانه بنجاح! الألمانية تبدو أسهل معك خطوة بخطوة. نراك غداً في تحدٍ جديد!"
    )

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "تنبيهات ذكية للمحافظة على استمرارية التحدث وتثبيت اللغة الألمانية"
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
            AppLogger.i("NotificationHelper", "Notification channel '$CHANNEL_ID' verified/created.")
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun showReminderNotification(context: Context) {
        if (!hasNotificationPermission(context)) {
            AppLogger.w("NotificationHelper", "Cannot show notification: POST_NOTIFICATIONS permission not granted.")
            return
        }

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("from_notification", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Activity-aware check: verify if user completed a session today
        val prefs = context.getSharedPreferences("katzu_settings_prefs", Context.MODE_PRIVATE)
        val lastCompletedAt = prefs.getLong("last_session_completed_at", 0L)
        val now = System.currentTimeMillis()

        val calLast = Calendar.getInstance().apply { timeInMillis = lastCompletedAt }
        val calNow = Calendar.getInstance().apply { timeInMillis = now }
        val isCompletedToday = (lastCompletedAt > 0L &&
                calLast.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calLast.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR))

        val title: String
        val message: String

        if (isCompletedToday) {
            title = "أبدعت اليوم في الألمانية! 🎉"
            message = KATZU_PRAISE_QUIPS[Random.nextInt(KATZU_PRAISE_QUIPS.size)]
            AppLogger.i("NotificationHelper", "Activity-aware: User completed session today. Dispatching praise notification.")
        } else {
            title = "كاتزو يسأل عنك 🐾"
            message = KATZU_QUIPS[Random.nextInt(KATZU_QUIPS.size)]
            AppLogger.i("NotificationHelper", "Activity-aware: No session completed yet today. Dispatching standard reminder.")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(NOTIFICATION_ID, builder.build())
            AppLogger.i("NotificationHelper", "Notification dispatched successfully: $message")
        } catch (e: SecurityException) {
            AppLogger.e("NotificationHelper", "SecurityException dispatching notification: ${e.message}", e)
        } catch (e: Exception) {
            AppLogger.e("NotificationHelper", "Failed to dispatch notification: ${e.message}", e)
        }
    }

    fun scheduleReminder(
        context: Context,
        frequencyPreset: String,
        hour: Int,
        minute: Int
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, KatzuReminderReceiver::class.java).apply {
            putExtra("frequency_preset", frequencyPreset)
            putExtra("hour", hour)
            putExtra("minute", minute)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            // If time has already passed today, advance to tomorrow
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val triggerAtMillis = calendar.timeInMillis

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            AppLogger.i("NotificationHelper", "Scheduled reminder alarm for: ${calendar.time} (preset: $frequencyPreset)")
        } catch (e: Exception) {
            AppLogger.e("NotificationHelper", "Failed to schedule alarm: ${e.message}", e)
        }
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, KatzuReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        AppLogger.i("NotificationHelper", "Cancelled scheduled Katzu reminder alarm.")
    }
}
