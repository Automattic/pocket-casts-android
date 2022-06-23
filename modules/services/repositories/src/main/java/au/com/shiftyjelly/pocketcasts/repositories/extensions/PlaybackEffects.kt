package au.com.shiftyjelly.pocketcasts.repositories.extensions

import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.preferences.Settings

fun PlaybackEffects.saveToGlobalSettings(settings: Settings) {
    settings.setGlobalAudioEffects(playbackSpeed, trimMode, isVolumeBoosted)
}
