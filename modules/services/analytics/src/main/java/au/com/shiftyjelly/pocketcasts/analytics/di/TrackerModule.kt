package au.com.shiftyjelly.pocketcasts.analytics.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.AnonymousBumpStatsTracker
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsWrapper
import au.com.shiftyjelly.pocketcasts.analytics.TracksAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
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
        firebaseAnalyticsTracker: FirebaseAnalyticsTracker,
        settings: Settings,
    ): AnalyticsTracker = AnalyticsTracker(
        trackers = listOf(tracksTracker, bumpStatsTracker, firebaseAnalyticsTracker),
        isTrackingEnabled = { settings.collectAnalytics.value },
    )

    @Provides
    @Singleton
    internal fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalyticsWrapper {
        return FirebaseAnalyticsWrapper(FirebaseAnalytics.getInstance(context))
    }
}
