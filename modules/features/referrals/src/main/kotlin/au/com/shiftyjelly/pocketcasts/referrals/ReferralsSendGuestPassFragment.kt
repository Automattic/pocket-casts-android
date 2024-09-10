package au.com.shiftyjelly.pocketcasts.referrals

import android.graphics.Color
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.graphics.toArgb
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.ui.helper.StatusBarColor
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil.setBackgroundColor
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.parcelize.Parcelize
import androidx.compose.ui.graphics.Color as ComposeColor

private const val NEW_INSTANCE_ARG = "ReferralsSendPassFragmentArgs"

@AndroidEntryPoint
class ReferralsSendGuestPassFragment : BaseFragment() {
    private val args get() = requireNotNull(arguments?.let { BundleCompat.getParcelable(it, NEW_INSTANCE_ARG, Args::class.java) })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        setBackgroundColor(view, ComposeColor.Transparent.toArgb())
        ReferralsSendGuestPassPage(
            passCount = args.passCount,
            onDismiss = {
                (activity as? FragmentHostListener)?.bottomSheetClosePressed(this)
            },
        )
    }

    override fun onResume() {
        super.onResume()
        updateStatusAndNavColors()
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

    @Parcelize
    private class Args(
        val passCount: Int,
    ) : Parcelable

    companion object {
        fun newInstance(
            passCount: Int,
        ) = ReferralsSendGuestPassFragment().apply {
            arguments = bundleOf(
                NEW_INSTANCE_ARG to Args(passCount),
            )
        }
    }
}
