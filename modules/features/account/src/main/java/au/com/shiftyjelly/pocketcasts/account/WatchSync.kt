package au.com.shiftyjelly.pocketcasts.account

import android.annotation.SuppressLint
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.horologist.auth.data.phone.tokenshare.TokenBundleRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("VisibleForTests") // https://issuetracker.google.com/issues/239451111
class WatchSync @Inject constructor(
    private val settings: Settings,
    private val accountAuth: AccountAuth,
    private val tokenBundleRepository: TokenBundleRepository<String?>,
) {
    /**
     * This should be called by the phone app to update the refresh token available to
     * the watch app in the data layer.
     */
    suspend fun sendAuthToDataLayer() {
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

                authData?.refreshToken.let { refreshToken ->
                    tokenBundleRepository.update(refreshToken)
                }
            } catch (cancellationException: CancellationException) {
                // Don't catch CancellationException since this represents the normal cancellation of a coroutine
                throw cancellationException
            } catch (exception: Exception) {
                LogBuffer.e(
                    LogBuffer.TAG_BACKGROUND_TASKS,
                    "saving refresh token to data layer failed: $exception"
                )
            }
        }
    }

    suspend fun processAuthDataChange(refreshToken: String?) {
        Timber.i("Received refreshToken change")
        if (refreshToken != null) {
            // Don't do anything if the user is already logged in.
            if (!settings.isLoggedIn()) {
                val result = accountAuth.signInWithToken(refreshToken, SignInSource.WatchPhoneSync)
                when (result) {
                    is AccountAuth.AuthResult.Failed -> { /* do nothing */
                    }

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
}
