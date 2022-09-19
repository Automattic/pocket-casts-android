package au.com.shiftyjelly.pocketcasts.wear.data.service.log

import android.content.res.Resources
import androidx.annotation.StringRes
import com.google.android.horologist.media3.logging.ErrorReporter
import timber.log.Timber

class Logging(
    private val res: Resources,
) : ErrorReporter {
    override fun showMessage(@StringRes message: Int) {
        val messageString = res.getString(message)
        Timber.i("ErrorReporter $messageString")
    }

    override fun logMessage(
        message: String,
        category: ErrorReporter.Category,
        level: ErrorReporter.Level,
    ) {
        when (level) {
            ErrorReporter.Level.Error -> Timber.e(category.name, message)
            ErrorReporter.Level.Info -> Timber.i(category.name, message)
            ErrorReporter.Level.Debug -> Timber.d(category.name, message)
        }
    }
}
