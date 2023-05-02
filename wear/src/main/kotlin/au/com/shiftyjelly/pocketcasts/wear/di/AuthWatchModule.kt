package au.com.shiftyjelly.pocketcasts.wear.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthData
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthDataSerializer
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.wear.ui.authentication.GoogleSignInEventListenerImpl
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.horologist.auth.data.googlesignin.GoogleSignInEventListener
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import com.google.android.horologist.auth.data.tokenshare.impl.TokenBundleRepositoryImpl
import com.google.android.horologist.data.WearDataLayerRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AuthWatchModule {

    @Provides
    fun providesTokenBundleRepository(
        wearDataLayerRegistry: WearDataLayerRegistry,
    ): TokenBundleRepository<WatchSyncAuthData?> {
        return TokenBundleRepositoryImpl.create(
            registry = wearDataLayerRegistry,
            serializer = WatchSyncAuthDataSerializer
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
                .build()
        )
    }

    @Provides
    fun providesGoogleSignInEventListener(
        googleSignInEventListenerImpl: GoogleSignInEventListenerImpl
    ): GoogleSignInEventListener = googleSignInEventListenerImpl
}
