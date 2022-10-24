package au.com.shiftyjelly.pocketcasts.servers.bumpstats

import au.com.shiftyjelly.pocketcasts.servers.di.WpComServerRetrofit
import retrofit2.Retrofit
import javax.inject.Inject

class WpComServerManager @Inject constructor(
    @WpComServerRetrofit retrofit: Retrofit
) {

    private val server: WpComServer = retrofit.create(WpComServer::class.java)

    suspend fun bumpStatAnonymously(key: String, properties: Map<String, String>) {
        val request = AnonymousBumpStatsRequest(
            events = listOf(
                AnonymousBumpStatsRequest.Event(
                    name = key,
                    eventTime = System.currentTimeMillis(), // FIXME make sure this stays the time of the event
                    customEventProps = properties
                )
            )
        )
        return server.bumpStatAnonymously(request)
    }
}
