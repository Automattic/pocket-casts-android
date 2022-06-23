package au.com.shiftyjelly.pocketcasts.servers.server

import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

class ListRepository(private val listWebService: ListWebService, private val platform: String) {

    fun getDiscoverFeed(): Single<Discover> {
        return listWebService.getDiscoverFeed(platform)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    suspend fun getDiscoverFeedSuspend(): Discover {
        return listWebService.getDiscoverFeedSuspend(platform)
    }

    fun getListFeed(url: String): Single<ListFeed> {
        return listWebService.getListFeed(url)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }

    suspend fun getListFeedSuspend(url: String): ListFeed {
        return listWebService.getListFeedSuspend(url)
    }

    fun getBundleFeed(uuid: String): Single<ListFeed> {
        val url = "${BuildConfig.SERVER_LIST_URL}/bundle-$uuid.json"
        return getListFeed(url)
    }

    fun getCategoriesList(url: String): Single<List<DiscoverCategory>> {
        return listWebService.getCategoriesList(url)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
    }
}
