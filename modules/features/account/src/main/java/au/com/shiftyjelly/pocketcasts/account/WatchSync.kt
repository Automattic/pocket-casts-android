package au.com.shiftyjelly.pocketcasts.account

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("VisibleForTests") // https://issuetracker.google.com/issues/239451111
class WatchSync @Inject constructor(
    @ApplicationContext context: Context,
    private val settings: Settings,
    private val accountAuth: AccountAuth,
) {

    companion object {
        private const val authPath = "/auth"
        private const val authKeyRefreshToken = "refreshToken"
    }

    private val dataClient = Wearable.getDataClient(context)

    /**
     * This should be called by the phone app to update the refresh token available to
     * the watch app in the data layer.
     */
    suspend fun sendAuthToDataLayer(activity: Activity) {
        withContext(Dispatchers.Default) {
            try {
                Timber.i("Updating refresh token in data layer")

                val authData = let {
                    val email = settings.getSyncEmail()
                    val password = settings.getSyncPassword()
                    if (email != null && password != null) {
                        // FIXME nonononono this makes an api call _every_ time
                        accountAuth.getTokensWithEmailAndPassword(email, password)
                    } else null
                }

                val putDataReq: PutDataRequest = PutDataMapRequest.create(authPath).apply {
                    authData?.refreshToken?.let {

                        dataMap.putString(authKeyRefreshToken, it)
                    } ?: dataMap.remove(authKeyRefreshToken)
                }
                    .asPutDataRequest()
                    .setUrgent()

                Wearable
                    .getDataClient(activity)
                    .putDataItem(putDataReq)
            } catch (cancellationException: CancellationException) {
                // Don't catch CancellationException since this represents the normal cancellation of a coroutine
                throw cancellationException
            } catch (exception: Exception) {
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "saving refresh token to data layer failed: $exception")
            }
        }
    }

    suspend fun processDataChange(dataEventBuffer: DataEventBuffer) {
        Timber.i("Received DataLayer change")
        dataEventBuffer.use { buffer ->
            buffer.forEach { event ->
                processEvent(event)
            }
        }
    }

    suspend fun processLatestData() {
        Timber.i("Checking latest sync data from Data Layer")
        dataClient.dataItems.await().use { dataItemBuffer ->
            dataItemBuffer.forEach { item ->
                processItem(item)
            }
        }
    }

    private suspend fun processEvent(event: DataEvent) {
        processItem(event.dataItem)
    }

    private suspend fun processAuthItem(item: DataItem) {
        val dataMap = DataMapItem.fromDataItem(item).dataMap
        val refreshToken = dataMap.getString(authKeyRefreshToken)
        if (refreshToken != null) {
            // Don't do anything if the user is already logged in.
            if (!settings.isLoggedIn()) {
                val result = accountAuth.signInWithToken(refreshToken, SignInSource.WatchPhoneSync)
                when (result) {
                    is AccountAuth.AuthResult.Failed -> { /* do nothing */ }
                    is AccountAuth.AuthResult.Success -> {
                        Timber.e("TODO: notify the user we have signed them in!")
                    }
                }
            } else {
                Timber.i("Received refreshToken from phone, but user is already logged in")
            }
        } else {
            // The user either was never logged in on their phone or just logged out.
            // Either way, leave the user's login state on the watch unchanged.
            Timber.i("Received data from phone without refresh token")
        }
    }

    private suspend fun processItem(item: DataItem) {
        val path = item.uri.path
        Timber.i("Processing DataItem with path: $path")
        when (path) {
            authPath -> {
                processAuthItem(item)
            }
            else -> {
                Timber.e("Unable to process DataItem with unknown path: $path")
            }
        }
    }
}
