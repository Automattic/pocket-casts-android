package au.com.shiftyjelly.pocketcasts.servers.cdn

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.servers.di.StaticServiceRetrofit
import au.com.shiftyjelly.pocketcasts.utils.Optional
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import retrofit2.Retrofit

class StaticServiceManagerImpl @Inject constructor(@StaticServiceRetrofit retrofit: Retrofit) : StaticServiceManager {

    val server = retrofit.create(StaticService::class.java)

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
