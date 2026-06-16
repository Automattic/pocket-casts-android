package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.summary.SummaryContent
import au.com.shiftyjelly.pocketcasts.compose.summary.SummaryPaywall
import au.com.shiftyjelly.pocketcasts.compose.summary.SummaryPaywallColors
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.SummaryViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.SummaryViewModel.SummaryState
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SummaryFragment : BaseFragment() {

    private val playerViewModel: PlayerViewModel by activityViewModels()
    private val summaryViewModel: SummaryViewModel by viewModels({ requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val state by summaryViewModel.state.collectAsStateWithLifecycle()
        val podcast by playerViewModel.podcastFlow.collectAsStateWithLifecycle()
        val backgroundColor = Color(theme.playerBackgroundColor(podcast))

        AppThemeWithBackground(Theme.ThemeType.DARK) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor),
            ) {
                when (val s = state) {
                    is SummaryState.Loading -> {
                        CircularProgressIndicator(
                            color = MaterialTheme.theme.colors.playerContrast01,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }

                    is SummaryState.Loaded -> {
                        SummaryContent(
                            text = s.text,
                            titleColor = MaterialTheme.theme.colors.playerContrast01,
                            textColor = MaterialTheme.theme.colors.playerContrast01,
                            scrollBarColor = MaterialTheme.theme.colors.playerContrast02,
                            modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection()),
                        )
                    }

                    is SummaryState.Upsell -> {
                        SummaryPaywall(
                            summaryText = s.text,
                            isFreeTrialAvailable = s.isFreeTrialAvailable,
                            onClickSubscribe = ::openUpgradeFlow,
                            colors = SummaryPaywallColors.player(
                                background = backgroundColor,
                                contrast01 = MaterialTheme.theme.colors.playerContrast01,
                                contrast02 = MaterialTheme.theme.colors.playerContrast02,
                            ),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 24.dp),
                            modifier = Modifier.fillMaxSize(),
                        )
                    }

                    is SummaryState.NotAvailable -> Unit
                }
            }
        }
    }

    private fun openUpgradeFlow() {
        OnboardingLauncher.openOnboardingFlow(
            requireActivity(),
            OnboardingFlow.Upsell(OnboardingUpgradeSource.AI_SUMMARIES),
        )
    }
}
