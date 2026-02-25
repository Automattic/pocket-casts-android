package au.com.shiftyjelly.pocketcasts.utils.di

import au.com.shiftyjelly.pocketcasts.utils.UUIDProvider
import au.com.shiftyjelly.pocketcasts.utils.UUIDProviderImpl
import au.com.shiftyjelly.pocketcasts.utils.accessibility.AccessibilityManager
import au.com.shiftyjelly.pocketcasts.utils.accessibility.AccessibilityManagerImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import kotlin.time.TimeSource

@Module
@InstallIn(SingletonComponent::class)
object UtilsModule {
    @Provides
    fun provideClock(): Clock = Clock.systemUTC()

    @Provides
    fun provideTimeSource(): TimeSource = TimeSource.Monotonic

    @Provides
    fun provideUuidProvider(): UUIDProvider = UUIDProviderImpl()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class UtilsBindingsModule {
    @Binds
    abstract fun bindAccessibilityManager(impl: AccessibilityManagerImpl): AccessibilityManager
}
