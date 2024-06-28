package au.com.shiftyjelly.pocketcasts.settings.developer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import com.airbnb.android.showkase.ui.ShowkaseBrowserActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class DeveloperFragment : BaseFragment() {

    private val viewModel: DeveloperViewModel by viewModels()

    @Inject lateinit var settings: Settings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeWithBackground(theme.activeTheme) {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(0)
                    DeveloperPage(
                        onBackClick = ::onBackClick,
                        onShowkaseClick = ::onShowkaseClick,
                        onForceRefreshClick = viewModel::forceRefresh,
                        onTriggerNotificationClick = viewModel::triggerNotification,
                        onDeleteFirstEpisodeClick = viewModel::deleteFirstEpisode,
                        onTriggerUpdateEpisodeDetails = viewModel::triggerUpdateEpisodeDetails,
                        onTriggerResetEoYModalProfileBadge = viewModel::resetEoYModalProfileBadge,
                        bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
                        onSendCrash = viewModel::onSendCrash,
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun onBackClick() {
        activity?.onBackPressed()
    }

    private fun onShowkaseClick() {
        val intent = Intent(context, ShowkaseBrowserActivity::class.java).apply {
            putExtra("SHOWKASE_ROOT_MODULE", "au.com.shiftyjelly.pocketcasts.showkase.AppShowkaseRootModule")
        }
        startActivity(intent)
    }
}
