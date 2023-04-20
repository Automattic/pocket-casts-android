package au.com.shiftyjelly.pocketcasts.repositories.playback

interface PlayerFactory {

    fun createCastPlayer(onPlayerEvent: (PocketCastsPlayer, PlayerEvent) -> Unit): PocketCastsPlayer
    fun createSimplePlayer(onPlayerEvent: (PocketCastsPlayer, PlayerEvent) -> Unit): PocketCastsPlayer
}
