package au.com.shiftyjelly.pocketcasts.endofyear

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.R
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment

class StoriesFragment : BaseDialogFragment() {
    private val viewModel: StoriesViewModel by viewModels()
    override val statusBarColor: StatusBarColor
        get() = StatusBarColor.Custom(Color.BLACK, true)

    override fun onCreate(savedInstance: Bundle?) {
        super.onCreate(savedInstance)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialogThemeBlack)
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
                    StoriesScreen(
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
