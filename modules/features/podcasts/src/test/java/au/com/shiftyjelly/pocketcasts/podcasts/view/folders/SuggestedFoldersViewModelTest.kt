package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SuggestedFoldersManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import au.com.shiftyjelly.pocketcasts.models.entity.SuggestedFolder as DbSuggestedFolder

@ExperimentalCoroutinesApi
class SuggestedFoldersViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: SuggestedFoldersViewModel

    @Mock
    private lateinit var folderManager: FolderManager

    @Mock
    private lateinit var suggestedFoldersManager: SuggestedFoldersManager

    @Mock
    private lateinit var suggestedFoldersPopupPolicy: SuggestedFoldersPopupPolicy

    @Mock
    private lateinit var podcastManager: PodcastManager

    @Mock
    private lateinit var userManager: UserManager

    @Mock
    private lateinit var analyticsTracker: AnalyticsTracker

    @Mock
    private lateinit var source: SuggestedFoldersFragment.Source

    private val folderCount = 1
    private val dbSuggestedFolders = listOf(DbSuggestedFolder("Folder1", "podcast1"))

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun `popup was marked as dismissed`() = runTest {
        initViewModel()

        viewModel.markPopupAsDismissed()

        verify(suggestedFoldersPopupPolicy).markPolicyUsed()
    }

    @Test
    fun `should track page shown`() = runTest {
        initViewModel()

        viewModel.trackPageShown()

        verify(analyticsTracker).track(
            AnalyticsEvent.SUGGESTED_FOLDERS_PAGE_SHOWN,
            mapOf(
                "source" to source.analyticsValue,
            ),
        )
    }

    @Test
    fun `should track suggested folders tapped`() = runTest {
        initViewModel()

        viewModel.trackUseSuggestedFoldersTapped()

        verify(analyticsTracker).track(
            AnalyticsEvent.SUGGESTED_FOLDERS_USE_SUGGESTED_FOLDERS_TAPPED,
            mapOf(
                "source" to source.analyticsValue,
                "user_type" to "free",
            ),
        )
    }

    @Test
    fun `should track custom create folders tapped`() = runTest {
        initViewModel()

        viewModel.trackCreateCustomFolderTapped()

        verify(analyticsTracker).track(
            AnalyticsEvent.SUGGESTED_FOLDERS_CREATE_CUSTOM_FOLDER_TAPPED,
            mapOf(
                "source" to source.analyticsValue,
                "user_type" to "free",
            ),
        )
    }

    @Test
    fun `should track replace folders tapped`() = runTest {
        initViewModel()

        viewModel.trackReplaceFolderTapped()

        verify(analyticsTracker).track(
            AnalyticsEvent.SUGGESTED_FOLDERS_REPLACE_FOLDERS_TAPPED,
            mapOf(
                "source" to source.analyticsValue,
            ),
        )
    }

    @Test
    fun `should track folders preview tapped`() = runTest {
        initViewModel()

        val folder = SuggestedFolder("Folder1", 0, listOf("podcast1"))

        viewModel.trackPreviewFolderTapped(folder)

        verify(analyticsTracker).track(
            AnalyticsEvent.SUGGESTED_FOLDERS_PREVIEW_FOLDER_TAPPED,
            mapOf(
                "source" to source.analyticsValue,
                "folder_name" to folder.name,
                "podcast_count" to folder.podcastIds.size,
            ),
        )
    }

    private suspend fun initViewModel() {
        whenever(folderManager.countFolders()).thenReturn(folderCount)
        whenever(suggestedFoldersManager.observeSuggestedFolders()).thenReturn(flowOf(dbSuggestedFolders))

        whenever(userManager.getSignInState()).thenReturn(
            Flowable.just(
                SignInState.SignedIn(
                    email = "",
                    subscriptionStatus = SubscriptionStatus.Free(),
                ),
            ),
        )

        viewModel = SuggestedFoldersViewModel(
            source = source,
            folderManager = folderManager,
            suggestedFoldersManager = suggestedFoldersManager,
            suggestedFoldersPopupPolicy = suggestedFoldersPopupPolicy,
            podcastManager = podcastManager,
            userManager = userManager,
            analyticsTracker = analyticsTracker,
        )
    }
}
