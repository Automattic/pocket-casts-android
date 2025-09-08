package au.com.shiftyjelly.pocketcasts.account.watchsync

import android.content.Context
import android.os.Bundle
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCustomCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.NoCredentialException
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.servers.sync.LoginIdentity
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.timeout

sealed class AuthResult {
    data class Success(val authData: WatchSyncAuthData) : AuthResult()
    object NoCredentials : AuthResult()
    object Cancelled : AuthResult()
    object ManualSignInRequired : AuthResult()
}

@OptIn(FlowPreview::class)
class HybridAuthManager(
    private val context: Context,
    private val tokenBundleRepository: TokenBundleRepository<WatchSyncAuthData?>,
) {

    private val credentialManager = CredentialManager.create(context)
    private val wearAuthManager = WearAuthManager(context)

    suspend fun signIn(): AuthResult {
        val cmResult = tryCredentialManager()
        if (cmResult is AuthResult.Success) return cmResult

        wearAuthManager.startListening()

        wearAuthManager.stopListening()

        val ultimately = merge(
            tokenBundleRepository.flow,
            wearAuthManager.refreshToken.map {
                WatchSyncAuthData(
                    refreshToken = RefreshToken(it),
                    loginIdentity = LoginIdentity.PocketCasts,
                )
            },
        ).timeout(2.seconds)
            .firstOrNull()

        return if (ultimately != null) {
            AuthResult.Success(ultimately)
        } else {
            AuthResult.ManualSignInRequired
        }
    }

    private suspend fun tryCredentialManager(): AuthResult {
        return try {
            val request = GetCredentialRequest(
                listOf(
                    GetCustomCredentialOption(
                        type = "pocketcasts.auth.refreshToken",
                        requestData = Bundle(),
                        candidateQueryData = Bundle(),
                        isSystemProviderRequired = false,
                        isAutoSelectAllowed = true,
                        allowedProviders = setOf(), // TODO check this
                    )
                )
            )
            val response = credentialManager.getCredential(context, request)
            val cred = response.credential as? PasswordCredential
            if (cred != null) AuthResult.Success(WatchSyncAuthData(refreshToken = RefreshToken(value = "oooooof"), loginIdentity = LoginIdentity.PocketCasts)) else AuthResult.NoCredentials
        } catch (e: NoCredentialException) {
            AuthResult.NoCredentials
        } catch (e: GetCredentialCancellationException) {
            AuthResult.Cancelled
        } catch (e: Exception) {
            AuthResult.NoCredentials
        }
    }
}