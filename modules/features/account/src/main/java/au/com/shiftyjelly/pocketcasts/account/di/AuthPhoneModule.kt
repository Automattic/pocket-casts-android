package au.com.shiftyjelly.pocketcasts.account.di

import au.com.shiftyjelly.pocketcasts.account.TokenSerializer
import com.google.android.horologist.auth.data.phone.tokenshare.TokenBundleRepository
import com.google.android.horologist.auth.data.phone.tokenshare.impl.TokenBundleRepositoryImpl
import com.google.android.horologist.data.WearDataLayerRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Qualifier

@Module
@InstallIn(SingletonComponent::class)
object AuthPhoneModule {

    @Provides
    fun providesTokenBundleRepository(
        wearDataLayerRegistry: WearDataLayerRegistry,
        @ForApplicationScope coroutineScope: CoroutineScope
    ): TokenBundleRepository<String> {
        return TokenBundleRepositoryImpl(
            registry = wearDataLayerRegistry,
            coroutineScope = coroutineScope,
            serializer = TokenSerializer
        )
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ForApplicationScope
