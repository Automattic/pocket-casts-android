package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.media3.common.Player

interface PlayerFactory {

    fun createSimplePlayer(
        onPlayerEvent: (PocketCastsPlayer, PlayerEvent) -> Unit,
        player: Player,
    ): PocketCastsPlayer

    fun createCastingPlayer(
        onPlayerEvent: (PocketCastsPlayer, PlayerEvent) -> Unit,
        player: Player,
    ): PocketCastsPlayer
}
