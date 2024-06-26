package au.com.shiftyjelly.pocketcasts.player.view.chapters

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.core.net.toUri
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.player.view.PlayerContainerFragment
import au.com.shiftyjelly.pocketcasts.player.view.chapters.ChaptersViewModel.Mode.Episode
import au.com.shiftyjelly.pocketcasts.player.view.chapters.ChaptersViewModel.Mode.Player
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import kotlinx.coroutines.delay
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class ChaptersFragment : BaseFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, Args::class.java) })

    private val mode get() = args.episodeId?.let(::Episode) ?: Player

    private val viewModel by viewModels<ChaptersViewModel>(
        ownerProducer = { requireParentFragment() },
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<ChaptersViewModel.Factory> { factory ->
                factory.create(mode)
            }
        },
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            val state by viewModel.uiState.collectAsState()

            AppThemeWithBackground(mode.themeType) {
                ChaptersTheme(state.podcast) {
                    val lazyListState = rememberLazyListState()

                    ChaptersPage(
                        lazyListState = lazyListState,
                        chapters = state.chapters,
                        showHeader = state.showHeader,
                        totalChaptersCount = state.chaptersCount,
                        isTogglingChapters = state.isTogglingChapters,
                        showSubscriptionIcon = state.showSubscriptionIcon,
                        onChapterClick = viewModel::playChapter,
                        onSkipChaptersClick = viewModel::enableTogglingOrUpsell,
                        onSelectionChange = viewModel::selectChapter,
                        onUrlClick = ::openChapterUrl,
                        modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
                    )

                    LaunchedEffect(Unit) {
                        viewModel.scrollToChapter.collect {
                            delay(250)
                            lazyListState.animateScrollToItem(it.index - 1)
                        }
                    }

                    LaunchedEffect(Unit) {
                        viewModel.showPlayer.collect {
                            showPlayer()
                        }
                    }

                    LaunchedEffect(Unit) {
                        viewModel.showUpsell.collect {
                            showUpsell()
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun ChaptersTheme(podcast: Podcast?, content: @Composable () -> Unit) {
        when (mode) {
            is Episode -> ChaptersTheme(content)
            is Player -> ChaptersThemeForPlayer(theme, podcast, content)
        }
    }

    private fun openChapterUrl(chapter: Chapter) {
        try {
            viewModel.trackChapterLinkTap(chapter)
            startActivity(Intent(Intent.ACTION_VIEW).setData(chapter.url.toString().toUri()))
        } catch (_: Throwable) {
            UiUtil.displayAlertError(requireContext(), getString(LR.string.player_open_url_failed, chapter.url.toString()), null)
        }
    }

    private fun showPlayer() {
        (parentFragment as? PlayerContainerFragment)?.openPlayer()
    }

    private fun showUpsell() {
        val source = OnboardingUpgradeSource.SKIP_CHAPTERS
        val onboardingFlow = OnboardingFlow.Upsell(
            source = source,
            showPatronOnly = Feature.DESELECT_CHAPTERS.tier == FeatureTier.Patron || Feature.DESELECT_CHAPTERS.isCurrentlyExclusiveToPatron(),
        )
        OnboardingLauncher.openOnboardingFlow(requireActivity(), onboardingFlow)
    }

    private val ChaptersViewModel.Mode.themeType get() = when (this) {
        is Player -> Theme.ThemeType.DARK
        is Episode -> theme.activeTheme
    }

    @Parcelize
    private class Args(
        val episodeId: String?,
    ) : Parcelable

    companion object {
        private const val NEW_INSTANCE_ARG = "ChaptersFragment2Arg"

        fun forEpisode(episodeUuid: String) = ChaptersFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARG to Args(episodeUuid))
        }

        fun forPlayer() = ChaptersFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_ARG to Args(episodeId = null))
        }
    }
}
