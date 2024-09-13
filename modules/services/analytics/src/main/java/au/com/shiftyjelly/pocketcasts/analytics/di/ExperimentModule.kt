package au.com.shiftyjelly.pocketcasts.analytics.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.analytics.experiments.ExperimentLogger
import au.com.shiftyjelly.pocketcasts.analytics.experiments.ExperimentsProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ExperimentModule {

    @Provides
    @Singleton
    fun provideExperimentProvider(
        @ApplicationContext context: Context,
        accountStatusInfo: AccountStatusInfo,
        logger: ExperimentLogger,
    ): ExperimentsProvider = ExperimentsProvider(context, accountStatusInfo, logger)

    @Provides
    @Singleton
    internal fun provideExperimentLogger(): ExperimentLogger {
        return ExperimentLogger()
    }
}
