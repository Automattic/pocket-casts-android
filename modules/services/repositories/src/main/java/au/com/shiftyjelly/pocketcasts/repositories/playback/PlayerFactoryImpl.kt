package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PlayerFactoryImpl @Inject constructor(
    private val settings: Settings,
    private val statsManager: StatsManager,
    @ApplicationContext private val context: Context
) : PlayerFactory {

    override fun createCastPlayer(
        onPlayerEvent: (PocketCastsPlayer, PlayerEvent) -> Unit,
        player: Player,
    ): PocketCastsPlayer {
        return CastingPlayer(
            context = context,
            onPlayerEvent = onPlayerEvent,
            player = player,
        )
    }

    @UnstableApi
    override fun createSimplePlayer(
        onPlayerEvent: (PocketCastsPlayer, PlayerEvent) -> Unit,
        player: Player,
    ): PocketCastsPlayer {
        return SimplePlayer(
            settings = settings,
            statsManager = statsManager,
            context = context,
            onPlayerEvent = onPlayerEvent,
            player = player,
        )
    }
}
