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
import com.automattic.encryptedlogging.AutomatticEncryptedLogging
import com.automattic.encryptedlogging.EncryptedLogging
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CrashLoggingModule {

    @Binds
    internal abstract fun bindLocaleProvider(localeProvider: ContextBasedLocaleProvider): LocaleProvider

    companion object {
        @Provides
        fun provideEncryptedLogging(@ApplicationContext context: Context): EncryptedLogging {
            return AutomatticEncryptedLogging(
                context,
                encryptedLoggingKey = BuildConfig.ENCRYPTION_KEY,
                clientSecret = BuildConfig.APP_SECRET,
            )
        }

        @Provides
        fun provideBuildDataProvider(): BuildDataProvider {
            return object : BuildDataProvider {
                override val buildPlatform: String = BuildConfig.BUILD_PLATFORM
            }
        }

        @Provides
        fun provideApplicationFilesDir(@ApplicationContext application: Context): File {
            return File(application.filesDir, "logs")
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
