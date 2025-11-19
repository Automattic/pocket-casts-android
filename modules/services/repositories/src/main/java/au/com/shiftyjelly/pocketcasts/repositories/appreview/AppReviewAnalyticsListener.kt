package au.com.shiftyjelly.pocketcasts.repositories.appreview

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.ReadWriteSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppReviewAnalyticsListener @Inject constructor(
    private val settings: Settings,
    private val clock: Clock,
) : AnalyticsTracker.Listener {
    private val episodesCompletedSetting = settings.appReviewEpisodeCompletedTimestamps
    private val episodeStarredSetting = settings.appReviewEpisodeStarredTimestamp
    private val podcastRatedSetting = settings.appReviewPodcastRatedTimestamp
    private val playlistCreatedSetting = settings.appReviewPlaylistCreatedTimestamp
    private val plusUpgradedSetting = settings.appReviewPlusUpgradedTimestamp
    private val folderCreatedSetting = settings.appReviewFolderCreatedTimestamp
    private val bookmarkCreatedSetting = settings.appReviewBookmarkCreatedTimestamp
    private val themeChangedSetting = settings.appReviewThemeChangedTimestamp
    private val referralSharedSetting = settings.appReviewReferralSharedTimestamp
    private val playbackSharedSetting = settings.appReviewPlaybackSharedTimestamp

    override fun onEvent(event: AnalyticsEvent, properties: Map<String, Any>) {
        if (!FeatureFlag.isEnabled(Feature.IMPROVE_APP_RATINGS)) {
            return
        }

        when (event) {
            AnalyticsEvent.PLAYER_EPISODE_COMPLETED -> {
                val timestamps = episodesCompletedSetting.value
                if (timestamps.size < 3) {
                    episodesCompletedSetting.set(timestamps + clock.instant(), updateModifiedAt = false)
                }
            }

            AnalyticsEvent.EPISODE_STARRED -> {
                updateSetting(episodeStarredSetting)
            }

            AnalyticsEvent.RATING_SCREEN_SUBMIT_TAPPED -> {
                updateSetting(podcastRatedSetting)
            }

            AnalyticsEvent.FILTER_CREATED -> {
                updateSetting(playlistCreatedSetting)
            }

            AnalyticsEvent.PURCHASE_SUCCESSFUL -> {
                updateSetting(plusUpgradedSetting)
            }

            AnalyticsEvent.FOLDER_SAVED, AnalyticsEvent.SUGGESTED_FOLDERS_REPLACE_FOLDERS_CONFIRM_TAPPED -> {
                updateSetting(folderCreatedSetting)
            }

            AnalyticsEvent.BOOKMARK_CREATED -> {
                updateSetting(bookmarkCreatedSetting)
            }

            AnalyticsEvent.SETTINGS_APPEARANCE_THEME_CHANGED -> {
                updateSetting(themeChangedSetting)
            }

            AnalyticsEvent.REFERRAL_PASS_SHARED -> {
                updateSetting(referralSharedSetting)
            }

            AnalyticsEvent.END_OF_YEAR_STORY_SHARED -> {
                updateSetting(playbackSharedSetting)
            }

            else -> Unit
        }
    }

    private fun updateSetting(setting: ReadWriteSetting<Instant?>) {
        val value = setting.value
        if (value == null) {
            setting.set(clock.instant(), updateModifiedAt = false)
        }
    }
}
