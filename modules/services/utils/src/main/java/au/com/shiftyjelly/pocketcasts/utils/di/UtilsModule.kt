package au.com.shiftyjelly.pocketcasts.utils.di

import au.com.shiftyjelly.pocketcasts.utils.UUIDProvider
import au.com.shiftyjelly.pocketcasts.utils.UUIDProviderImpl
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

    @Provides
    fun provideUuidProvider(): UUIDProvider = UUIDProviderImpl()
}
