package au.com.shiftyjelly.pocketcasts.analytics.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.analytics.experiments.Experiment
import au.com.shiftyjelly.pocketcasts.analytics.experiments.ExperimentLogger
import au.com.shiftyjelly.pocketcasts.analytics.experiments.ExperimentsProvider
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

@Module
@InstallIn(SingletonComponent::class)
object ExperimentModule {

    @Provides
    @Singleton
    fun provideExperimentProvider(
        accountStatusInfo: AccountStatusInfo,
        repository: VariationsRepository,
    ): ExperimentsProvider = ExperimentsProvider(accountStatusInfo, repository)

    @Provides
    @Singleton
    internal fun provideExperimentLogger(): ExperimentLogger {
        return ExperimentLogger()
    }

    @Provides
    @Singleton
    fun provideVariationsRepository(
        @ApplicationContext context: Context,
        logger: ExperimentLogger,
    ): VariationsRepository {
        val cacheDir = File(context.cacheDir, "experiments_cache").apply {
            if (!exists()) mkdirs()
        }

        val experiments = Experiment.getAllExperiments().map { experiment ->
            com.automattic.android.experimentation.Experiment(experiment.identifier)
        }.toSet()

        return VariationsRepository.create(
            platform = ExperimentsProvider.PLATFORM,
            experiments = experiments,
            logger = logger,
            failFast = true,
            cacheDir = cacheDir,
            coroutineScope = CoroutineScope(Dispatchers.IO + Job()),
        )
    }
}
