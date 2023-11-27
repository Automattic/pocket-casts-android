package au.com.shiftyjelly.pocketcasts.ui

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlagWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersionWrapper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class MainActivityViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    lateinit var playbackManager: PlaybackManager

    @Mock
    lateinit var userManager: UserManager

    @Mock
    lateinit var settings: Settings

    @Mock
    lateinit var endOfYearManager: EndOfYearManager

    @Mock
    lateinit var multiSelectBookmarksHelper: MultiSelectBookmarksHelper

    @Mock
    lateinit var podcastManager: PodcastManager

    @Mock
    lateinit var bookmarkManager: BookmarkManager

    @Mock
    lateinit var theme: Theme

    private lateinit var viewModel: MainActivityViewModel

    private val betaEarlyAccessRelease = ReleaseVersion(7, 50, null, 1)
    private val productionEarlyAccessRelease = ReleaseVersion(7, 50)
    private val betaFullAccessRelease = ReleaseVersion(7, 51, null, 1)
    private val productionFullAccessRelease = ReleaseVersion(7, 51)

    @Before
    fun setup() = runTest {
        whenever(playbackManager.playbackStateRelay).thenReturn(BehaviorRelay.create<PlaybackState>().toSerialized())
    }

    @Test
    fun `given user entitled for bookmarks, when any release, then what's new shown`() = runTest {
        initViewModel(
            isUserEntitled = true,
            currentRelease = mock(),
            patronExclusiveAccessRelease = mock(),
        )

        viewModel.state.test {
            assertTrue(awaitItem().shouldShowWhatsNew)
        }
    }

    @Test
    fun `given user not entitled for bookmarks, when beta early access release, then what's new not shown`() = runTest {
        initViewModel(
            isUserEntitled = false,
            currentRelease = betaEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        viewModel.state.test {
            assertFalse(awaitItem().shouldShowWhatsNew)
        }
    }

    @Test
    fun `given user not entitled for bookmarks, when prod early access release, then what's new not shown`() = runTest {
        initViewModel(
            isUserEntitled = false,
            currentRelease = productionEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        viewModel.state.test {
            assertFalse(awaitItem().shouldShowWhatsNew)
        }
    }

    @Test
    fun `given user not entitled for bookmarks, when beta full access release, then what's new shown`() = runTest {
        initViewModel(
            isUserEntitled = false,
            currentRelease = betaFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        viewModel.state.test {
            assertTrue(awaitItem().shouldShowWhatsNew)
        }
    }

    @Test
    fun `given user not entitled for bookmarks, when prod full access release, then what's new shown`() = runTest {
        initViewModel(
            isUserEntitled = false,
            currentRelease = productionFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        viewModel.state.test {
            assertTrue(awaitItem().shouldShowWhatsNew)
        }
    }

    private fun initViewModel(
        isUserEntitled: Boolean,
        currentRelease: ReleaseVersion,
        patronExclusiveAccessRelease: ReleaseVersion?,
    ) {
        whenever(settings.getMigratedVersionCode()).thenReturn(1) // this is not 0, we don't show what's new to new users
        whenever(settings.getWhatsNewVersionCode()).thenReturn(2) // this is less than the current what's new version code and so will trigger what's new

        whenever(userManager.getSignInState()).thenReturn(
            Flowable.just(
                SignInState.SignedIn(
                    email = "",
                    subscriptionStatus = mock<SubscriptionStatus>()
                )
            )
        )

        val releaseVersion = mock<ReleaseVersionWrapper>().apply {
            doReturn(currentRelease).whenever(this).currentReleaseVersion
        }
        val bookmarksFeature = mock<Feature>().apply {
            doReturn(FeatureTier.Plus(patronExclusiveAccessRelease)).whenever(this).tier
        }
        val feature = mock<FeatureWrapper>().apply {
            doReturn(bookmarksFeature).whenever(this).bookmarksFeature
            doReturn(isUserEntitled).whenever(this).isUserEntitled(anyOrNull(), anyOrNull())
        }

        val featureFlag = mock<FeatureFlagWrapper>()
        whenever(featureFlag.isEnabled(feature.bookmarksFeature)).thenReturn(true)

        viewModel = MainActivityViewModel(
            playbackManager = playbackManager,
            userManager = userManager,
            settings = settings,
            endOfYearManager = endOfYearManager,
            multiSelectBookmarksHelper = multiSelectBookmarksHelper,
            podcastManager = podcastManager,
            bookmarkManager = bookmarkManager,
            theme = theme,
            feature = feature,
            featureFlag = featureFlag,
            releaseVersion = releaseVersion
        )
    }
}
