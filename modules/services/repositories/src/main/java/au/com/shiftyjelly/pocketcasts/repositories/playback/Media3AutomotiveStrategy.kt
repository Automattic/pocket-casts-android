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
 * Automotive strategy for the Media3 session path (flag ON).
 *
 * Layout: playback speed first (promoted slot), then circular skip icons
 * matching the configured duration, then remaining custom actions
 * (excluding speed since it is already shown).
 */
@UnstableApi
internal class Media3AutomotiveStrategy(
    private val speedToDrawable: (Double) -> Int,
    private val skipBackIconForDuration: (Int) -> Int,
    private val skipForwardIconForDuration: (Int) -> Int,
) : AutomotiveSessionStrategy {

    override fun buildCustomLayout(
        playbackManager: PlaybackManager,
        settings: Settings,
        context: Context,
        buildCustomActionButton: (MediaNotificationControls, BaseEpisode?) -> CommandButton?,
    ): List<CommandButton> {
        val buttons = mutableListOf<CommandButton>()
        val currentEpisode = playbackManager.getCurrentEpisode()

        if (playbackManager.isAudioEffectsAvailable()) {
            buttons.add(
                CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                    .setSessionCommand(SessionCommand(APP_ACTION_CHANGE_SPEED, Bundle.EMPTY))
                    .setDisplayName(context.getString(LR.string.playback_speed))
                    .setCustomIconResId(speedToDrawable(playbackManager.getPlaybackSpeed()))
                    .build(),
            )
        }
        buttons.add(
            CommandButton.Builder(skipBackIconForDuration(settings.skipBackInSecs.value))
                .setSessionCommand(SessionCommand(APP_ACTION_SKIP_BACK, Bundle.EMPTY))
                .setDisplayName(context.getString(LR.string.skip_back))
                .setCustomIconResId(IR.drawable.media_skipback)
                .build(),
        )
        buttons.add(
            CommandButton.Builder(skipForwardIconForDuration(settings.skipForwardInSecs.value))
                .setSessionCommand(SessionCommand(APP_ACTION_SKIP_FWD, Bundle.EMPTY))
                .setDisplayName(context.getString(LR.string.skip_forward))
                .setCustomIconResId(IR.drawable.media_skipforward)
                .build(),
        )
        val visibleCount = if (settings.customMediaActionsVisibility.value) MediaNotificationControls.MAX_VISIBLE_OPTIONS else 0
        settings.mediaControlItems.value.take(visibleCount).forEach { mediaControl ->
            if (mediaControl != MediaNotificationControls.PlaybackSpeed) {
                buildCustomActionButton(mediaControl, currentEpisode)?.let(buttons::add)
            }
        }
        return buttons
    }
}
