package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.app.Activity
import android.content.Intent
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.repositories.support.DatabaseExportHelper
import au.com.shiftyjelly.pocketcasts.repositories.support.Support
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject

@HiltViewModel
class HelpViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    private val support: Support,
    private val databaseExportHelper: DatabaseExportHelper,
) : ViewModel() {
    suspend fun exportDatabase(): File? {
        return databaseExportHelper.getExportFile()
    }

    suspend fun getFeedbackIntent(activity: Activity): Intent {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_LEAVE_FEEDBACK)
        return support.shareLogs(
            subject = "Android feedback.",
            intro = "It's a great app, but it really needs…",
            emailSupport = true,
            context = activity,
        )
    }

    suspend fun getSupportIntent(activity: Activity): Intent {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_GET_SUPPORT)
        return support.shareLogs(
            subject = "Android support.",
            intro = "Hi there, just needed help with something…",
            emailSupport = true,
            context = activity,
        )
    }
}
