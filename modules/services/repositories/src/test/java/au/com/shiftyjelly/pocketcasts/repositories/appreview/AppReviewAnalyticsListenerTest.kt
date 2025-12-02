package au.com.shiftyjelly.pocketcasts.repositories.appreview

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MutableClock
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
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

    private val tracker = AnalyticsTracker.test(AppReviewAnalyticsListener(settings, clock))

    @Before
    fun setup() {
        FeatureFlag.setEnabled(Feature.IMPROVE_APP_RATINGS, true)
    }

    @Test
    fun `does not update settings when feature flag is disabled`() {
        FeatureFlag.setEnabled(Feature.IMPROVE_APP_RATINGS, false)

        tracker.track(AnalyticsEvent.EPISODE_STARRED)

        assertNull(episodeStarredSetting.value)
    }

    @Test
    fun `updates episodeStarred setting on EPISODE_STARRED event`() {
        tracker.track(AnalyticsEvent.EPISODE_STARRED)

        assertEquals(clock.instant(), episodeStarredSetting.value)
    }

    @Test
    fun `updates podcastRated setting on RATING_SCREEN_SUBMIT_TAPPED event`() {
        tracker.track(AnalyticsEvent.RATING_SCREEN_SUBMIT_TAPPED)

        assertEquals(clock.instant(), podcastRatedSetting.value)
    }

    @Test
    fun `updates playlistCreated setting on FILTER_CREATED event`() {
        tracker.track(AnalyticsEvent.FILTER_CREATED)

        assertEquals(clock.instant(), playlistCreatedSetting.value)
    }

    @Test
    fun `updates plusUpgraded setting on PURCHASE_SUCCESSFUL event`() {
        tracker.track(AnalyticsEvent.PURCHASE_SUCCESSFUL)

        assertEquals(clock.instant(), plusUpgradedSetting.value)
    }

    @Test
    fun `updates folderCreated setting on FOLDER_SAVED event`() {
        tracker.track(AnalyticsEvent.FOLDER_SAVED)

        assertEquals(clock.instant(), folderCreatedSetting.value)
    }

    @Test
    fun `updates folderCreated setting on SUGGESTED_FOLDERS_REPLACE_FOLDERS_CONFIRM_TAPPED event`() {
        tracker.track(AnalyticsEvent.SUGGESTED_FOLDERS_REPLACE_FOLDERS_CONFIRM_TAPPED)

        assertEquals(clock.instant(), folderCreatedSetting.value)
    }

    @Test
    fun `updates bookmarkCreated setting on BOOKMARK_CREATED event`() {
        tracker.track(AnalyticsEvent.BOOKMARK_CREATED)

        assertEquals(clock.instant(), bookmarkCreatedSetting.value)
    }

    @Test
    fun `updates themeChanged setting on SETTINGS_APPEARANCE_THEME_CHANGED event`() {
        tracker.track(AnalyticsEvent.SETTINGS_APPEARANCE_THEME_CHANGED)

        assertEquals(clock.instant(), themeChangedSetting.value)
    }

    @Test
    fun `updates referralShared setting on REFERRAL_PASS_SHARED event`() {
        tracker.track(AnalyticsEvent.REFERRAL_PASS_SHARED)

        assertEquals(clock.instant(), referralSharedSetting.value)
    }

    @Test
    fun `updates endOfYearShared setting on END_OF_YEAR_STORY_SHARED event`() {
        tracker.track(AnalyticsEvent.END_OF_YEAR_STORY_SHARED)

        assertEquals(clock.instant(), endOfYearSharedSetting.value)
    }

    @Test
    fun `updates endOfYearCompleted setting when END_OF_YEAR_STORIES_DISMISSED with ending story`() {
        val properties = mapOf("story" to "ending")

        tracker.track(AnalyticsEvent.END_OF_YEAR_STORIES_DISMISSED, properties)

        assertEquals(clock.instant(), endOfYearCompletedSetting.value)
    }

    @Test
    fun `does not update endOfYearCompleted setting when END_OF_YEAR_STORIES_DISMISSED with another story`() {
        val properties = mapOf("story" to "intro")

        tracker.track(AnalyticsEvent.END_OF_YEAR_STORIES_DISMISSED, properties)

        assertNull(endOfYearCompletedSetting.value)
    }

    @Test
    fun `does not update endOfYearCompleted setting when END_OF_YEAR_STORIES_DISMISSED with no story property`() {
        tracker.track(AnalyticsEvent.END_OF_YEAR_STORIES_DISMISSED)

        assertNull(endOfYearCompletedSetting.value)
    }

    @Test
    fun `adds first episode completed timestamp on PLAYER_EPISODE_COMPLETED event`() {
        tracker.track(AnalyticsEvent.PLAYER_EPISODE_COMPLETED)

        assertEquals(1, episodesCompletedSetting.value.size)
        assertEquals(clock.instant(), episodesCompletedSetting.value[0])
    }

    @Test
    fun `adds second episode completed timestamp on PLAYER_EPISODE_COMPLETED event`() {
        episodesCompletedSetting.set(listOf(clock.instant()))

        tracker.track(AnalyticsEvent.PLAYER_EPISODE_COMPLETED)

        assertEquals(2, episodesCompletedSetting.value.size)
    }

    @Test
    fun `adds third episode completed timestamp on PLAYER_EPISODE_COMPLETED event`() {
        episodesCompletedSetting.set(listOf(clock.instant(), clock.instant()))

        tracker.track(AnalyticsEvent.PLAYER_EPISODE_COMPLETED)

        assertEquals(3, episodesCompletedSetting.value.size)
    }

    @Test
    fun `does not add fourth episode completed timestamp on PLAYER_EPISODE_COMPLETED event`() {
        episodesCompletedSetting.set(listOf(clock.instant(), clock.instant(), clock.instant()))

        tracker.track(AnalyticsEvent.PLAYER_EPISODE_COMPLETED)

        assertEquals(3, episodesCompletedSetting.value.size)
    }

    @Test
    fun `does not update setting if already set`() {
        val firstTimestamp = clock.instant()
        episodeStarredSetting.set(firstTimestamp)

        clock += Duration.parse("1h")
        tracker.track(AnalyticsEvent.EPISODE_STARRED)

        assertEquals(firstTimestamp, episodeStarredSetting.value)
    }

    @Test
    fun `ignores unknown events`() {
        tracker.track(AnalyticsEvent.PLAYBACK_PLAY)

        assertNull(episodeStarredSetting.value)
        assertNull(podcastRatedSetting.value)
        assertNull(playlistCreatedSetting.value)
    }
}
