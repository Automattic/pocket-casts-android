package au.com.shiftyjelly.pocketcasts.servers.cdn

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.servers.di.StaticServerRetrofit
import au.com.shiftyjelly.pocketcasts.utils.Optional
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Retrofit
import javax.inject.Inject

class StaticServerManagerImpl @Inject constructor(@StaticServerRetrofit retrofit: Retrofit) : StaticServerManager {

    val server = retrofit.create(StaticServer::class.java)

    override fun getColorsSingle(podcastUuid: String): Single<Optional<ArtworkColors>> {
        return server.getColorsMaybe(podcastUuid)
            // convert response to artwork colors
            .map(ColorsResponse::toArtworkColors)
            // convert to optional so we can handle if the value is missing
            .map { Optional.of(it) }
            .defaultIfEmpty(Optional.empty())
            .toSingle()
    }

    override suspend fun getColors(podcastUuid: String): ArtworkColors? {
        return server.getColors(podcastUuid)?.toArtworkColors()
    }

    fun getFeaturedPodcasts(): Observable<Podcast> {
        return server.getFeaturedPodcasts()
            .flatMap(PodcastsResponse::toPodcasts)
    }
}
