package au.com.shiftyjelly.pocketcasts.crashlogging.fakes

import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.JsException
import com.automattic.android.tracks.crashlogging.JsExceptionCallback

class FakeCrashLogging : CrashLogging {

    var initialized = false

    override fun initialize() {
        initialized = true
    }

    override fun recordEvent(message: String, category: String?) = Unit

    override fun recordException(exception: Throwable, category: String?) = Unit

    override fun sendJavaScriptReport(jsException: JsException, callback: JsExceptionCallback) =
        Unit

    override fun sendReport(exception: Throwable?, tags: Map<String, String>, message: String?) =
        Unit
}
