package au.com.shiftyjelly.pocketcasts

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.ui.MainActivity
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions
import com.google.android.gms.cast.framework.media.MediaIntentReceiver
import com.google.android.gms.cast.framework.media.NotificationOptions
import com.google.android.gms.cast.framework.R as CastR

class CastOptionsProvider : OptionsProvider {
    override fun getCastOptions(context: Context): CastOptions {
        val application = context as PocketCastsApplication
        val buttonActions = listOf(
            MediaIntentReceiver.ACTION_REWIND,
            MediaIntentReceiver.ACTION_TOGGLE_PLAYBACK,
            MediaIntentReceiver.ACTION_FORWARD,
            MediaIntentReceiver.ACTION_STOP_CASTING,
        )
        val compatButtonActionsIndices = intArrayOf(0, 1, 2)
        val forwardDrawableResId = when (application.settings.skipForwardInSecs.value) {
            10 -> CastR.drawable.cast_ic_notification_forward10
            30 -> CastR.drawable.cast_ic_notification_forward30
            else -> CastR.drawable.cast_ic_notification_forward
        }
        val notificationOptions = NotificationOptions.Builder()
            .setActions(buttonActions, compatButtonActionsIndices)
            .setSkipStepMs(application.settings.skipBackInSecs.value * 1000L)
            .setForwardDrawableResId(forwardDrawableResId)
            .setForward10DrawableResId(forwardDrawableResId)
            .setForward30DrawableResId(forwardDrawableResId)
            .setTargetActivityClassName(MainActivity::class.java.name)
            .build()
        val mediaOptions = CastMediaOptions.Builder()
            .setNotificationOptions(notificationOptions)
            .setMediaIntentReceiverClassName(PocketCastsMediaIntentReceiver::class.java.name)
            .setExpandedControllerActivityClassName(MainActivity::class.java.name)
            .build()
        return CastOptions.Builder()
            .setReceiverApplicationId(Settings.CHROME_CAST_APP_ID)
            .setCastMediaOptions(mediaOptions)
            .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider> {
        return emptyList()
    }
}
