package au.com.shiftyjelly.pocketcasts.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.account.AccountActivity
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TrialFinishedFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        AppTheme(themeType = theme.activeTheme) {
            val context = LocalContext.current

            TrialFinishedPage(
                onUpgradeClick = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                    activity?.startActivity(AccountActivity.newUpgradeInstance(context))
                },
                onDoneClick = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
            )
        }
    }
}
