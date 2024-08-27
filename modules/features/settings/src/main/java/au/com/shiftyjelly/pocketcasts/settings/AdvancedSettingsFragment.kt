package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.fragment.compose.content
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AdvancedSettingsViewModel
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AdvancedSettingsFragment : BaseFragment() {
    @Inject lateinit var setting: Settings
    private val viewModel: AdvancedSettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val bottomInset = setting.bottomInset.collectAsStateWithLifecycle(initialValue = 0)
        AppThemeWithBackground(theme.activeTheme) {
            AdvancedSettingsPage(
                viewModel = viewModel,
                onBackPressed = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
                bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
            )
        }
    }
}
