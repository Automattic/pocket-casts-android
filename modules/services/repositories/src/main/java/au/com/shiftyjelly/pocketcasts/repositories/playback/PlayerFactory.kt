package au.com.shiftyjelly.pocketcasts.repositories.playback

interface PlayerFactory {

    fun createCastPlayer(onPlayerEvent: (Player, PlayerEvent) -> Unit): Player
    fun createSimplePlayer(onPlayerEvent: (Player, PlayerEvent) -> Unit): Player
}
