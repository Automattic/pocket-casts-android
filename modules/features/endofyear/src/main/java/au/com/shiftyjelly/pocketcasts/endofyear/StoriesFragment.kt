package au.com.shiftyjelly.pocketcasts.endofyear

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseAppCompatDialogFragment

class StoriesFragment : BaseAppCompatDialogFragment() {
    private val viewModel: StoriesViewModel by viewModels()
    override val statusBarColor: StatusBarColor
        get() = StatusBarColor.Custom(Color.BLACK, true)

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        val isTablet = Util.isTablet(requireContext())
        if (!isTablet) {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            setStyle(STYLE_NORMAL, android.R.style.Theme_Material_NoActionBar)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme(theme.activeTheme) {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    StoriesPage(
                        viewModel = viewModel,
                        onCloseClicked = { dismiss() },
                    )
                }
            }
        }
    }

    companion object {
        fun newInstance() = StoriesFragment()
    }
}
