package au.com.shiftyjelly.pocketcasts.servers.server

import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed

class ListRepository(private val listWebService: ListWebService, private val platform: String) {

    suspend fun getDiscoverFeed(): Discover {
        return listWebService.getDiscoverFeed(platform)
    }

    suspend fun getListFeed(url: String): ListFeed {
        return listWebService.getListFeed(url)
    }

    suspend fun getCategoriesList(url: String): List<DiscoverCategory> {
        return listWebService.getCategoriesList(url)
    }
}
