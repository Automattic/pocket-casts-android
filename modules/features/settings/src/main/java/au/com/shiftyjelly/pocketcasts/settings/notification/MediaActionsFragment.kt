package au.com.shiftyjelly.pocketcasts.settings.notification

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        CallOnce {
            analyticsTracker.track(AnalyticsEvent.SETTINGS_GENERAL_MEDIA_NOTIFICATION_CONTROLS_SHOWN)
        }

        val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(0)
        val state by viewModel.state.collectAsStateWithLifecycle()

        AppThemeWithBackground(theme.activeTheme) {
            val context = LocalContext.current
            MediaActionsPage(
                state = state,
                onShowCustomActionsChange = viewModel::setShowCustomActionsChanged,
                onNextPreviousTrackSkipButtonsChange = viewModel::setNextPreviousTrackSkipButtonsChanged,
                onActionsOrderChange = viewModel::onActionsOrderChanged,
                onActionMove = viewModel::onActionMoved,
                onBackPress = ::onBackPress,
                bottomInset = bottomInset.value.pxToDp(context).dp,
            )
        }
    }

    fun onBackPress() {
        (activity as? FragmentHostListener)?.closeModal(this)
    }
}
