package au.com.shiftyjelly.pocketcasts.settings.notificationstesting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NotificationsTestingFragment : BaseFragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = contentWithoutConsumedInsets {
        AppTheme(theme.activeTheme) {
            NotificationsTestingPage(
                onBack = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
            )
        }
    }
}
