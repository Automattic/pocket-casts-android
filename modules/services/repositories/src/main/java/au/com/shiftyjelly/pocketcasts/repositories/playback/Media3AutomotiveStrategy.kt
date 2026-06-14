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

@UnstableApi
internal class Media3AutomotiveStrategy(
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

        if (useCustomSkipButtons()) {
            buttons.add(
                CommandButton.Builder(CommandButton.ICON_UNDEFINED)
                    .setSessionCommand(SessionCommand(APP_ACTION_SKIP_BACK, Bundle.EMPTY))
                    .setDisplayName(context.getString(LR.string.skip_back))
                    .setCustomIconResId(IR.drawable.media_skipback)
                    .build(),
            )
            buttons.add(
                CommandButton.Builder(CommandButton.ICON_UNDEFINED)
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

        buildShuffleButton(settings, context)?.let(buttons::add)

        return AutomotiveSessionStrategy.ButtonLayout(primaryButtons = buttons, overflowButtons = emptyList())
    }

    /**
     * Builds the Up Next shuffle toggle button shown in the AAOS player.
     *
     * Mirrors the mobile Up Next shuffle control: it is a paid (Plus/Patron) feature, so it is
     * only shown to subscribers. Shuffle is a persistent mode toggle, so the button is always
     * present for subscribers regardless of the current queue length. The icon reflects whether
     * shuffle mode is currently enabled.
     */
    private fun buildShuffleButton(
        settings: Settings,
        context: Context,
    ): CommandButton? {
        val isPaidUser = settings.cachedSubscription.value != null
        if (!isPaidUser) {
            return null
        }

        val iconRes = if (settings.upNextShuffle.value) IR.drawable.shuffle_enabled else IR.drawable.shuffle
        return CommandButton.Builder(CommandButton.ICON_UNDEFINED)
            .setSessionCommand(SessionCommand(APP_ACTION_SHUFFLE, Bundle.EMPTY))
            .setDisplayName(context.getString(LR.string.up_next_shuffle_button_content_description))
            .setCustomIconResId(iconRes)
            .build()
    }
}
