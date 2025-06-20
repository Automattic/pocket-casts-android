package au.com.shiftyjelly.pocketcasts.transcripts

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.transcripts.ui.TranscriptPage
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@AndroidEntryPoint
class TranscriptFragment : BaseDialogFragment() {
    companion object {
        const val NEW_INSTANCE_KEY = "new_instance_key"

        fun newInstance(
            episodeUuid: String,
        ) = TranscriptFragment().apply {
            arguments = bundleOf(NEW_INSTANCE_KEY to Args(episodeUuid))
        }
    }

    private val args
        get() = requireNotNull(BundleCompat.getParcelable(requireArguments(), NEW_INSTANCE_KEY, Args::class.java)) {
            "Missing input parameters"
        }

    private val transcriptViewModel by viewModels<TranscriptViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        CallOnce {
            transcriptViewModel.loadTranscript(args.episodeUuid)
        }

        AppTheme(theme.activeTheme) {
            val uiState by transcriptViewModel.uiState.collectAsState()

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxHeight(0.93f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            ) {
                TranscriptPage(
                    uiState = uiState,
                    toolbarPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp),
                    paywallPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    transcriptPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
                    onClickClose = {
                        transcriptViewModel.hideSearch()
                        dismiss()
                    },
                    onClickReload = transcriptViewModel::reloadTranscript,
                    onUpdateSearchTerm = transcriptViewModel::searchInTranscript,
                    onClearSearchTerm = transcriptViewModel::clearSearch,
                    onSelectPreviousSearch = transcriptViewModel::selectPreviousSearchMatch,
                    onSelectNextSearch = transcriptViewModel::selectNextSearchMatch,
                    onShowSearchBar = transcriptViewModel::openSearch,
                    onHideSearchBar = transcriptViewModel::hideSearch,
                    onClickSubscribe = {
                        transcriptViewModel.track(AnalyticsEvent.TRANSCRIPT_GENERATED_PAYWALL_SUBSCRIBE_TAPPED)
                        OnboardingLauncher.openOnboardingFlow(requireActivity(), OnboardingFlow.Upsell(OnboardingUpgradeSource.GENERATED_TRANSCRIPTS))
                    },
                    onShowTranscript = { transcript ->
                        val properties = mapOf(
                            "type" to transcript.type.analyticsValue,
                            "show_as_webpage" to (transcript is Transcript.Web),
                        )
                        transcriptViewModel.track(AnalyticsEvent.TRANSCRIPT_SHOWN, properties)
                    },
                    onShowTransciptPaywall = {
                        transcriptViewModel.track(AnalyticsEvent.TRANSCRIPT_GENERATED_PAYWALL_SHOWN)
                    },
                    modifier = Modifier.fillMaxWidth(fraction = ResourcesCompat.getFloat(resources, UR.dimen.seekbar_width_percentage)),
                )
            }
        }
    }

    @Parcelize
    private class Args(
        val episodeUuid: String,
    ) : Parcelable
}
