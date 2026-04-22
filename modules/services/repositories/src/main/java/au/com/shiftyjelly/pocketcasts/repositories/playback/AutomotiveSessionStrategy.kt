package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls

/**
 * Strategy interface for automotive-specific custom action layout.
 *
 * Returns two lists:
 * - **primaryButtons**: shown in the main transport bar via `setMediaButtonPreferences`
 * - **overflowButtons**: additional actions available in the overflow menu via `setCustomLayout`
 */
@UnstableApi
internal interface AutomotiveSessionStrategy {

    data class ButtonLayout(
        val primaryButtons: List<CommandButton>,
        val overflowButtons: List<CommandButton>,
    )

    fun buildLayout(
        playbackManager: PlaybackManager,
        settings: Settings,
        context: Context,
        buildCustomActionButton: (MediaNotificationControls, BaseEpisode?) -> CommandButton?,
    ): ButtonLayout
}
