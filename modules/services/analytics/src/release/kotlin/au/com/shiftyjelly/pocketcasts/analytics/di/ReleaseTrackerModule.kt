package au.com.shiftyjelly.pocketcasts.analytics.di

import au.com.shiftyjelly.pocketcasts.analytics.AnonymousBumpStatsTracker
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.Tracker
import au.com.shiftyjelly.pocketcasts.analytics.TracksAnalyticsTracker
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
abstract class ReleaseTrackerModule {
    @Binds
    @IntoSet
    abstract fun bindTracksTracker(tracker: TracksAnalyticsTracker): Tracker

    @Binds
    @IntoSet
    abstract fun bindFirebaseTracker(tracker: FirebaseAnalyticsTracker): Tracker

    @Binds
    @IntoSet
    abstract fun bindBumpStatsTracker(tracker: AnonymousBumpStatsTracker): Tracker
}
