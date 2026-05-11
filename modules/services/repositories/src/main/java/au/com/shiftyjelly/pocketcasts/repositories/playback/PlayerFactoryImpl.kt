package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.stats.PlaybackStatsCollector
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PlayerFactoryImpl @Inject constructor(
    private val settings: Settings,
    private val statsManager: StatsManager,
    private val playbackStatsCollector: PlaybackStatsCollector,
    private val dataSourceFactory: ExoPlayerDataSourceFactory,
    @ApplicationContext private val context: Context,
) : PlayerFactory {

    override fun createCastPlayer(onPlayerEvent: (Player, PlayerEvent) -> Unit): Player {
        return CastPlayer(
            playbackStatsCollector = playbackStatsCollector,
            onPlayerEvent = onPlayerEvent,
        )
    }

    override fun createSimplePlayer(onPlayerEvent: (Player, PlayerEvent) -> Unit): Player {
        return SimplePlayer(
            settings = settings,
            statsManager = statsManager,
            playbackStatsCollector = playbackStatsCollector,
            context = context,
            dataSourceFactory = dataSourceFactory,
            onPlayerEvent = onPlayerEvent,
        )
    }
}
