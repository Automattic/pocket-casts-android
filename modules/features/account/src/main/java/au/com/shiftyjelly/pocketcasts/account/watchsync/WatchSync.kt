package au.com.shiftyjelly.pocketcasts.account.watchsync

import android.annotation.SuppressLint
import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.auth.data.phone.tokenshare.TokenBundleRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
@SuppressLint("VisibleForTests") // https://issuetracker.google.com/issues/239451111
class WatchSync
@OptIn(ExperimentalHorologistApi::class)
@Inject
constructor(
    private val syncManager: SyncManager,
    private val tokenBundleRepository: TokenBundleRepository<WatchSyncAuthData?>,
) {
    companion object {
        private const val TAG = "WatchSync"
    }

    /**
     * This should be called by the phone app to update the refresh token available to
     * the watch app in the data layer.
     */
    @OptIn(ExperimentalHorologistApi::class)
    suspend fun sendAuthToDataLayer() {
        withContext(Dispatchers.Default) {
            try {
                LogBuffer.i(TAG, "Initiating auth token sync to Wear")

                val watchSyncAuthData = syncManager.getRefreshToken()?.let { refreshToken ->
                    syncManager.getLoginIdentity()?.let { loginIdentity ->
                        WatchSyncAuthData(
                            refreshToken = refreshToken,
                            loginIdentity = loginIdentity,
                        )
                    }
                }

                if (watchSyncAuthData == null) {
                    LogBuffer.i(TAG, "Removing auth token from Wear: Phone not logged in to Pocket Casts")
                }

                tokenBundleRepository.update(watchSyncAuthData)

                if (watchSyncAuthData != null) {
                    LogBuffer.i(TAG, "Successfully sent auth token to Wear via Data Layer")
                } else {
                    LogBuffer.i(TAG, "Successfully removed auth token from Wear Data Layer")
                }
            } catch (cancellationException: CancellationException) {
                // Don't catch CancellationException since this represents the normal cancellation of a coroutine
                throw cancellationException
            } catch (exception: Exception) {
                LogBuffer.e(
                    TAG,
                    "Failed to sync auth token to Wear Data Layer: ${exception.message}",
                )
            }
        }
    }

    suspend fun processAuthDataChange(data: WatchSyncAuthData?, onResult: (LoginResult) -> Unit) {
        if (data != null) {
            Timber.i("Received WatchSyncAuthData change from phone")

            if (!syncManager.isLoggedIn()) {
                val result = syncManager.loginWithToken(
                    token = data.refreshToken,
                    loginIdentity = data.loginIdentity,
                    signInSource = SignInSource.WatchPhoneSync,
                )
                onResult(result)
            } else {
                Timber.i("Received WatchSyncAuthData from phone, but user is already logged in")
            }
        } else {
            // The user either was never logged in on their phone or just logged out.
            // Either way, leave the user's login state on the watch unchanged.
            Timber.i("Received null WatchSyncAuthData change")
        }
    }
}
