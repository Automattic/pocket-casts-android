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
import androidx.compose.ui.unit.dp
import androidx.fragment.compose.content
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
internal class EnableNotificationsPromptFragment : BaseDialogFragment() {

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var settings: Settings

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
            handleDismissedByUser()
        }
        isFinalizingActionUsed = true
        dismiss()
    }

    private var isFinalizingActionUsed = false

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
                    ),
                ),
        ) {
            AppThemeWithBackground(theme.activeTheme) {
                EnableNotificationsPromptScreen(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
                    onCtaClick = {
                        analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_PERMISSIONS_ALLOW_TAPPED)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                                permissionRequester.launch(Manifest.permission.POST_NOTIFICATIONS)
                                analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_OPT_IN_SHOWN)
                            } else {
                                notificationHelper.openNotificationSettings(requireActivity())
                            }
                        }
                    },
                    onDismissClick = {
                        isFinalizingActionUsed = true
                        handleDismissedByUser()
                        dismiss()
                    },
                )
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (!requireActivity().isChangingConfigurations && !isFinalizingActionUsed) {
            handleDismissedByUser()
        }
    }

    private fun handleDismissedByUser() {
        settings.notificationsPromptAcknowledged.set(value = true, updateModifiedAt = true)
        analyticsTracker.track(AnalyticsEvent.NOTIFICATIONS_PERMISSIONS_DISMISSED)
    }
}
