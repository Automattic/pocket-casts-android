package au.com.shiftyjelly.pocketcasts.crashlogging.di

import android.app.Application
import android.content.Context
import au.com.shiftyjelly.pocketcasts.crashlogging.FilteringCrashLogging
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
class CrashLoggingModule {

    @Provides
    fun provideCrashLogging(
        @ApplicationContext application: Context,
        crashLoggingDataProvider: CrashLoggingDataProvider,
        @ApplicationScope appScope: CoroutineScope,
    ): CrashLogging {
        return FilteringCrashLogging(
            CrashLoggingProvider.createInstance(
                application as Application,
                crashLoggingDataProvider,
                appScope,
            ),
        )
    }
}
