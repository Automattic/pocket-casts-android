package au.com.shiftyjelly.pocketcasts.wear.ui.component

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.concurrent.futures.await
import androidx.core.net.toUri
import androidx.wear.remote.interactions.RemoteActivityHelper
import au.com.shiftyjelly.pocketcasts.wear.WearLogging
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

suspend fun openUrlOnPhone(url: String, context: Context) {
    try {
        val intent = Intent(Intent.ACTION_VIEW)
            .addCategory(Intent.CATEGORY_BROWSABLE)
            .setData(url.toUri())
        RemoteActivityHelper(context, Dispatchers.IO.asExecutor())
            .startRemoteActivity(intent)
            .await()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        // On Wear SDK 6+ (API 36) RemoteActivityHelper fails with a bare Throwable rather than an Exception.
        Timber.i(e, "${WearLogging.PREFIX} Failed to open url $url on phone")
        Toast.makeText(context, LR.string.settings_could_not_open_on_phone, Toast.LENGTH_SHORT)
            .show()
    }
}
