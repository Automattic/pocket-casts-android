package au.com.shiftyjelly.pocketcasts.discover.util

import android.content.res.Resources
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.NetworkLoadableList
import au.com.shiftyjelly.pocketcasts.servers.model.transformWithRegion
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DiscoverDeepLinkManager @Inject constructor(
    private val repository: ListRepository,
    private val settings: Settings,
) {
    companion object {
        const val STAFF_PICKS_LIST_ID = "staff-picks"
        const val RECOMMENDATIONS_USER = "recommendations_user"
    }

    suspend fun getDiscoverList(listId: String, resources: Resources): NetworkLoadableList? = withContext(Dispatchers.IO) {
        val discover: Discover = repository.getDiscoverFeed()
        val currentRegionCode: String = settings.discoverCountryCode.value
        val defaultRegion: String = discover.defaultRegionCode
        val region = discover.regions[currentRegionCode] ?: discover.regions[defaultRegion] ?: return@withContext null

        val replacements: Map<String, String> = mapOf(
            discover.regionCodeToken to region.code,
            discover.regionNameToken to region.name,
        )

        val discoverRows: List<DiscoverRow> = discover.layout.transformWithRegion(region, replacements, resources)
        val staffPicksRow: DiscoverRow? = discoverRows.firstOrNull { it.inferredId() == listId }
        staffPicksRow?.transformWithReplacements(replacements, resources)
    }
}
