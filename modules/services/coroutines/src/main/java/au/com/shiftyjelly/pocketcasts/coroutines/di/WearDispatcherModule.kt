package au.com.shiftyjelly.pocketcasts.coroutines.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Wear OS optimized dispatchers with limited parallelism for 2-4 core devices.
 */
@Module
@InstallIn(SingletonComponent::class)
object WearDispatcherModule {

    @Provides
    @WearIoDispatcher
    fun provideWearIoDispatcher(): CoroutineDispatcher = Dispatchers.IO.limitedParallelism(2)

    @Provides
    @WearDefaultDispatcher
    fun provideWearDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default.limitedParallelism(2)
}
