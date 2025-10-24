@file:Suppress("DEPRECATION")

package au.com.shiftyjelly.pocketcasts.wear.di

import android.content.Context
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthData
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthDataSerializer
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import com.google.android.horologist.auth.data.tokenshare.impl.TokenBundleRepositoryImpl
import com.google.android.horologist.data.WearDataLayerRegistry
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.util.UUID

@Module
@InstallIn(SingletonComponent::class)
object AuthWatchModule {

    @Provides
    fun providesTokenBundleRepository(
        wearDataLayerRegistry: WearDataLayerRegistry,
        serializer: WatchSyncAuthDataSerializer,
    ): TokenBundleRepository<WatchSyncAuthData?> {
        return TokenBundleRepositoryImpl.create(
            registry = wearDataLayerRegistry,
            serializer = serializer,
        )
    }

    @Provides
    fun providesGoogleSignInClient(
        @ApplicationContext application: Context,
    ): GoogleSignInClient {
        return GoogleSignIn.getClient(
            application,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken(Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID)
                .build(),
        )
    }

    @Provides
    fun provideCredentialsRequest(): GetCredentialRequest {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setRequestVerifiedPhoneNumber(false)
            .setServerClientId(Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID)
            .setNonce(UUID.randomUUID().toString())
            .build()

        return GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .addCredentialOption(GetPasswordOption())
            .build()
    }
}
