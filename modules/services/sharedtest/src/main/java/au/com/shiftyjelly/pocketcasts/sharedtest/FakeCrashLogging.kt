package au.com.shiftyjelly.pocketcasts.sharedtest

import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.JsException
import com.automattic.android.tracks.crashlogging.JsExceptionCallback

class FakeCrashLogging : CrashLogging {
    override fun initialize() = Unit

    override fun recordEvent(message: String, category: String?) = Unit

    override fun recordException(exception: Throwable, category: String?) = Unit

    override fun sendJavaScriptReport(jsException: JsException, callback: JsExceptionCallback) = Unit

    override fun sendReport(exception: Throwable?, tags: Map<String, String>, message: String?) = Unit
}
