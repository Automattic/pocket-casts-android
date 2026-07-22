package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolder
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.podcasts.view.folders.SuggestedFoldersPopupPolicy
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.repositories.ads.BlazeAdsManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SuggestedFoldersManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EventHorizon
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastsViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Test
    fun `changing to recently played waits for recently played podcasts before updating sort type`() = runTest {
        val sortTypeFlow = MutableStateFlow(PodcastsSortType.DATE_ADDED_NEWEST_TO_OLDEST)
        val latestEpisodePodcasts = MutableSharedFlow<List<Podcast>>(replay = 1)
        val recentlyPlayedPodcasts = MutableSharedFlow<List<Podcast>>()
        latestEpisodePodcasts.emit(listOf(podcast("latest-episode-podcast")))

        val viewModel = createViewModel(
            sortTypeFlow = sortTypeFlow,
            latestEpisodePodcasts = latestEpisodePodcasts,
            recentlyPlayedPodcasts = recentlyPlayedPodcasts,
        )

        viewModel.uiState.test {
            var initialState = awaitItem()
            while (initialState.items.isEmpty()) {
                initialState = awaitItem()
            }
            assertEquals(PodcastsSortType.DATE_ADDED_NEWEST_TO_OLDEST, initialState.sortType)
            assertEquals(listOf("latest-episode-podcast"), initialState.items.map { it.uuid })

            sortTypeFlow.value = PodcastsSortType.RECENTLY_PLAYED
            expectNoEvents()

            recentlyPlayedPodcasts.emit(listOf(podcast("recently-played-podcast")))

            val recentlyPlayedState = awaitItem()
            assertEquals(PodcastsSortType.RECENTLY_PLAYED, recentlyPlayedState.sortType)
            assertEquals(listOf("recently-played-podcast"), recentlyPlayedState.items.map { it.uuid })
        }
    }

    private fun createViewModel(
        sortTypeFlow: MutableStateFlow<PodcastsSortType>,
        latestEpisodePodcasts: MutableSharedFlow<List<Podcast>>,
        recentlyPlayedPodcasts: MutableSharedFlow<List<Podcast>>,
    ): PodcastsViewModel {
        val sortTypeSetting = mock<UserSetting<PodcastsSortType>> {
            on { flow } doReturn sortTypeFlow
            on { value } doReturn sortTypeFlow.value
        }
        val notificationsPromptAcknowledgedSetting = mock<UserSetting<Boolean>> {
            on { flow } doReturn MutableStateFlow(false)
        }
        val podcastBadgeTypeSetting = mock<UserSetting<BadgeType>> {
            on { flow } doReturn MutableStateFlow(BadgeType.OFF)
            on { value } doReturn BadgeType.OFF
        }
        val podcastGridLayoutSetting = mock<UserSetting<PodcastGridLayoutType>> {
            on { flow } doReturn MutableStateFlow(PodcastGridLayoutType.LARGE_ARTWORK)
            on { value } doReturn PodcastGridLayoutType.LARGE_ARTWORK
        }
        val showRecentlyPlayedTooltip = mock<UserSetting<Boolean>> {
            on { value } doReturn false
        }
        val settings = mock<Settings> {
            on { podcastsSortType } doReturn sortTypeSetting
            on { notificationsPromptAcknowledged } doReturn notificationsPromptAcknowledgedSetting
            on { podcastBadgeType } doReturn podcastBadgeTypeSetting
            on { podcastGridLayout } doReturn podcastGridLayoutSetting
            on { refreshStateFlow } doReturn MutableStateFlow<RefreshState>(RefreshState.Never)
            on { showPodcastsRecentlyPlayedSortOrderTooltip } doReturn showRecentlyPlayedTooltip
        }

        val podcastManager = mock<PodcastManager>()
        whenever(podcastManager.observePodcastsSortedByLatestEpisode()).thenReturn(latestEpisodePodcasts)
        whenever(podcastManager.observePodcastsBySortedRecentlyPlayed()).thenReturn(recentlyPlayedPodcasts)

        val folderManager = mock<FolderManager>()
        whenever(folderManager.observeFolders()).thenReturn(flowOf(emptyList()))

        val suggestedFoldersManager = mock<SuggestedFoldersManager>()
        whenever(suggestedFoldersManager.observeSuggestedFolders()).thenReturn(flowOf(emptyList<SuggestedFolder>()))

        val userManager = mock<UserManager>()
        whenever(userManager.getSignInState()).thenReturn(Flowable.just(SignInState.SignedOut))

        val notificationHelper = mock<NotificationHelper>()
        whenever(notificationHelper.hasNotificationsPermission()).thenReturn(true)

        val blazeAdsManager = mock<BlazeAdsManager>()
        whenever(blazeAdsManager.findPodcastListAd()).thenReturn(flowOf<BlazeAd?>(null))

        return PodcastsViewModel(
            podcastManager = podcastManager,
            episodeManager = mock<EpisodeManager>(),
            folderManager = folderManager,
            settings = settings,
            eventHorizon = EventHorizon(TestEventSink()),
            suggestedFoldersManager = suggestedFoldersManager,
            suggestedFoldersPopupPolicy = mock<SuggestedFoldersPopupPolicy>(),
            userManager = userManager,
            notificationHelper = notificationHelper,
            blazeAdsManager = blazeAdsManager,
            folderUuid = null,
        )
    }

    private fun podcast(uuid: String) = Podcast(
        uuid = uuid,
        title = uuid,
        isSubscribed = true,
    )
}
