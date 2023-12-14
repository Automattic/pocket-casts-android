package au.com.shiftyjelly.pocketcasts.account.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.data.WearDataLayerRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @OptIn(ExperimentalHorologistApi::class)
    @Singleton
    @Provides
    fun providesWearDataLayerRegistry(
        @ApplicationContext context: Context,
        @ApplicationScope coroutineScope: CoroutineScope
    ): WearDataLayerRegistry {
        return WearDataLayerRegistry.fromContext(
            application = context,
            coroutineScope = coroutineScope
        )
    }
}
