package au.com.shiftyjelly.pocketcasts.endofyear

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ShareResultReceiver : BroadcastReceiver() {
    @Inject
    lateinit var analyticsTracker: AnalyticsTrackerWrapper

    @Inject
    lateinit var shareableTextProvider: ShareableTextProvider

    override fun onReceive(context: Context, intent: Intent) {
        val componentName: ComponentName? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT, ComponentName::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_CHOSEN_COMPONENT)
            }
        shareableTextProvider.chosenActivity = componentName?.className
    }

    companion object {
        const val EXTRA_STORY_ID = "extra_story_id"
    }
}
