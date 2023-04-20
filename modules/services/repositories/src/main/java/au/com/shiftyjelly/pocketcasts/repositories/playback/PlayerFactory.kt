package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.media3.common.Player

interface PlayerFactory {

    fun createSimplePlayer(
        onPlayerEvent: (PocketCastsPlayer, PlayerEvent) -> Unit,
        player: Player,
    ): PocketCastsPlayer

    fun createCastPlayer(
        onPlayerEvent: (PocketCastsPlayer, PlayerEvent) -> Unit,
        player: Player,
    ): PocketCastsPlayer
}
