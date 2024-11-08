package au.com.shiftyjelly.pocketcasts.analytics.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.analytics.BuildConfig
import au.com.shiftyjelly.pocketcasts.analytics.TracksAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Experiment
import au.com.shiftyjelly.pocketcasts.analytics.experiments.ExperimentProvider
import au.com.shiftyjelly.pocketcasts.servers.di.Cached
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.experimentation.ExperimentLogger
import com.automattic.android.experimentation.VariationsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
object ExperimentModule {

    @Provides
    @Singleton
    fun provideExperimentProvider(
        tracksAnalyticsTracker: TracksAnalyticsTracker,
        repository: VariationsRepository,
        accountStatusInfo: AccountStatusInfo,
    ): ExperimentProvider = ExperimentProvider(tracksAnalyticsTracker, repository, accountStatusInfo)

    @Provides
    @Singleton
    fun provideVariationsRepository(
        @ApplicationContext context: Context,
        @Cached okHttpClient: OkHttpClient,
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
            coroutineScope = CoroutineScope(Dispatchers.IO + Job()),
            okhttpClient = okHttpClient,
        )
    }
}
