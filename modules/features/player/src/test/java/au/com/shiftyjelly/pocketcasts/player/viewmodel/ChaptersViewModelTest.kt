package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionPlatform
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionType
import au.com.shiftyjelly.pocketcasts.player.view.chapters.ChaptersViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import com.jakewharton.rxrelay2.BehaviorRelay
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ChaptersViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Mock
    private lateinit var playbackManager: PlaybackManager

    @Mock
    private lateinit var settings: Settings

    private val freeSubscriptionStatus = SubscriptionStatus.Free()

    private val paidSubscriptionStatus = SubscriptionStatus.Paid(
        expiry = Date(),
        autoRenew = false,
        index = 0,
        platform = SubscriptionPlatform.GIFT,
        tier = SubscriptionTier.PATRON,
        type = SubscriptionType.PLUS,
    )
    private val cachedSubscriptionStatusFlow: MutableStateFlow<SubscriptionStatus> = MutableStateFlow(freeSubscriptionStatus)

    private lateinit var chaptersViewModel: ChaptersViewModel

    private val chaptersTwoSelectedOneUnselected = Chapters(
        listOf(
            Chapter("1", 0.milliseconds, 100.milliseconds, selected = true),
            Chapter("2", 101.milliseconds, 200.milliseconds, selected = false),
            Chapter("3", 201.milliseconds, 300.milliseconds, selected = true),
        ),
    )

    private val chaptersOneSelectedOneUnselected = Chapters(
        listOf(
            Chapter("1", 0.milliseconds, 100.milliseconds, selected = true),
            Chapter("2", 101.milliseconds, 200.milliseconds, selected = false),
        ),
    )

    @Test
    fun `given unselected chapter contains playback pos, then skip to next selected chapter`() = runTest {
        val chapters = initChapters()
        initViewModel()

        chaptersViewModel.buildChaptersWithState(chapters, 150)

        verify(playbackManager).skipToNextSelectedOrLastChapter()
    }

    @Test
    fun `given selected chapter contains playback pos, then do not skip to next selected chapter`() = runTest {
        val chapters = initChapters()
        initViewModel()

        chaptersViewModel.buildChaptersWithState(chapters, 50)

        verify(playbackManager, never()).skipToNextSelectedOrLastChapter()
    }

    @Test
    fun `given user seeking playback pos, then do not skip to next selected chapter`() = runTest {
        val chapters = initChapters()
        initViewModel()

        chaptersViewModel.buildChaptersWithState(chapters, 150, lastChangeFrom = PlaybackManager.LastChangeFrom.OnUserSeeking.value)

        verify(playbackManager, never()).skipToNextSelectedOrLastChapter()
    }

    @Test
    fun `given seek complete, then do not skip to next selected chapter`() = runTest {
        val chapters = initChapters()
        initViewModel()

        chaptersViewModel.buildChaptersWithState(chapters, 150, lastChangeFrom = PlaybackManager.LastChangeFrom.OnSeekComplete.value)

        verify(playbackManager, never()).skipToNextSelectedOrLastChapter()
    }

    @Test
    fun `given feature flag off, then chapter is not skipped`() = runTest {
        FeatureFlag.setEnabled(Feature.DESELECT_CHAPTERS, false)
        val chapters = initChapters()
        initViewModel()

        chaptersViewModel.buildChaptersWithState(chapters, 150)

        verify(playbackManager, never()).skipToNextSelectedOrLastChapter()
    }

    @Test
    fun `given user not entitled to feature, when user taps skip chapters, then upsell starts`() = runTest {
        initViewModel()

        chaptersViewModel.uiState.test {
            assertFalse(awaitItem().canSkipChapters)
            chaptersViewModel.navigationState.test {
                chaptersViewModel.onSkipChaptersClick(true)
                assertTrue(awaitItem() is ChaptersViewModel.NavigationState.StartUpsell)
            }
        }
    }

    @Test
    fun `given user entitled to feature, when user taps skip chapters, then upsell does not start`() = runTest {
        initViewModel()

        chaptersViewModel.uiState.test {
            assertFalse(awaitItem().canSkipChapters)
            cachedSubscriptionStatusFlow.value = paidSubscriptionStatus
            assertTrue(awaitItem().canSkipChapters)
            chaptersViewModel.navigationState.test {
                chaptersViewModel.onSkipChaptersClick(true)
                expectNoEvents()
            }
        }
    }

    @Test
    fun `given only one chapter unselected, when chapter is unselected, then select at least one chapter message shown`() = runTest {
        val chapters = initChapters(chaptersOneSelectedOneUnselected)
        initViewModel(chapters = chapters)

        chaptersViewModel.uiState.test {
            cachedSubscriptionStatusFlow.value = paidSubscriptionStatus
            skipItems(2)
            chaptersViewModel.snackbarMessage.test {
                chaptersViewModel.onSelectionChange(false, chapters.getList().first { it.selected })
                assertTrue(awaitItem() == LR.string.select_one_chapter_message)
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `given more than one chapter unselected, when chapter is unselected, then select at least one chapter message not shown`() = runTest {
        val chapters = initChapters(chaptersTwoSelectedOneUnselected)
        initViewModel(chapters = chapters)

        chaptersViewModel.uiState.test {
            cachedSubscriptionStatusFlow.value = paidSubscriptionStatus
            skipItems(2)
            chaptersViewModel.snackbarMessage.test {
                chaptersViewModel.onSelectionChange(false, chapters.getList().first { it.selected })
                expectNoEvents()
            }
        }
    }

    private fun initChapters(chapters: Chapters = chaptersTwoSelectedOneUnselected) = chapters

    private fun initViewModel(
        chapters: Chapters = Chapters(),
    ) {
        whenever(playbackManager.playbackStateRelay)
            .thenReturn(BehaviorRelay.create<PlaybackState>().toSerialized().apply { accept(PlaybackState(chapters = chapters)) })
        whenever(settings.userTier)
            .thenReturn(UserTier.Free)

        val userSetting = mock<UserSetting<SubscriptionStatus?>>()
        whenever(userSetting.flow).thenReturn(cachedSubscriptionStatusFlow)
        whenever(settings.cachedSubscriptionStatus).thenReturn(userSetting)

        chaptersViewModel = ChaptersViewModel(
            playbackManager = playbackManager,
            settings = settings,
            analyticsTracker = mock(),
        )
    }
}
