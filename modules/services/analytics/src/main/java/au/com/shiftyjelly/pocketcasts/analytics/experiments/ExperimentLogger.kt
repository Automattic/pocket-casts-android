package au.com.shiftyjelly.pocketcasts.analytics.experiments

import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.experimentation.ExperimentLogger

class ExperimentLogger : ExperimentLogger {
    override fun d(message: String) {
        LogBuffer.i(ExperimentProvider.TAG, message)
    }

    override fun e(message: String, throwable: Throwable?) {
        throwable?.let { LogBuffer.e(ExperimentProvider.TAG, throwable, message) } ?: LogBuffer.e(ExperimentProvider.TAG, message)
    }
}
