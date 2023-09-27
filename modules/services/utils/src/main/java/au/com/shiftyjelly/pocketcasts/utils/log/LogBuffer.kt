package au.com.shiftyjelly.pocketcasts.utils.log

import android.util.Log
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper
import timber.log.Timber
import timber.log.Timber.Forest.tag
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date

object LogBuffer {

    const val TAG_PLAYBACK = "Playback"
    const val TAG_CRASH = "Crash"
    const val TAG_BACKGROUND_TASKS = "BgTask"
    const val TAG_RX_JAVA_DEFAULT_ERROR_HANDLER = "RxJavaDefaultErrorHandler"
    const val TAG_SUBSCRIPTIONS = "Subscriptions"
    const val TAG_INVALID_STATE = "InvalidState"

    private const val LOG_FILE_NAME = "debug.log"
    private const val LOG_BACKUP_FILE_NAME = "debug.log.1"

    private val LOG_FILE_DATE_FORMAT = SimpleDateFormat("dd/M HH:mm:ss")
    private const val FILE_MAX_SIZE_BYTES = (200 * 1024).toLong()

    private var logPath: String? = null
    private var logBackupPath: String? = null

    fun setup(dirPath: String) {
        val dirFile = File(dirPath)
        if (!dirFile.exists()) {
            dirFile.mkdirs()
        }
        logPath = File(dirPath, LOG_FILE_NAME).absolutePath
        logBackupPath = File(dirPath, LOG_BACKUP_FILE_NAME).absolutePath
    }

    @Synchronized
    private fun add(message: String?) {
        val path = logPath
        val backupPath = logBackupPath
        if (message == null || message.isEmpty() || path == null || backupPath == null) {
            return
        }

        var out: FileWriter? = null
        try {
            val logFile = File(path)
            var exists = logFile.exists()
            if (exists && logFile.length() > FILE_MAX_SIZE_BYTES) {
                val logBackupFile = File(backupPath)
                if (logBackupFile.exists()) {
                    logBackupFile.delete()
                }
                logFile.renameTo(logBackupFile)
                exists = false
            }

            out = FileWriter(logPath, exists)
            out.write(message)
            out.write("\n")
            out.flush()
        } catch (e: IOException) {
            Timber.w(e, "Unable to write log buffer %s", logPath)
        } finally {
            try {
                out?.close()
            } catch (t: Throwable) {
            }
        }
    }

    fun output(out: OutputStream) {
        val path = logPath
        val backupPath = logBackupPath
        if (path == null || backupPath == null) {
            return
        }
        try {
            val logFile = File(path)
            val logBackupFile = File(backupPath)
            if (logBackupFile.exists()) {
                FileUtil.readFileTo(logBackupFile, out)
            }
            if (logFile.exists()) {
                FileUtil.readFileTo(logFile, out)
            }
        } catch (e: IOException) {
            Timber.w(e, "Unable to output log buffer to file %s", path)
        }
    }

    fun clearLog() {
        val path = logPath
        val backupPath = logBackupPath
        if (path == null || backupPath == null) {
            return
        }
        val logFile = File(path)
        val logBackupFile = File(backupPath)
        logFile.delete()
        logBackupFile.delete()
    }

    fun i(tag: String, message: String, vararg args: Any) {
        addLog(Log.INFO, tag, null, message, *args)
    }

    fun i(tag: String, throwable: Throwable, message: String, vararg args: Any) {
        addLog(Log.INFO, tag, throwable, message, *args)
    }

    fun w(tag: String, message: String, vararg args: Any) {
        addLog(Log.WARN, tag, null, message, *args)
    }

    fun e(tag: String, message: String, vararg args: Any) {
        addLog(Log.ERROR, tag, null, message, *args)
    }

    fun trace(tag: String, message: String, vararg args: Any) {
        addLog(Log.INFO, tag, null, message + getThreadStackTraceString(), *args)
    }

    fun e(tag: String, throwable: Throwable, message: String, vararg args: Any) {
        addLog(Log.ERROR, tag, throwable, message, *args)
    }

    /**
     * Log the exception with the appropriate priority based on the exception type.
     * Exceptions such as network timeouts are logged at the info level as they can't be fixed.
     */
    fun logException(tag: String, throwable: Throwable, message: String, vararg args: Any) {
        val priority = if (SentryHelper.shouldIgnoreExceptions(throwable)) Log.INFO else Log.ERROR
        addLog(priority, tag, throwable, message, *args)
    }

    @Suppress("NAME_SHADOWING")
    fun addLog(priority: Int, tag: String, throwable: Throwable?, message: String?, vararg args: Any) {
        var message = message
        if (message != null && message.isEmpty()) {
            message = null
        }
        if (message == null) {
            if (throwable == null) {
                return
            }
            message = getStackTraceString(throwable)
        } else {
            if (args.isNotEmpty()) {
                message = String.format(message, *args)
            }
            if (throwable != null) {
                message += "\n" + getStackTraceString(throwable)
            }
        }

        var prefix: String
        val timberPrefix = "$tag: "
        when (priority) {
            Log.DEBUG -> {
                prefix = "D "
                Timber.d(timberPrefix + message)
            }
            Log.INFO -> {
                prefix = "I "
                Timber.i(timberPrefix + message)
            }
            Log.WARN -> {
                prefix = "W "
                Timber.w(timberPrefix + message)
            }
            Log.ERROR -> {
                prefix = "E "
                Timber.e(timberPrefix + message)
            }
            else -> prefix = ""
        }

        prefix += LOG_FILE_DATE_FORMAT.format(Date()) // + " ["+tag+"]";

        add("$prefix $message")
    }

    private fun getThreadStackTraceString(): String {
        val sw = StringWriter(256)
        val pw = PrintWriter(sw, false)
        Throwable().printStackTrace(pw)
        pw.flush()
        val rawTrace = sw.toString()
        val lines = rawTrace.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val trace = StringBuilder()
        for (i in lines.indices) {
            if (i == 0) {
                continue
            }
            val line = lines[i]
            if (!line.startsWith("\tat au.com.shiftyjelly.pocketcasts.") || line.contains("LogBuffer")) {
                continue
            }
            trace.append("\n at ").append(line.substring(34))
        }
        return trace.toString()
    }

    private fun getStackTraceString(t: Throwable): String {
        // Don't replace this with Log.getStackTraceString() - it hides
        // UnknownHostException, which is not what we want.
        val sw = StringWriter(256)
        val pw = PrintWriter(sw, false)
        t.printStackTrace(pw)
        pw.flush()
        return sw.toString()
    }
}
