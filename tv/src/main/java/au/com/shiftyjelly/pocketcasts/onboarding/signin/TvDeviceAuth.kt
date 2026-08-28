package au.com.shiftyjelly.pocketcasts.onboarding.signin

import au.com.shiftyjelly.pocketcasts.repositories.sync.LoginResult
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

private const val AUTHORIZATION_PENDING = "authorization_pending"
private const val MIN_POLL_INTERVAL_SECONDS = 5L

fun deviceAuthFlow(syncManager: SyncManager, isNewAccount: Boolean): Flow<TvSignInUiState> = flow {
    emit(TvSignInUiState.Loading)
    val response = try {
        syncManager.deviceAuthorize()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Timber.e(e, "Failed to request device code")
        emit(TvSignInUiState.Error)
        return@flow
    }
    emit(
        TvSignInUiState.Ready(
            userCode = response.userCode.map { it.toString() },
            verificationUri = response.verificationUri,
            verificationUriComplete = response.verificationUriComplete,
        ),
    )
    val intervalSeconds = response.interval.toLong().coerceAtLeast(MIN_POLL_INTERVAL_SECONDS)
    while (true) {
        delay(intervalSeconds * 1000)
        val result = syncManager.loginWithDeviceAuth(
            deviceCode = response.deviceCode,
            signInSource = SignInSource.UserInitiated.Onboarding,
            isNewAccount = isNewAccount,
        )
        when {
            result is LoginResult.Success -> {
                emit(TvSignInUiState.Complete)
                return@flow
            }

            result is LoginResult.Failed && result.messageId == AUTHORIZATION_PENDING -> Unit

            else -> {
                Timber.w("Device auth polling stopped: ${(result as? LoginResult.Failed)?.message}")
                emit(TvSignInUiState.Error)
                return@flow
            }
        }
    }
}
