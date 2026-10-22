package au.com.shiftyjelly.pocketcasts.onboarding.signin

import au.com.shiftyjelly.pocketcasts.repositories.sync.DeviceAuthState
import au.com.shiftyjelly.pocketcasts.repositories.sync.SignInSource
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.deviceAuthFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun tvDeviceAuthFlow(syncManager: SyncManager, isNewAccount: Boolean): Flow<TvSignInUiState> {
    return deviceAuthFlow(
        syncManager = syncManager,
        signInSource = SignInSource.UserInitiated.Onboarding,
        isNewAccount = isNewAccount,
    ).map { state ->
        when (state) {
            DeviceAuthState.Loading -> TvSignInUiState.Loading

            is DeviceAuthState.Ready -> TvSignInUiState.Ready(
                userCode = state.userCode.map { it.toString() },
                verificationUri = state.verificationUri,
                verificationUriComplete = state.verificationUriComplete,
            )

            DeviceAuthState.Error -> TvSignInUiState.Error

            DeviceAuthState.Complete -> TvSignInUiState.Complete
        }
    }
}
