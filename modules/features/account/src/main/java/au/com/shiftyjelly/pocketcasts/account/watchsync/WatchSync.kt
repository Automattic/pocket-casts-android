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
    /**
     * This should be called by the phone app to update the refresh token available to
     * the watch app in the data layer.
     */
    @OptIn(ExperimentalHorologistApi::class)
    suspend fun sendAuthToDataLayer(): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                Timber.i("Updating WatchSyncAuthData in data layer")

                val watchSyncAuthData = syncManager.getRefreshToken()?.let { refreshToken ->
                    syncManager.getLoginIdentity()?.let { loginIdentity ->
                        WatchSyncAuthData(
                            refreshToken = refreshToken,
                            loginIdentity = loginIdentity,
                        )
                    }
                }

                if (watchSyncAuthData == null) {
                    Timber.i("Removing WatchSyncAuthData from data layer")
                }

                tokenBundleRepository.update(watchSyncAuthData)
                Result.success(Unit)
            } catch (cancellationException: CancellationException) {
                // Don't catch CancellationException since this represents the normal cancellation of a coroutine
                throw cancellationException
            } catch (exception: Exception) {
                LogBuffer.e(
                    LogBuffer.TAG_BACKGROUND_TASKS,
                    "Saving refresh token to data layer failed: $exception",
                )
                Result.failure(exception)
            }
        }
    }

    suspend fun processAuthDataChange(data: WatchSyncAuthData?, onResult: (LoginResult) -> Unit) {
        try {
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
                    Timber.i("Already logged in, skipping login")
                }
            } else {
                // The user either was never logged in on their phone or just logged out.
                // Either way, leave the user's login state on the watch unchanged.
                Timber.i("Received null WatchSyncAuthData (no login from phone yet)")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            LogBuffer.e(TAG, "Failed to process auth data: ${e.message}")
            onResult(LoginResult.Failed(message = e.message ?: "Unknown error", messageId = null))
        }
    }

    companion object {
        private const val TAG = "WatchSync"
    }
}
