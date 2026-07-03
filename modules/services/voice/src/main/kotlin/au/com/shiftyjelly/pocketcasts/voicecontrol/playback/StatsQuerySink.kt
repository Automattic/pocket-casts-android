package au.com.shiftyjelly.pocketcasts.voicecontrol.playback

import au.com.shiftyjelly.pocketcasts.models.to.StatsBundle
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.runBlocking

@Singleton
class StatsQuerySink @Inject constructor(
    private val statsManager: StatsManager,
) : VoiceStatsQuerySink {
    override fun listeningTime(period: String?): VoiceResponse.Spoken {
        val timeListened = serverStats()?.values?.get(StatsBundle.SERVER_KEY_TOTAL_LISTENED) ?: 0L
        val hours = timeListened / 3600
        return VoiceResponse.Spoken("$hours hours total listening time")
    }

    private fun serverStats(): StatsBundle? = try {
        runBlocking { statsManager.getServerStats() }
    } catch (_: Exception) {
        null
    }

    override fun topPodcasts(period: String?): VoiceResponse.Spoken = VoiceResponse.Spoken("Top podcasts are available in the stats view")

    override fun episodesFinished(period: String?): VoiceResponse.Spoken = VoiceResponse.Spoken("Episode completion stats are available in the stats view")

    override fun listeningStreak(): VoiceResponse.Spoken = VoiceResponse.Spoken("Listening streak is available in the stats view")

    override fun subscriptionCount(): VoiceResponse.Spoken = VoiceResponse.Spoken("Subscription count is available in the stats view")

    override fun unplayedTotal(): VoiceResponse.Spoken = VoiceResponse.Spoken("Unplayed count is available in the stats view")

    override fun downloadStats(): VoiceResponse.Spoken = VoiceResponse.Spoken("Download stats are available in the stats view")

    override fun queueTotal(): VoiceResponse.Spoken = VoiceResponse.Spoken("Queue total is available in the player")

    override fun newEpisodes(timeframe: String?): VoiceResponse.Spoken = VoiceResponse.Spoken("New episodes are shown in the podcast view")

    override fun timeSinceLastListen(): VoiceResponse.Spoken = VoiceResponse.Spoken("Last listen time is available in the stats view")
}
