package au.com.shiftyjelly.pocketcasts.wear.di

import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthData
import au.com.shiftyjelly.pocketcasts.account.watchsync.WatchSyncAuthDataSerializer
import com.google.android.horologist.auth.data.tokenshare.TokenBundleRepository
import com.google.android.horologist.auth.data.tokenshare.impl.TokenBundleRepositoryImpl
import com.google.android.horologist.data.WearDataLayerRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}
