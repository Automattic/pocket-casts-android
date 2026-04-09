package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls

/**
 * Strategy interface for automotive-specific custom action layout and metadata formatting.
 *
 * Automotive always uses a [MediaLibraryService][androidx.media3.session.MediaLibraryService]
 * shell, but the internal behavior differs based on the `media3_session` feature flag:
 * - Flag ON: [Media3AutomotiveStrategy] — new layout with promoted speed button and circular skip icons.
 * - Flag OFF: [LegacyAutomotiveStrategy] — matches the old `MediaBrowserServiceCompat` behavior.
 */
@UnstableApi
internal interface AutomotiveSessionStrategy {

    fun buildCustomLayout(
        playbackManager: PlaybackManager,
        settings: Settings,
        context: Context,
        buildCustomActionButton: (MediaNotificationControls, BaseEpisode?) -> CommandButton?,
    ): List<CommandButton>
}
