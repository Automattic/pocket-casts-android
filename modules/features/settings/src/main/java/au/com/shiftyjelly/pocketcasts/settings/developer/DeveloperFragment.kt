package au.com.shiftyjelly.pocketcasts.settings.developer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import com.airbnb.android.showkase.ui.ShowkaseBrowserActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeveloperFragment : BaseFragment() {

    private val viewModel: DeveloperViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppThemeWithBackground(theme.activeTheme) {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    DeveloperPage(
                        onBackClick = ::onBackClick,
                        onShowkaseClick = ::onShowkaseClick,
                        onForceRefreshClick = viewModel::forceRefresh,
                        onTriggerNotificationClick = viewModel::triggerNotification,
                        onDeleteFirstEpisodeClick = viewModel::deleteFirstEpisode,
                        onTriggerUpdateEpisodeDetails = viewModel::triggerUpdateEpisodeDetails
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
