package au.com.shiftyjelly.pocketcasts.repositories.appreview

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsListener
import au.com.shiftyjelly.pocketcasts.analytics.TrackedEvent
import au.com.shiftyjelly.pocketcasts.preferences.ReadWriteSetting
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.BookmarkCreatedEvent
import com.automattic.eventhorizon.EndOfYearStoriesDismissedEvent
import com.automattic.eventhorizon.EndOfYearStorySharedEvent
import com.automattic.eventhorizon.EndOfYearStoryType
import com.automattic.eventhorizon.EpisodeStarredEvent
import com.automattic.eventhorizon.FilterCreatedEvent
import com.automattic.eventhorizon.FolderSavedEvent
import com.automattic.eventhorizon.PlayerEpisodeCompletedEvent
import com.automattic.eventhorizon.PurchaseSuccessfulEvent
import com.automattic.eventhorizon.RatingScreenSubmitTappedEvent
import com.automattic.eventhorizon.ReferralPassSharedEvent
import com.automattic.eventhorizon.SettingsAppearanceThemeChangedEvent
import com.automattic.eventhorizon.SuggestedFoldersReplaceFoldersConfirmTappedEvent
import com.automattic.eventhorizon.Trackable
import java.time.Clock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppReviewAnalyticsListener @Inject constructor(
    private val settings: Settings,
    private val clock: Clock,
) : AnalyticsListener {
    private val episodesCompletedSetting = settings.appReviewEpisodeCompletedTimestamps
    private val episodeStarredSetting = settings.appReviewEpisodeStarredTimestamp
    private val podcastRatedSetting = settings.appReviewPodcastRatedTimestamp
    private val playlistCreatedSetting = settings.appReviewPlaylistCreatedTimestamp
    private val plusUpgradedSetting = settings.appReviewPlusUpgradedTimestamp
    private val folderCreatedSetting = settings.appReviewFolderCreatedTimestamp
    private val bookmarkCreatedSetting = settings.appReviewBookmarkCreatedTimestamp
    private val themeChangedSetting = settings.appReviewThemeChangedTimestamp
    private val referralSharedSetting = settings.appReviewReferralSharedTimestamp
    private val endOfYearSharedSetting = settings.appReviewEndOfYearSharedTimestamp
    private val endOfYearCompletedSetting = settings.appReviewEndOfYearCompletedTimestamp

    override fun onEvent(
        event: Trackable,
        trackedEvents: Map<String, TrackedEvent?>,
    ) {
        if (!FeatureFlag.isEnabled(Feature.IMPROVE_APP_RATINGS)) {
            return
        }

        when (event) {
            is PlayerEpisodeCompletedEvent -> {
                val timestamps = episodesCompletedSetting.value
                if (timestamps.size < 3) {
                    episodesCompletedSetting.set(timestamps + clock.instant(), updateModifiedAt = false)
                }
            }

            is EpisodeStarredEvent -> {
                updateSetting(episodeStarredSetting)
            }

            is RatingScreenSubmitTappedEvent -> {
                updateSetting(podcastRatedSetting)
            }

            is FilterCreatedEvent -> {
                updateSetting(playlistCreatedSetting)
            }

            is PurchaseSuccessfulEvent -> {
                updateSetting(plusUpgradedSetting)
            }

            is FolderSavedEvent, is SuggestedFoldersReplaceFoldersConfirmTappedEvent -> {
                updateSetting(folderCreatedSetting)
            }

            is BookmarkCreatedEvent -> {
                updateSetting(bookmarkCreatedSetting)
            }

            is SettingsAppearanceThemeChangedEvent -> {
                updateSetting(themeChangedSetting)
            }

            is ReferralPassSharedEvent -> {
                updateSetting(referralSharedSetting)
            }

            is EndOfYearStorySharedEvent -> {
                updateSetting(endOfYearSharedSetting)
            }

            is EndOfYearStoriesDismissedEvent -> {
                if (event.story == EndOfYearStoryType.Ending) {
                    updateSetting(endOfYearCompletedSetting)
                }
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
