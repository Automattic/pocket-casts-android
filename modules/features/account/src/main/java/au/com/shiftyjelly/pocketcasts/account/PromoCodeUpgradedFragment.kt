package au.com.shiftyjelly.pocketcasts.account

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint

private const val ARG_DESCRIPTION = "description"

@AndroidEntryPoint
class PromoCodeUpgradedFragment : BaseDialogFragment() {
    companion object {
        fun newInstance(codeDescription: String): PromoCodeUpgradedFragment {
            val instance = PromoCodeUpgradedFragment()
            instance.arguments = Bundle().apply {
                putString(ARG_DESCRIPTION, codeDescription)
            }
            return instance
        }
    }

    override val includeNavigationBarPadding = false

    val codeDescription: String
        get() = arguments?.getString(ARG_DESCRIPTION) ?: ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            PromoCodeUpgradedPage(
                codeDescription = codeDescription,
                logoRes = theme.verticalPlusLogoRes(),
                onDoneClick = ::dismiss,
                modifier = Modifier.windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout).only(WindowInsetsSides.Bottom)),
            )
        }
    }
}
