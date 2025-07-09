package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AutoArchiveFragmentViewModel
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AutoArchiveFragment : BaseFragment() {

    @Inject
    lateinit var settings: Settings

    private val viewModel: AutoArchiveFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        CallOnce {
            viewModel.trackOnViewShownEvent()
        }

        val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(initialValue = 0)

        AppThemeWithBackground(theme.activeTheme) {
            AutoArchiveSettingsPage(
                viewModel = viewModel,
                bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
                onBackPress = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
            )
        }
    }
}
