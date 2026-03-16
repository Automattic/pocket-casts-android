package au.com.shiftyjelly.pocketcasts.repositories.appreview

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.AppThemeType
import com.automattic.eventhorizon.BookmarkCreatedEvent
import com.automattic.eventhorizon.BookmarkSourceType
import com.automattic.eventhorizon.EndOfYearShareSourceType
import com.automattic.eventhorizon.EndOfYearStoriesDismissedEvent
import com.automattic.eventhorizon.EndOfYearStoryCloseSourceType
import com.automattic.eventhorizon.EndOfYearStorySharedEvent
import com.automattic.eventhorizon.EndOfYearStoryType
import com.automattic.eventhorizon.EpisodeStarredEvent
import com.automattic.eventhorizon.FilterCreatedEvent
import com.automattic.eventhorizon.FolderSavedEvent
import com.automattic.eventhorizon.PlayerEpisodeCompletedEvent
import com.automattic.eventhorizon.PurchaseSuccessfulEvent
import com.automattic.eventhorizon.RatingScreenSubmitTappedEvent
import com.automattic.eventhorizon.ReferralPassSharedEvent
import com.automattic.eventhorizon.SettingsAboutWebsiteTappedEvent
import com.automattic.eventhorizon.SettingsAppearanceThemeChangedEvent
import com.automattic.eventhorizon.SourceViewType
import com.automattic.eventhorizon.SubscriptionFrequencyType
import com.automattic.eventhorizon.SubscriptionTierType
import com.automattic.eventhorizon.SuggestedFolderSourceType
import com.automattic.eventhorizon.SuggestedFoldersReplaceFoldersConfirmTappedEvent
import java.time.Instant
import kotlin.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class AppReviewAnalyticsListenerTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val episodesCompletedSetting = TestSetting(emptyList<Instant>())
    private val episodeStarredSetting = TestSetting<Instant?>(null)
    private val podcastRatedSetting = TestSetting<Instant?>(null)
    private val playlistCreatedSetting = TestSetting<Instant?>(null)
    private val plusUpgradedSetting = TestSetting<Instant?>(null)
    private val folderCreatedSetting = TestSetting<Instant?>(null)
    private val bookmarkCreatedSetting = TestSetting<Instant?>(null)
    private val themeChangedSetting = TestSetting<Instant?>(null)
    private val referralSharedSetting = TestSetting<Instant?>(null)
    private val endOfYearSharedSetting = TestSetting<Instant?>(null)
    private val endOfYearCompletedSetting = TestSetting<Instant?>(null)

    private val clock = MutableClock()

    private val settings = mock<Settings> {
        on { appReviewEpisodeCompletedTimestamps } doReturn episodesCompletedSetting
        on { appReviewEpisodeStarredTimestamp } doReturn episodeStarredSetting
        on { appReviewPodcastRatedTimestamp } doReturn podcastRatedSetting
        on { appReviewPlaylistCreatedTimestamp } doReturn playlistCreatedSetting
        on { appReviewPlusUpgradedTimestamp } doReturn plusUpgradedSetting
        on { appReviewFolderCreatedTimestamp } doReturn folderCreatedSetting
        on { appReviewBookmarkCreatedTimestamp } doReturn bookmarkCreatedSetting
        on { appReviewThemeChangedTimestamp } doReturn themeChangedSetting
        on { appReviewReferralSharedTimestamp } doReturn referralSharedSetting
        on { appReviewEndOfYearSharedTimestamp } doReturn endOfYearSharedSetting
        on { appReviewEndOfYearCompletedTimestamp } doReturn endOfYearCompletedSetting
    }

    private val listener = AppReviewAnalyticsListener(settings, clock)

    @Before
    fun setup() {
        FeatureFlag.setEnabled(Feature.IMPROVE_APP_RATINGS, true)
    }

    @Test
    fun `does not update settings when feature flag is disabled`() {
        FeatureFlag.setEnabled(Feature.IMPROVE_APP_RATINGS, false)

        listener.onEvent(
            EpisodeStarredEvent(
                source = SourceViewType.Player,
                episodeUuid = "",
            ),
        )

        assertNull(episodeStarredSetting.value)
    }

    @Test
    fun `updates episodeStarred setting on EPISODE_STARRED event`() {
        listener.onEvent(
            EpisodeStarredEvent(
                episodeUuid = "",
                source = SourceViewType.Player,
            ),
        )

        assertEquals(clock.instant(), episodeStarredSetting.value)
    }

    @Test
    fun `updates podcastRated setting on RATING_SCREEN_SUBMIT_TAPPED event`() {
        listener.onEvent(
            RatingScreenSubmitTappedEvent(
                uuid = "",
                stars = 0,
            ),
        )

        assertEquals(clock.instant(), podcastRatedSetting.value)
    }

    @Test
    fun `updates playlistCreated setting on FILTER_CREATED event`() {
        listener.onEvent(FilterCreatedEvent())

        assertEquals(clock.instant(), playlistCreatedSetting.value)
    }

    @Test
    fun `updates plusUpgraded setting on PURCHASE_SUCCESSFUL event`() {
        listener.onEvent(
            PurchaseSuccessfulEvent(
                tier = SubscriptionTierType.Plus,
                frequency = SubscriptionFrequencyType.Yearly,
                isInstallment = false,
                source = "",
            ),
        )

        assertEquals(clock.instant(), plusUpgradedSetting.value)
    }

    @Test
    fun `updates folderCreated setting on FOLDER_SAVED event`() {
        listener.onEvent(
            FolderSavedEvent(
                numberOfPodcasts = 0,
                color = "",
            ),
        )

        assertEquals(clock.instant(), folderCreatedSetting.value)
    }

    @Test
    fun `updates folderCreated setting on SUGGESTED_FOLDERS_REPLACE_FOLDERS_CONFIRM_TAPPED event`() {
        listener.onEvent(
            SuggestedFoldersReplaceFoldersConfirmTappedEvent(
                source = SuggestedFolderSourceType.Popup,
            ),
        )

        assertEquals(clock.instant(), folderCreatedSetting.value)
    }

    @Test
    fun `updates bookmarkCreated setting on BOOKMARK_CREATED event`() {
        listener.onEvent(
            BookmarkCreatedEvent(
                podcastUuid = "",
                episodeUuid = "",
                time = 0,
                source = BookmarkSourceType.Player,
            ),
        )

        assertEquals(clock.instant(), bookmarkCreatedSetting.value)
    }

    @Test
    fun `updates themeChanged setting on SETTINGS_APPEARANCE_THEME_CHANGED event`() {
        listener.onEvent(
            SettingsAppearanceThemeChangedEvent(
                value = AppThemeType.DarkContrast,
            ),
        )

        assertEquals(clock.instant(), themeChangedSetting.value)
    }

    @Test
    fun `updates referralShared setting on REFERRAL_PASS_SHARED event`() {
        listener.onEvent(
            ReferralPassSharedEvent(
                code = "",
                source = SourceViewType.Referrals,
            ),
        )

        assertEquals(clock.instant(), referralSharedSetting.value)
    }

    @Test
    fun `updates endOfYearShared setting on END_OF_YEAR_STORY_SHARED event`() {
        listener.onEvent(
            EndOfYearStorySharedEvent(
                currentYear = 0,
                story = EndOfYearStoryType.YearVsYear,
                from = EndOfYearShareSourceType.Screenshot,
            ),
        )

        assertEquals(clock.instant(), endOfYearSharedSetting.value)
    }

    @Test
    fun `updates endOfYearCompleted setting when END_OF_YEAR_STORIES_DISMISSED with ending story`() {
        listener.onEvent(
            EndOfYearStoriesDismissedEvent(
                currentYear = 0,
                story = EndOfYearStoryType.Ending,
                source = EndOfYearStoryCloseSourceType.CloseButton,
            ),
        )

        assertEquals(clock.instant(), endOfYearCompletedSetting.value)
    }

    @Test
    fun `does not update endOfYearCompleted setting when END_OF_YEAR_STORIES_DISMISSED with another story`() {
        listener.onEvent(
            EndOfYearStoriesDismissedEvent(
                currentYear = 0,
                story = EndOfYearStoryType.Top1Show,
                source = EndOfYearStoryCloseSourceType.CloseButton,
            ),
        )

        assertNull(endOfYearCompletedSetting.value)
    }

    @Test
    fun `does not update endOfYearCompleted setting when END_OF_YEAR_STORIES_DISMISSED with no story property`() {
        listener.onEvent(
            EndOfYearStoriesDismissedEvent(
                currentYear = 0,
                story = null,
                source = EndOfYearStoryCloseSourceType.CloseButton,
            ),
        )

        assertNull(endOfYearCompletedSetting.value)
    }

    @Test
    fun `adds first episode completed timestamp on PLAYER_EPISODE_COMPLETED event`() {
        listener.onEvent(
            PlayerEpisodeCompletedEvent(
                podcastUuid = "",
                episodeUuid = "",
            ),
        )

        assertEquals(1, episodesCompletedSetting.value.size)
        assertEquals(clock.instant(), episodesCompletedSetting.value[0])
    }

    @Test
    fun `adds second episode completed timestamp on PLAYER_EPISODE_COMPLETED event`() {
        episodesCompletedSetting.set(listOf(clock.instant()))

        listener.onEvent(
            PlayerEpisodeCompletedEvent(
                podcastUuid = "",
                episodeUuid = "",
            ),
        )

        assertEquals(2, episodesCompletedSetting.value.size)
    }

    @Test
    fun `adds third episode completed timestamp on PLAYER_EPISODE_COMPLETED event`() {
        episodesCompletedSetting.set(listOf(clock.instant(), clock.instant()))

        listener.onEvent(
            PlayerEpisodeCompletedEvent(
                podcastUuid = "",
                episodeUuid = "",
            ),
        )

        assertEquals(3, episodesCompletedSetting.value.size)
    }

    @Test
    fun `does not add fourth episode completed timestamp on PLAYER_EPISODE_COMPLETED event`() {
        episodesCompletedSetting.set(listOf(clock.instant(), clock.instant(), clock.instant()))

        listener.onEvent(
            PlayerEpisodeCompletedEvent(
                podcastUuid = "",
                episodeUuid = "",
            ),
        )

        assertEquals(3, episodesCompletedSetting.value.size)
    }

    @Test
    fun `does not update setting if already set`() {
        val firstTimestamp = clock.instant()
        episodeStarredSetting.set(firstTimestamp)

        clock += Duration.parse("1h")
        listener.onEvent(
            EpisodeStarredEvent(
                source = SourceViewType.Player,
                episodeUuid = "",
            ),
        )

        assertEquals(firstTimestamp, episodeStarredSetting.value)
    }

    @Test
    fun `ignores unknown events`() {
        listener.onEvent(SettingsAboutWebsiteTappedEvent)

        assertNull(episodeStarredSetting.value)
        assertNull(podcastRatedSetting.value)
        assertNull(playlistCreatedSetting.value)
    }
}
