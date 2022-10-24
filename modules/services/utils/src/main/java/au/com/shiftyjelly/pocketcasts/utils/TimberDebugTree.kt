package au.com.shiftyjelly.pocketcasts.utils

import android.os.Build
import android.util.Log
import timber.log.Timber
import java.util.regex.Pattern
import kotlin.math.min

class TimberDebugTree : Timber.Tree() {

    /**
     * Break up {@code message} into maximum-length chunks (if needed) and send to either
     * {@link Log#println(int, String, String) Log.println()} or
     * {@link Log#wtf(String, String) Log.wtf()} for logging.
     *
     * {@inheritDoc}
     */
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val realTag = getRealTag(tag)
        if (message.length < MAX_LOG_LENGTH) {
            if (priority == Log.ASSERT) {
                Log.wtf(realTag, message)
            } else {
                Log.println(priority, realTag, message)
            }
            return
        }

        // Split by line, then ensure each line can fit into Log's maximum length.
        var i = 0
        val length = message.length
        while (i < length) {
            var newline = message.indexOf('\n', i)
            newline = if (newline != -1) newline else length
            do {
                val end = min(newline, i + MAX_LOG_LENGTH)
                val part = message.substring(i, end)
                if (priority == Log.ASSERT) {
                    Log.wtf(realTag, part)
                } else {
                    Log.println(priority, realTag, part)
                }
                i = end
            } while (i < newline)
            i++
        }
    }

    private fun createStackElementTag(element: StackTraceElement): String? {
        var tag = element.className
        val matcher = ANONYMOUS_CLASS.matcher(tag)
        if (matcher.find()) tag = matcher.replaceAll("")
        tag = tag.substring(tag.lastIndexOf('.') + 1)
        // Tag length limit was removed in API 24.
        return if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            tag
        } else tag.substring(0, MAX_TAG_LENGTH)
    }

    private fun getRealTag(tag: String?): String? {
        if (tag != null) return tag

        val stackTrace = Throwable().stackTrace
        check(stackTrace.isNotEmpty()) { "Synthetic stacktrace didn't have enough elements: are you using proguard?" }
        return createStackElementTag(stackTrace[CALL_STACK_INDEX])
    }

    companion object {
        private const val MAX_LOG_LENGTH = 400
        private const val MAX_TAG_LENGTH = 23
        private const val CALL_STACK_INDEX = 4
        private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
    }
}
