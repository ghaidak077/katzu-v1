package com.example.katzu.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Lightweight, thread-safe file-based logging utility for Katzu.
 * Writes timestamped lines to context.getExternalFilesDir(null)/logs/katzu-log-YYYY-MM-DD.txt.
 * Captures uncaught exceptions globally across the application.
 */
object AppLogger {

    private var appContext: Context? = null
    private val writeLock = Any()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    // In-memory buffer of recent log lines for real-time UI display
    private val recentLogLines = ArrayDeque<String>(20)
    private val _recentLogsFlow = MutableStateFlow<List<String>>(emptyList())
    val recentLogsFlow: StateFlow<List<String>> = _recentLogsFlow.asStateFlow()

    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext

        // Global uncaught exception handler
        val originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                e("CRASH", "Uncaught exception on thread: ${thread.name} (id: ${thread.id})", throwable)
            } catch (t: Throwable) {
                Log.e("AppLogger", "Failed to log crash in exception handler", t)
            } finally {
                originalHandler?.uncaughtException(thread, throwable)
            }
        }

        i("AppLogger", "AppLogger initialized. Log dir: ${getLogDir()?.absolutePath}")
    }

    private fun getLogDir(): File? {
        val ctx = appContext ?: return null
        val external = ctx.getExternalFilesDir(null)
        val baseDir = if (external != null) File(external, "logs") else File(ctx.filesDir, "logs")
        if (!baseDir.exists()) {
            baseDir.mkdirs()
        }
        return baseDir
    }

    fun getTodayLogFile(): File? {
        val dir = getLogDir() ?: return null
        val dateStr = synchronized(dateFormat) { dateFormat.format(Date()) }
        return File(dir, "katzu-log-$dateStr.txt")
    }

    fun d(tag: String, message: String) {
        write("DEBUG", tag, message, null)
    }

    fun i(tag: String, message: String) {
        write("INFO", tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        write("WARN", tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        write("ERROR", tag, message, throwable)
    }

    private fun write(level: String, tag: String, message: String, throwable: Throwable?) {
        // Output to Logcat
        if (com.example.katzu.BuildConfig.DEBUG) {
            when (level) {
                "DEBUG" -> Log.d(tag, message, throwable)
                "INFO" -> Log.i(tag, message, throwable)
                "WARN" -> Log.w(tag, message, throwable)
                "ERROR" -> Log.e(tag, message, throwable)
                else -> Log.d(tag, message, throwable)
            }
        } else if (level == "ERROR") {
            Log.e(tag, message, throwable)
        }

        if (!com.example.katzu.BuildConfig.DEBUG) return

        try {
            val file = getTodayLogFile()
            val timestamp = synchronized(timestampFormat) { timestampFormat.format(Date()) }
            val stackTrace = if (throwable != null) "\n" + Log.getStackTraceString(throwable) else ""
            val logEntry = "$timestamp [$level] [$tag] $message$stackTrace"
            val line = "$logEntry\n"

            synchronized(writeLock) {
                file?.appendText(line)
                if (recentLogLines.size >= 25) {
                    recentLogLines.removeFirst()
                }
                recentLogLines.addLast(logEntry)
                _recentLogsFlow.value = recentLogLines.toList()
            }
        } catch (err: Exception) {
            Log.e("AppLogger", "Failed to write log to file: ${err.message}")
        }
    }

    fun getLastLines(maxLines: Int = 50): String {
        val file = getTodayLogFile() ?: return "No log directory available."
        if (!file.exists() || file.length() == 0L) {
            return "Log file is currently empty."
        }
        return try {
            val lines = file.readLines()
            val slice = if (lines.size > maxLines) lines.takeLast(maxLines) else lines
            slice.joinToString("\n")
        } catch (e: Exception) {
            "Error reading log file: ${e.message}"
        }
    }

    fun shareText(context: Context, text: String, title: String = "مشاركة سجل الأخطاء") {
        if (text.isBlank()) {
            Toast.makeText(context, "سجل الأخطاء فارغ حالياً", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                putExtra(Intent.EXTRA_SUBJECT, "Katzu Log")
            }
            val chooser = Intent.createChooser(intent, title)
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e("AppLogger", "Failed to share text", e)
            Toast.makeText(context, "تعذر مشاركة النص: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareLogFile(context: Context) {
        val file = getTodayLogFile()
        if (file == null || !file.exists() || file.length() == 0L) {
            Toast.makeText(context, "سجل الأخطاء فارغ حالياً", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Katzu Error Log - ${file.name}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "إرسال سجل الأخطاء")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e("AppLogger", "Failed to share log file", e)
            Toast.makeText(context, "تعذر مشاركة السجل: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun copyLastLinesToClipboard(context: Context, maxLines: Int = 50) {
        val text = getLastLines(maxLines)
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Katzu Logs", text)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(context, "تم نسخ آخر $maxLines سطر من السجل إلى الحافظة", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            e("AppLogger", "Failed to copy logs to clipboard", e)
            Toast.makeText(context, "تعذر نسخ السجل: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
