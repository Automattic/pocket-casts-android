package au.com.shiftyjelly.pocketcasts.settings.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MediaActionsFragment : BaseFragment() {
    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var settings: Settings
    private val viewModel: MediaActionsViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext()).apply {
        setContent {
            val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(0)
            val state by viewModel.state.collectAsStateWithLifecycle(MediaActionsViewModel.State())
            AppThemeWithBackground(theme.activeTheme) {
                // val state: MediaActionsViewModel.State by viewModel.state.collectAsState()
                val context = LocalContext.current
                MediaActionsPage(
                    state = state,
                    onShowCustomActionsChanged = viewModel::setShowCustomActionsChanged,
                    onActionsOrderChanged = viewModel::onActionsOrderChanged,
                    onBackClick = ::onBackPressed,
//                    onRetryClick = { viewModel.loadStats() },
//                    launchReviewDialog = { viewModel.launchAppReviewDialog(it) },
                    bottomInset = bottomInset.value.pxToDp(context).dp,
                )
            }
        }
    }

//    override fun onResume() {
//        super.onResume()
//
//        viewModel.loadStats()
//    }

//    override fun onBackPressed(): Boolean {
//        analyticsTracker.track(AnalyticsEvent.STATS_DISMISSED)
//        return super.onBackPressed()
//    }
}
