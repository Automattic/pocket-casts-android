package au.com.shiftyjelly.pocketcasts.transcripts

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.components.AnimatedPlayPauseButton
import au.com.shiftyjelly.pocketcasts.compose.components.rememberViewInteropNestedScrollConnection
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.transcripts.ui.ToolbarColors
import au.com.shiftyjelly.pocketcasts.transcripts.ui.TranscriptPage
import au.com.shiftyjelly.pocketcasts.transcripts.ui.TranscriptShareButton
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireParcelable
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.withCreationCallback
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class TranscriptFragment : BaseDialogFragment() {
    companion object {
        const val NEW_INSTANCE_KEY = "new_instance_key"

        fun newInstance(
            episodeUuid: String,
            podcastUuid: String?,
        ) = TranscriptFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_KEY to Args(episodeUuid, podcastUuid))
        }
    }

    private val args get() = requireArguments().requireParcelable<Args>(NEW_INSTANCE_KEY)

    private val viewModel by viewModels<TranscriptViewModel>(
        ownerProducer = { requireParentFragment() },
        extrasProducer = {
            defaultViewModelCreationExtras.withCreationCallback<TranscriptViewModel.Factory> { factory ->
                val viewModel = factory.create(TranscriptViewModel.Source.Episode)
                viewModel.loadTranscript(args.episodeUuid)
                viewModel
            }
        },
    )

    @Inject
    internal lateinit var playbackManger: PlaybackManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        CallOnce {
            viewModel.track(
                AnalyticsEvent.EPISODE_TRANSCRIPT_SHOWN,
                buildMap {
                    put("episode_uuid", args.episodeUuid)
                    args.podcastUuid?.let { uuid -> put("podcast_uuid", uuid) }
                },
            )
        }

        DialogBox {
            val uiState by viewModel.uiState.collectAsState()

            TranscriptPage(
                uiState = uiState,
                toolbarPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp),
                paywallPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                transcriptPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
                onClickClose = {
                    viewModel.hideSearch()
                    dismiss()
                },
                onClickReload = viewModel::reloadTranscript,
                onUpdateSearchTerm = viewModel::searchInTranscript,
                onClearSearchTerm = viewModel::clearSearch,
                onSelectPreviousSearch = viewModel::selectPreviousSearchMatch,
                onSelectNextSearch = viewModel::selectNextSearchMatch,
                onShowSearchBar = viewModel::openSearch,
                onHideSearchBar = viewModel::hideSearch,
                onClickSubscribe = {
                    viewModel.track(AnalyticsEvent.TRANSCRIPT_GENERATED_PAYWALL_SUBSCRIBE_TAPPED)
                    OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.Upsell(OnboardingUpgradeSource.GENERATED_TRANSCRIPTS))
                },
                onShowTranscript = { transcript ->
                    val properties = mapOf(
                        "type" to transcript.type.analyticsValue,
                        "show_as_webpage" to (transcript is Transcript.Web),
                    )
                    viewModel.track(AnalyticsEvent.TRANSCRIPT_SHOWN, properties)
                },
                onShowTranscriptPaywall = {
                    viewModel.track(AnalyticsEvent.TRANSCRIPT_GENERATED_PAYWALL_SHOWN)
                },
                toolbarTrailingContent = { toolbarColors ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (uiState.isTextTranscriptLoaded && FeatureFlag.isEnabled(Feature.SHARE_TRANSCRIPTS)) {
                            TranscriptShareButton(
                                toolbarColors = toolbarColors,
                                onClick = viewModel::shareTranscript,
                            )
                        }
                        PlayPauseButton(toolbarColors)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(fraction = ResourcesCompat.getFloat(resources, UR.dimen.player_max_content_width_fraction))
                    .nestedScroll(rememberViewInteropNestedScrollConnection()),
            )
        }
    }

    @Composable
    private fun PlayPauseButton(
        toolbarColors: ToolbarColors,
        modifier: Modifier = Modifier,
    ) {
        val scope = rememberCoroutineScope()
        val playbackState by remember {
            playbackManger.playbackStateFlow.map { it.episodeUuid to it.isPlaying }
        }.collectAsState(initial = null)
        val isPlayingThisEpisode = playbackState?.first == args.episodeUuid && playbackState?.second == true

        AnimatedPlayPauseButton(
            isPlaying = isPlayingThisEpisode,
            onClick = {
                if (isPlayingThisEpisode) {
                    playbackManger.pause(sourceView = SourceView.EPISODE_TRANSCRIPT)
                } else {
                    scope.launch {
                        playbackManger.playNowSuspend(args.episodeUuid, sourceView = SourceView.EPISODE_TRANSCRIPT)
                    }
                }
            },
            iconWidth = 24.dp,
            iconHeight = 24.dp,
            circleSize = 48.dp,
            iconTint = toolbarColors.button,
            circleColor = toolbarColors.buttonBackground,
            modifier = modifier,
        )
    }

    @Parcelize
    private class Args(
        val episodeUuid: String,
        val podcastUuid: String?,
    ) : Parcelable
}
