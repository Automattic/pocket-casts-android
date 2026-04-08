package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag

object PlaybackServiceToggle {

    fun ensureCorrectServiceEnabled(
        context: Context,
        media3Services: List<String> = listOf(PlaybackService::class.java.name),
        legacyServices: List<String> = listOf(LegacyPlaybackService::class.java.name),
    ) {
        val useMedia3 = FeatureFlag.isEnabled(Feature.MEDIA3_SESSION)
        media3Services.forEach { setComponentEnabled(context, it, useMedia3) }
        legacyServices.forEach { setComponentEnabled(context, it, !useMedia3) }
    }

    private fun setComponentEnabled(context: Context, className: String, enabled: Boolean) {
        val componentName = ComponentName(context.packageName, className)
        val desiredState = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        val currentState = context.packageManager.getComponentEnabledSetting(componentName)
        if (currentState != desiredState) {
            context.packageManager.setComponentEnabledSetting(
                componentName,
                desiredState,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
