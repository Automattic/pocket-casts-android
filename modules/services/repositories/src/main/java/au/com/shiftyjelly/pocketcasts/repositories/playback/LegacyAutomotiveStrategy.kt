package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.MediaNotificationControls
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * Automotive strategy for the legacy session path (flag OFF).
 *
 * Matches the old `MediaBrowserServiceCompat` custom action layout:
 * skip buttons first (if [useCustomSkipButtons]), then media control items
 * in settings order (archive, mark as played, play next, speed, star)
 * up to [MediaNotificationControls.MAX_VISIBLE_OPTIONS].
 *
 * Uses generic skip icons (not circular duration icons) to match the old behavior.
 */
@UnstableApi
internal class LegacyAutomotiveStrategy(
    private val useCustomSkipButtons: () -> Boolean,
) : AutomotiveSessionStrategy {

    override fun buildLayout(
        playbackManager: PlaybackManager,
        settings: Settings,
        context: Context,
        buildCustomActionButton: (MediaNotificationControls, BaseEpisode?) -> CommandButton?,
    ): AutomotiveSessionStrategy.ButtonLayout {
        val buttons = mutableListOf<CommandButton>()
        val currentEpisode = playbackManager.getCurrentEpisode()

        // Legacy order: skip buttons first, then all media control items in order.
        if (useCustomSkipButtons()) {
            buttons.add(
                CommandButton.Builder(CommandButton.ICON_SKIP_BACK)
                    .setSessionCommand(SessionCommand(APP_ACTION_SKIP_BACK, Bundle.EMPTY))
                    .setDisplayName(context.getString(LR.string.skip_back))
                    .setCustomIconResId(IR.drawable.media_skipback)
                    .build(),
            )
            buttons.add(
                CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD)
                    .setSessionCommand(SessionCommand(APP_ACTION_SKIP_FWD, Bundle.EMPTY))
                    .setDisplayName(context.getString(LR.string.skip_forward))
                    .setCustomIconResId(IR.drawable.media_skipforward)
                    .build(),
            )
        }

        val visibleCount = if (settings.customMediaActionsVisibility.value) MediaNotificationControls.MAX_VISIBLE_OPTIONS else 0
        settings.mediaControlItems.value.take(visibleCount).forEach { mediaControl ->
            buildCustomActionButton(mediaControl, currentEpisode)?.let(buttons::add)
        }

        // Legacy strategy puts everything in one flat list (no primary/overflow split)
        return AutomotiveSessionStrategy.ButtonLayout(primaryButtons = buttons, overflowButtons = emptyList())
    }
}
