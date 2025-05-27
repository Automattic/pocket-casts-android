package au.com.shiftyjelly.pocketcasts.podcasts.view.notifications

import android.Manifest
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class EnableNotificationsPromptFragment : BaseDialogFragment() {

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    companion object {
        fun newInstance(): EnableNotificationsPromptFragment {
            return EnableNotificationsPromptFragment()
        }
    }

    private val permissionRequester = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_OPT_IN_ALLOWED)
        } else {
            analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_OPT_IN_DENIED)
        }

        dismiss()
    }

    private var wasDismissedViaCloseButton = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = content {
        CallOnce {
            analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_PERMISSIONS_SHOWN)
        }

        Box(
            modifier = Modifier
                .fillMaxHeight(0.93f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                    )
                ),
        ) {
            AppThemeWithBackground(theme.activeTheme) {
                Box(
                    modifier = Modifier
                        .nestedScroll(rememberNestedScrollInteropConnection())
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                ) {

                    EnableNotificationsPromptScreen(
                        modifier = Modifier.padding(
                            vertical = 16.dp,
                            horizontal = 16.dp,
                        ),
                        onCtaClicked = {
                            analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_PERMISSIONS_ALLOW_TAPPED)
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionRequester.launch(Manifest.permission.POST_NOTIFICATIONS)
                                analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_OPT_IN_SHOWN)
                            }
                        },
                        onDismissClicked = {
                            wasDismissedViaCloseButton = true
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (wasDismissedViaCloseButton) {
            analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_PERMISSIONS_DISMISSED)
        }
    }
}