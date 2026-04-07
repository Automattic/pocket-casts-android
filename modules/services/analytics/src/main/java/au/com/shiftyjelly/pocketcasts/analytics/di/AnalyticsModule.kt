package au.com.shiftyjelly.pocketcasts.analytics.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsController
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsListener
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.BuildConfig
import au.com.shiftyjelly.pocketcasts.analytics.EventSink
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsWrapper
import au.com.shiftyjelly.pocketcasts.analytics.LoggingAnalyticsListener
import au.com.shiftyjelly.pocketcasts.analytics.NoOpTracker
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Experiment
import au.com.shiftyjelly.pocketcasts.analytics.experiments.ExperimentProvider
import au.com.shiftyjelly.pocketcasts.servers.di.Cached
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.experimentation.ExperimentLogger
import com.automattic.android.experimentation.VariationsRepository
import com.automattic.eventhorizon.EventHorizon
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Binds
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
abstract class AnalyticsModule {
    companion object {
        // Necessary to satisfy Dagger injection
        @Provides
        @IntoSet
        fun provideNoOpTracker(): AnalyticsTracker = NoOpTracker

        @Provides
        @Singleton
        fun provideEventSink(
            trackers: Set<@JvmSuppressWildcards AnalyticsTracker>,
            listeners: Set<@JvmSuppressWildcards AnalyticsListener>,
        ): EventSink = EventSink(trackers, listeners)

        @Provides
        @Singleton
        fun provideEventHorizon(eventSink: EventSink): EventHorizon {
            return EventHorizon(eventSink)
        }

        @Provides
        @Singleton
        fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalyticsWrapper {
            return FirebaseAnalyticsWrapper(FirebaseAnalytics.getInstance(context))
        }

        @Provides
        @IntoSet
        fun provideLoggingListener(): AnalyticsListener {
            return LoggingAnalyticsListener()
        }

        @Provides
        @Singleton
        fun provideExperimentProvider(
            repository: VariationsRepository,
            accountStatusInfo: AccountStatusInfo,
        ): ExperimentProvider = ExperimentProvider(repository, accountStatusInfo)

        @Provides
        @Singleton
        fun provideVariationsRepository(
            @ApplicationContext context: Context,
            @Cached httpClient: Lazy<OkHttpClient>,
        ): VariationsRepository {
            val directory = File(context.filesDir, "experiments")

            val experiments = Experiment.getAllExperiments().map { experiment ->
                com.automattic.android.experimentation.Experiment(experiment.identifier)
            }.toSet()

            val logger = object : ExperimentLogger {
                override fun d(message: String) {
                    LogBuffer.i(ExperimentProvider.TAG, message)
                }

                override fun e(message: String, throwable: Throwable?) {
                    throwable?.let { LogBuffer.e(ExperimentProvider.TAG, throwable, message) } ?: LogBuffer.e(ExperimentProvider.TAG, message)
                }
            }

            return VariationsRepository.create(
                platform = ExperimentProvider.PLATFORM,
                experiments = experiments,
                logger = logger,
                failFast = BuildConfig.DEBUG,
                cacheDir = directory,
                coroutineScope = CoroutineScope(
                    Dispatchers.IO + SupervisorJob() + CoroutineExceptionHandler { _, throwable ->
                        LogBuffer.e(ExperimentProvider.TAG, throwable, "Uncaught exception in ExPlat coroutine scope")
                        val deleted = runCatching { directory.deleteRecursively() }.getOrDefault(false)
                        if (deleted) {
                            LogBuffer.i(ExperimentProvider.TAG, "Cleared corrupted experiment cache")
                        } else {
                            LogBuffer.e(ExperimentProvider.TAG, "Failed to clear corrupted experiment cache")
                        }
                    },
                ),
                callFactory = { request -> httpClient.get().newCall(request) },
            )
        }
    }

    @Binds
    abstract fun bindAnalyticsController(eventSink: EventSink): AnalyticsController
}
