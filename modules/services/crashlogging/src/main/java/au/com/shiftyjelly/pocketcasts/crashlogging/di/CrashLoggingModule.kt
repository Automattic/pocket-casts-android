package au.com.shiftyjelly.pocketcasts.crashlogging.di

import android.app.Application
import android.content.Context
import au.com.shiftyjelly.pocketcasts.crashlogging.BuildConfig
import au.com.shiftyjelly.pocketcasts.crashlogging.BuildDataProvider
import au.com.shiftyjelly.pocketcasts.crashlogging.ContextBasedLocaleProvider
import au.com.shiftyjelly.pocketcasts.crashlogging.FilteringCrashLogging
import au.com.shiftyjelly.pocketcasts.crashlogging.LocaleProvider
import au.com.shiftyjelly.pocketcasts.crashlogging.PocketCastsCrashLoggingDataProvider
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.android.tracks.crashlogging.CrashLoggingProvider
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CrashLoggingModule {

    @Binds
    internal abstract fun bindLocaleProvider(localeProvider: ContextBasedLocaleProvider): LocaleProvider

    companion object {
        @Provides
        fun provideBuildDataProvider(): BuildDataProvider {
            return object : BuildDataProvider {
                override val buildPlatform: String = BuildConfig.BUILD_PLATFORM
            }
        }

        @Provides
        @Singleton
        internal fun provideCrashLogging(
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
}
