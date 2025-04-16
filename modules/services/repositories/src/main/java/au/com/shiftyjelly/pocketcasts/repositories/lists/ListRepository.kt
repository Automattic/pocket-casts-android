package au.com.shiftyjelly.pocketcasts.repositories.lists

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.server.ListWebService
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.rx2.rxSingle

class ListRepository(
    private val listWebService: ListWebService,
    private val syncManager: SyncManager,
    private val platform: String,
) {

    fun getDiscoverFeed(): Single<Discover> {
        return listWebService.getDiscoverFeed(
            platform = platform,
            version = getDiscoverFeedVersion(),
        )
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    suspend fun getDiscoverFeedSuspend(): Discover {
        return listWebService.getDiscoverFeedSuspend(
            platform = platform,
            version = getDiscoverFeedVersion(),
        )
    }

    private fun getDiscoverFeedVersion(): Int {
        return if (FeatureFlag.isEnabled(Feature.RECOMMENDATIONS)) 3 else 2
    }

    fun getListFeed(url: String, authenticated: Boolean?): Single<ListFeed> {
        return rxSingle { getListFeedSuspend(url, authenticated) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    suspend fun getListFeedSuspend(url: String, authenticated: Boolean?): ListFeed {
        return if (authenticated == true) {
            syncManager.getCacheTokenOrLogin { token ->
                listWebService.getListFeedAuthenticated(url, "Bearer ${token.value}")
            }
        } else {
            listWebService.getListFeedSuspend(url)
        }
    }

    fun getCategoriesList(url: String): Single<List<DiscoverCategory>> {
        return listWebService.getCategoriesList(url)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    suspend fun getSimilarPodcasts(podcastUuid: String): ListFeed {
        return getListFeedSuspend(
            url = "${Settings.SERVER_API_URL}/recommendations/podcast?podcast_uuid=$podcastUuid",
            authenticated = false,
        )
    }
}
