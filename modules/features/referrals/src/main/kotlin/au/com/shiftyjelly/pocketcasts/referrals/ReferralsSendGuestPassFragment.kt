package au.com.shiftyjelly.pocketcasts.referrals

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.getActivity
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil.setBackgroundColor
import dagger.hilt.android.AndroidEntryPoint
import androidx.compose.ui.graphics.Color as ComposeColor

@AndroidEntryPoint
class ReferralsSendGuestPassFragment : BaseFragment() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        val context = LocalContext.current
        val windowSize = calculateWindowSizeClass(context.getActivity() as Activity)

        setBackgroundColor(view, ComposeColor.Transparent.toArgb())
        ReferralsSendGuestPassPage(
            onDismiss = {
                (activity as? FragmentHostListener)?.bottomSheetClosePressed(this)
            },
        )

        LaunchedEffect(Unit) {
            if (windowSize.widthSizeClass == WindowWidthSizeClass.Compact ||
                windowSize.heightSizeClass == WindowHeightSizeClass.Compact
            ) {
                updateStatusAndNavColors()
            }
        }
    }

    private fun updateStatusAndNavColors() {
        activity?.let {
            theme.setNavigationBarColor(it.window, true, Color.BLACK)
            theme.updateWindowStatusBar(
                it.window,
                StatusBarColor.Custom(Color.BLACK, true),
                it,
            )
        }
    }

    companion object {
        fun newInstance() = ReferralsSendGuestPassFragment()
    }
}
