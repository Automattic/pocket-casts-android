package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.BadgeType
import au.com.shiftyjelly.pocketcasts.preferences.model.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SuggestedFoldersManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EventHorizon
import io.reactivex.Flowable
import java.time.Instant
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class PodcastsViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val folder = Folder(
        uuid = "folder",
        name = "Folder",
        color = 0,
        addedDate = Date(),
        sortPosition = 0,
        podcastsSortType = PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST,
        deleted = false,
        syncModified = 0,
    )
    private val unplayedPodcast = Podcast(uuid = "unplayed")
    private val playedPodcast = Podcast(uuid = "played")
    private val playedPodcastInFolder = Podcast(uuid = "played-in-folder", rawFolderUuid = folder.uuid)

    private val plusSubscription = Subscription(
        tier = SubscriptionTier.Plus,
        billingCycle = BillingCycle.Monthly,
        platform = SubscriptionPlatform.Android,
        expiryDate = Instant.now(),
        isAutoRenewing = true,
        giftDays = 0,
    )

    @Test
    fun `shows played podcasts when hiding is off`() = runTest {
        val viewModel = createViewModel(hidePlayedPodcasts = false, subscription = null)

        assertEquals(
            listOf(unplayedPodcast.uuid, playedPodcast.uuid, playedPodcastInFolder.uuid),
            viewModel.loadedItems().map(FolderItem::uuid),
        )
    }

    @Test
    fun `hides played podcasts for free users`() = runTest {
        val viewModel = createViewModel(hidePlayedPodcasts = true, subscription = null)

        assertEquals(listOf(unplayedPodcast.uuid), viewModel.loadedItems().map(FolderItem::uuid))
    }

    @Test
    fun `keeps folders whose podcasts are all hidden`() = runTest {
        val viewModel = createViewModel(hidePlayedPodcasts = true, subscription = plusSubscription)

        val items = viewModel.loadedItems()

        assertEquals(listOf(unplayedPodcast.uuid, folder.uuid), items.map(FolderItem::uuid))
        assertEquals(emptyList<Podcast>(), (items.last() as FolderItem.Folder).podcasts)
    }

    @Test
    fun `hides played podcasts inside a folder`() = runTest {
        val viewModel = createViewModel(hidePlayedPodcasts = true, subscription = plusSubscription, folderUuid = folder.uuid)

        assertEquals(emptyList<FolderItem>(), viewModel.loadedItems())
    }

    private suspend fun PodcastsViewModel.loadedItems(): List<FolderItem> {
        return uiState.first { !it.isLoadingItems }.items
    }

    private fun createViewModel(
        hidePlayedPodcasts: Boolean,
        subscription: Subscription?,
        folderUuid: String? = null,
    ): PodcastsViewModel {
        val settings = mock<Settings> {
            on { podcastsSortType } doReturn UserSetting.Mock(PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST, mock())
            on { this.hidePlayedPodcasts } doReturn UserSetting.Mock(hidePlayedPodcasts, mock())
            on { notificationsPromptAcknowledged } doReturn UserSetting.Mock(true, mock())
            on { podcastBadgeType } doReturn UserSetting.Mock(BadgeType.OFF, mock())
            on { podcastGridLayout } doReturn UserSetting.Mock(PodcastGridLayoutType.LARGE_ARTWORK, mock())
            on { refreshStateFlow } doReturn MutableStateFlow(RefreshState.Never)
        }
        val podcastManager = mock<PodcastManager> {
            on { observePodcastsSortedByLatestEpisode() } doReturn flowOf(listOf(unplayedPodcast, playedPodcast, playedPodcastInFolder))
            on { observePodcastsSortedByUserChoice(any()) } doReturn flowOf(listOf(playedPodcastInFolder))
            on { observePlayedPodcastUuids() } doReturn flowOf(setOf(playedPodcast.uuid, playedPodcastInFolder.uuid))
        }
        val folderManager = mock<FolderManager> {
            on { observeFolders() } doReturn flowOf(listOf(folder))
        }
        val userManager = mock<UserManager> {
            on { getSignInState() } doReturn Flowable.just(SignInState.SignedIn(email = "", subscription = subscription))
        }
        val suggestedFoldersManager = mock<SuggestedFoldersManager> {
            on { observeSuggestedFolders() } doReturn flowOf(emptyList())
        }
        return PodcastsViewModel(
            podcastManager = podcastManager,
            episodeManager = mock(),
            folderManager = folderManager,
            settings = settings,
            eventHorizon = EventHorizon(TestEventSink()),
            suggestedFoldersManager = suggestedFoldersManager,
            suggestedFoldersPopupPolicy = mock(),
            userManager = userManager,
            notificationHelper = mock(),
            blazeAdsManager = mock { on { findPodcastListAd() } doReturn flowOf(null) },
            folderUuid = folderUuid,
        )
    }
}
