package au.com.shiftyjelly.pocketcasts.utils.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock

@Module
@InstallIn(SingletonComponent::class)
object UtilsModule {
    @Provides
    fun provideClock(): Clock = Clock.systemUTC()
}
