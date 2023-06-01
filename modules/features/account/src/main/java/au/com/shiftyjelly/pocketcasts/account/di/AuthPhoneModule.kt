package au.com.shiftyjelly.pocketcasts.account.di

import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthData
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthDataSerializer
import au.com.shiftyjelly.pocketcasts.servers.di.ForApplicationScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.auth.data.phone.tokenshare.TokenBundleRepository
import com.google.android.horologist.auth.data.phone.tokenshare.impl.TokenBundleRepositoryImpl
import com.google.android.horologist.data.WearDataLayerRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
object AuthPhoneModule {

    @ExperimentalHorologistApi
    @Provides
    fun providesTokenBundleRepository(
        wearDataLayerRegistry: WearDataLayerRegistry,
        @ForApplicationScope coroutineScope: CoroutineScope,
    ): TokenBundleRepository<WatchSyncAuthData?> {
        return TokenBundleRepositoryImpl(
            registry = wearDataLayerRegistry,
            coroutineScope = coroutineScope,
            serializer = WatchSyncAuthDataSerializer
        )
    }
}
