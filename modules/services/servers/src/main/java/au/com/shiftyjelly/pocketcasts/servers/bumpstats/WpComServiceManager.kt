package au.com.shiftyjelly.pocketcasts.servers.bumpstats

import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat
import au.com.shiftyjelly.pocketcasts.servers.di.WpComServiceRetrofit
import javax.inject.Inject
import retrofit2.Response
import retrofit2.Retrofit

class WpComServiceManager @Inject constructor(
    @WpComServiceRetrofit retrofit: Retrofit,
) {
    private val service: WpComService = retrofit.create(WpComService::class.java)

    suspend fun bumpStatAnonymously(bumpStats: List<AnonymousBumpStat>): Response<String> {
        val request = AnonymousBumpStatsRequest(bumpStats)
        return service.bumpStatAnonymously(request)
    }
}
