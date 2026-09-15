package au.com.shiftyjelly.pocketcasts.repositories.sync

import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

private const val AUTHORIZATION_PENDING = "authorization_pending"
private const val EXPIRED_TOKEN = "expired_token"
private const val MIN_POLL_INTERVAL_SECONDS = 5L

sealed interface DeviceAuthState {
    data object Loading : DeviceAuthState
    data class Ready(
        val userCode: String,
        val verificationUri: String,
        val verificationUriComplete: String,
    ) : DeviceAuthState
    data object Error : DeviceAuthState
    data object Complete : DeviceAuthState
}

/**
 * Requests a device code and polls until the user approves it on another device, rotating the code when it expires.
 */
fun deviceAuthFlow(
    syncManager: SyncManager,
    signInSource: SignInSource,
    isNewAccount: Boolean,
): Flow<DeviceAuthState> = flow {
    emit(DeviceAuthState.Loading)
    while (true) {
        val response = try {
            syncManager.deviceAuthorize()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Failed to request device code")
            emit(DeviceAuthState.Error)
            return@flow
        }
        emit(
            DeviceAuthState.Ready(
                userCode = response.userCode,
                verificationUri = response.verificationUri,
                verificationUriComplete = response.verificationUriComplete,
            ),
        )
        val intervalSeconds = response.interval.toLong().coerceAtLeast(MIN_POLL_INTERVAL_SECONDS)
        var codeExpired = false
        while (!codeExpired) {
            delay(intervalSeconds.seconds)
            val result = syncManager.loginWithDeviceAuth(
                deviceCode = response.deviceCode,
                signInSource = signInSource,
                isNewAccount = isNewAccount,
            )
            when (result) {
                is LoginResult.Success -> {
                    emit(DeviceAuthState.Complete)
                    return@flow
                }

                is LoginResult.Failed if result.messageId == AUTHORIZATION_PENDING -> Unit

                is LoginResult.Failed if result.messageId == EXPIRED_TOKEN -> codeExpired = true

                else -> {
                    Timber.w("Device auth polling stopped: ${(result as? LoginResult.Failed)?.message}")
                    emit(DeviceAuthState.Error)
                    return@flow
                }
            }
        }
    }
}
