package au.com.shiftyjelly.pocketcasts.sharedtest

import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.JsException
import com.automattic.android.tracks.crashlogging.JsExceptionCallback

class FakeCrashLogging : CrashLogging {
    override fun initialize() {
        TODO("Not yet implemented")
    }

    override fun recordEvent(message: String, category: String?) {
        TODO("Not yet implemented")
    }

    override fun recordException(exception: Throwable, category: String?) {
        TODO("Not yet implemented")
    }

    override fun sendJavaScriptReport(jsException: JsException, callback: JsExceptionCallback) {
        TODO("Not yet implemented")
    }

    override fun sendReport(exception: Throwable?, tags: Map<String, String>, message: String?) {
        TODO("Not yet implemented")
    }
}
