package au.com.shiftyjelly.pocketcasts.crashlogging.di

import android.app.Application
import android.content.Context
import au.com.shiftyjelly.pocketcasts.crashlogging.FilteringCrashLogging
import au.com.shiftyjelly.pocketcasts.crashlogging.PocketCastsCrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
class CrashLoggingModule {

    @Provides
    fun provideCrashLogging(
        @ApplicationContext application: Context,
        crashLoggingDataProvider: PocketCastsCrashLoggingDataProvider,
        provideApplicationScope: ProvideApplicationScope,
    ): CrashLogging {
        return FilteringCrashLogging(
            CrashLoggingProvider.createInstance(
                application as Application,
                crashLoggingDataProvider,
                provideApplicationScope(),
            ),
        )
    }
}
