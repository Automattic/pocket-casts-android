package au.com.shiftyjelly.pocketcasts.servers.server

import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag

class ListRepository(private val listWebService: ListWebService, private val platform: String) {

    suspend fun getDiscoverFeed(): Discover {
        val version = if (FeatureFlag.isEnabled(Feature.RECOMMENDATIONS)) 3 else 2
        return listWebService.getDiscoverFeed(platform = platform, version = version)
    }

    suspend fun getListFeed(url: String): ListFeed {
        return listWebService.getListFeed(url)
    }

    suspend fun getCategoriesList(url: String): List<DiscoverCategory> {
        return listWebService.getCategoriesList(url)
    }
}
