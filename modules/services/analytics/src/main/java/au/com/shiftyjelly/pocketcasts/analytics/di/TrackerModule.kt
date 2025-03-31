package au.com.shiftyjelly.pocketcasts.analytics.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.AnonymousBumpStatsTracker
import au.com.shiftyjelly.pocketcasts.analytics.AppsFlyerAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.AppsFlyerAnalyticsWrapper
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsWrapper
import au.com.shiftyjelly.pocketcasts.analytics.TracksAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
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
        appsFlyerAnalyticsTracker: AppsFlyerAnalyticsTracker,
        settings: Settings,
    ): AnalyticsTracker = AnalyticsTracker(
        trackers = listOf(tracksTracker, bumpStatsTracker, firebaseAnalyticsTracker, appsFlyerAnalyticsTracker),
        isFirstPartyTrackingEnabled = { settings.collectAnalytics.value },
        isThirdPartyTrackingEnabled = { settings.collectAnalyticsThirdParty.value },
    )

    @Provides
    @Singleton
    internal fun provideFirebaseAnalytics(@ApplicationContext context: Context): FirebaseAnalyticsWrapper {
        return FirebaseAnalyticsWrapper(FirebaseAnalytics.getInstance(context))
    }

    @Provides
    @Singleton
    internal fun provideAppsFlyerAnalytics(settings: Settings, @ApplicationContext context: Context): AppsFlyerAnalyticsWrapper {
        val appsFlyerLib = if (FeatureFlag.isEnabled(Feature.APPSFLYER_ANALYTICS)) {
            AppsFlyerAnalyticsWrapper.setupAppsFlyerLib(context)
        } else {
            null
        }

        return AppsFlyerAnalyticsWrapper(
            appsFlyerLib = appsFlyerLib,
            isTrackingEnabled = { settings.collectAnalyticsThirdParty.value },
            context = context,
        )
    }
}
