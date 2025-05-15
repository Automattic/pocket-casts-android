package au.com.shiftyjelly.pocketcasts.utils.log

import android.util.Log
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStream
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.IllegalFormatException
import java.util.Locale
import timber.log.Timber

object LogBuffer {

    const val TAG_PLAYBACK = "Playback"
    const val TAG_CRASH = "Crash"
    const val TAG_BACKGROUND_TASKS = "BgTask"
    const val TAG_RX_JAVA_DEFAULT_ERROR_HANDLER = "RxJavaDefaultErrorHandler"
    const val TAG_SUBSCRIPTIONS = "Subscriptions"
    const val TAG_INVALID_STATE = "InvalidState"

    private const val LOG_FILE_NAME = "debug.log"
    private const val LOG_BACKUP_FILE_NAME = "debug.log.1"

    private val LOG_FILE_DATE_FORMAT = SimpleDateFormat("dd/M HH:mm:ss.SSS", Locale.US)
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
        if (message.isNullOrEmpty() || path == null || backupPath == null) {
            return
        }

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

            FileWriter(logPath, exists).use { writer ->
                writer.write(message)
                writer.write("\n")
                writer.flush()
            }
        } catch (e: IOException) {
            Timber.w(e, "Unable to write log buffer %s", logPath)
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

    fun e(tag: String, throwable: Throwable, message: String, vararg args: Any) {
        addLog(Log.ERROR, tag, throwable, message, *args)
    }

    fun addLog(priority: Int, tag: String, throwable: Throwable?, message: String?, vararg args: Any) {
        var logMessage = message
        if (logMessage != null && logMessage.isEmpty()) {
            logMessage = null
        }
        if (logMessage == null) {
            if (throwable == null) {
                return
            }
            logMessage = getStackTraceString(throwable)
        } else {
            if (args.isNotEmpty()) {
                logMessage = logMessage.formatCatching(*args)
            }
            if (throwable != null) {
                logMessage += "\n" + getStackTraceString(throwable)
            }
        }

        var prefix: String
        val timberPrefix = "$tag: "
        when (priority) {
            Log.DEBUG -> {
                prefix = "D "
                Timber.tag(tag).d("%s%s", timberPrefix, logMessage)
            }
            Log.INFO -> {
                prefix = "I "
                Timber.tag(tag).i("%s%s", timberPrefix, logMessage)
            }
            Log.WARN -> {
                prefix = "W "
                Timber.tag(tag).w("%s%s", timberPrefix, logMessage)
            }
            Log.ERROR -> {
                prefix = "E "
                Timber.tag(tag).e("%s%s", timberPrefix, logMessage)
            }
            else -> prefix = ""
        }

        prefix += LOG_FILE_DATE_FORMAT.format(Date())

        add("$prefix $logMessage")
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

    private fun String.formatCatching(vararg args: Any) = try {
        this.format(*args)
    } catch (e: IllegalFormatException) {
        // Return the string without the arguments, including the error
        val errorDetails = "Unable to format log message with args ${args.contentToString()}"
        Timber.e(e, errorDetails)
        "$this ($errorDetails)"
    }
}
