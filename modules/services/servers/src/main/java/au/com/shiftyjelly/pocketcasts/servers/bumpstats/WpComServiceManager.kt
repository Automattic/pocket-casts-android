package au.com.shiftyjelly.pocketcasts.servers.bumpstats

import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat
import javax.inject.Inject
import retrofit2.Response

class WpComServiceManager @Inject constructor(
    private val service: WpComService,
) {
    suspend fun bumpStatAnonymously(bumpStats: List<AnonymousBumpStat>): Response<String> {
        val request = AnonymousBumpStatsRequest(bumpStats)
        return service.bumpStatAnonymously(request)
    }
}
