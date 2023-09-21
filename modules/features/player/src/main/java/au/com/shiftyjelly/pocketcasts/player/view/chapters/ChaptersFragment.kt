package au.com.shiftyjelly.pocketcasts.player.view.chapters

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.fragment.app.activityViewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.player.view.PlayerContainerFragment
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class ChaptersFragment : BaseFragment() {

    @Inject lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private val chaptersViewModel: ChaptersViewModel by activityViewModels()
    private var lazyListState: LazyListState? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppTheme(Theme.ThemeType.DARK) {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

                val uiState by chaptersViewModel.uiState.subscribeAsState(chaptersViewModel.defaultUiState)
                val lazyListState = rememberLazyListState()
                this@ChaptersFragment.lazyListState = lazyListState

                val scrollToChapter by chaptersViewModel.scrollToChapterState.collectAsState()
                LaunchedEffect(scrollToChapter) {
                    scrollToChapter?.let {
                        delay(250)
                        lazyListState.animateScrollToItem(it.index - 1)

                        // Need to clear this so that if the user taps on the same chapter a second time we
                        // still get the scrollTo behavior.
                        chaptersViewModel.setScrollToChapter(null)
                    }
                }

                Surface(modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())) {
                    ChaptersPage(
                        lazyListState = lazyListState,
                        chapters = uiState.chapters,
                        onChapterClick = ::onChapterClick,
                        onUrlClick = ::onUrlClick,
                        backgroundColor = uiState.backgroundColor
                    )
                }
            }
        }
    }

    private fun onChapterClick(chapter: Chapter, isPlaying: Boolean) {
        analyticsTracker.track(AnalyticsEvent.PLAYER_CHAPTER_SELECTED)
        if (isPlaying) {
            showPlayer()
        } else {
            chaptersViewModel.skipToChapter(chapter)
        }
    }

    private fun onUrlClick(url: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            UiUtil.displayAlertError(requireContext(), getString(LR.string.player_open_url_failed, url), null)
        }
    }

    private fun showPlayer() {
        (parentFragment as? PlayerContainerFragment)?.openPlayer()
    }
}
