package au.com.shiftyjelly.pocketcasts.analytics.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsLoggingListener
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.AnonymousBumpStatsTracker
import au.com.shiftyjelly.pocketcasts.analytics.EventSink
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsWrapper
import au.com.shiftyjelly.pocketcasts.analytics.Tracker
import au.com.shiftyjelly.pocketcasts.analytics.TracksAnalyticsTracker
import com.automattic.eventhorizon.EventHorizon
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TrackerModule {
    companion object {
        @Provides
        @Singleton
        fun provideAnalyticsTracker(
            trackers: Set<@JvmSuppressWildcards Tracker>,
            listeners: Set<@JvmSuppressWildcards AnalyticsTracker.Listener>,
        ): AnalyticsTracker = AnalyticsTracker(trackers, listeners)

        @Provides
        fun provideEventSink(
            trackers: Set<@JvmSuppressWildcards Tracker>,
            listeners: Set<@JvmSuppressWildcards AnalyticsTracker.Listener>,
        ): EventSink = EventSink(trackers, listeners)

        @Provides
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
        fun provideLoggingAnalyticsListener(): AnalyticsTracker.Listener {
            return AnalyticsLoggingListener()
        }
    }

    @Binds
    @IntoSet
    abstract fun bindTracksAnalyticsTracker(tracker: TracksAnalyticsTracker): Tracker

    @Binds
    @IntoSet
    abstract fun bindFirebaseAnalyticsTracker(tracker: FirebaseAnalyticsTracker): Tracker

    @Binds
    @IntoSet
    abstract fun bindBumpStatsAnalyticsTracker(tracker: AnonymousBumpStatsTracker): Tracker
}
