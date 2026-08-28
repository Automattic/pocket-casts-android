package au.com.shiftyjelly.pocketcasts.discover

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TvDiscoverPodcastAttribution @Inject constructor() {

    data class Attribution(
        val listId: String?,
        val listDatetime: String?,
        val isFeatured: Boolean,
    )

    private val attributions = mutableMapOf<String, Attribution>()

    @Synchronized
    fun record(podcastUuid: String, listId: String?, listDatetime: String?, isFeatured: Boolean) {
        if (listId == null && !isFeatured) {
            attributions.remove(podcastUuid)
        } else {
            attributions[podcastUuid] = Attribution(listId, listDatetime, isFeatured)
        }
    }

    @Synchronized
    fun consume(podcastUuid: String): Attribution? = attributions.remove(podcastUuid)
}
