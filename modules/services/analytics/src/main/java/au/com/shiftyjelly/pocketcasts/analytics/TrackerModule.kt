package au.com.shiftyjelly.pocketcasts.analytics

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TrackerModule {
    @Provides
    @Singleton
    fun provideAnalyticsTracker(
        tracksTracker: TracksAnalyticsTracker,
        bumpStatsTracker: AnonymousBumpStatsTracker,
        settings: Settings,
    ): AnalyticsTracker = AnalyticsTracker(
        trackers = listOf(tracksTracker, bumpStatsTracker),
        isTrackingEnabled = { settings.collectAnalytics.value },
    )
}
