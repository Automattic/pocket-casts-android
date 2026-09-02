package au.com.shiftyjelly.pocketcasts.discover

import android.content.Context
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.model.Discover
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverEpisode
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverPodcast
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRegion
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverRow
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyle
import au.com.shiftyjelly.pocketcasts.servers.model.ListFeed
import au.com.shiftyjelly.pocketcasts.servers.model.ListType
import au.com.shiftyjelly.pocketcasts.servers.model.transformWithRegion
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Singleton
class TvDiscoverFeedLoader @Inject constructor(
    private val listRepository: ListRepository,
    private val settings: Settings,
    @ApplicationScope private val applicationScope: CoroutineScope,
    @ApplicationContext private val context: Context,
) {
    private val categoryCoverUrlCache = ConcurrentHashMap<String, List<String>>()
    private val categoryCoverUrlLoads = ConcurrentHashMap<String, Deferred<List<String>>>()

    suspend fun load(isLoggedIn: Boolean): List<TvDiscoverRow> {
        return buildRows(listRepository.getHomeDiscoverFeed(isLoggedIn), isLoggedIn, includeHomeSections = true)
    }

    suspend fun searchDiscoverFeed(): Discover = listRepository.getSearchDiscoverFeed()

    suspend fun loadCategories(discover: Discover): List<DiscoverCategory> {
        val source = discover.layout.firstOrNull { it.type is ListType.Categories }
            ?.source?.takeIf(String::isNotBlank) ?: return emptyList()
        val replacements = regionReplacements(discover)
        return try {
            listRepository.getCategoriesList(source).map { it.resolveSource(replacements) }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to load TV search categories")
            emptyList()
        }
    }

    suspend fun loadCategoryPodcasts(source: String, categoryId: Int, isLoggedIn: Boolean): TvCategoryPodcasts = coroutineScope {
        val feedDeferred = async { checkNotNull(listRepository.getListFeed(source)) { "Failed to load category feed $source" } }
        val sponsoredDeferred = async { loadCategorySponsoredPodcasts(categoryId, isLoggedIn) }
        val feed = feedDeferred.await()
        val podcasts = feed.podcasts.orEmpty()
            .distinctBy(DiscoverPodcast::uuid)
            .map { it.toTvDiscoverPodcast(isSponsored = false) }
        TvCategoryPodcasts(
            listId = feed.listId,
            podcasts = mergeCategorySponsored(podcasts, sponsoredDeferred.await()),
        )
    }

    suspend fun loadCategoryCoverUrls(source: String): List<String> {
        if (source.isBlank()) return emptyList()
        categoryCoverUrlCache[source]?.let { return it }
        val load = categoryCoverUrlLoads.computeIfAbsent(source) {
            applicationScope.async {
                try {
                    val feed = listRepository.getListFeed(source) ?: return@async emptyList()
                    val uuids = feed.podcasts.orEmpty().map(DiscoverPodcast::uuid).distinct()
                    val urls = listOfNotNull(uuids.firstOrNull(), uuids.lastOrNull().takeIf { uuids.size > 1 })
                        .map { PodcastImage.getArtworkUrl(size = COVER_ARTWORK_SIZE, uuid = it, isWearOS = false) }
                    categoryCoverUrlCache[source] = urls
                    urls
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    Timber.e(exception, "Failed to load TV category covers")
                    emptyList()
                }
            }
        }
        load.invokeOnCompletion { categoryCoverUrlLoads.remove(source, load) }
        return load.await()
    }

    private suspend fun loadCategorySponsoredPodcasts(categoryId: Int, isLoggedIn: Boolean): List<TvDiscoverPodcast> = coroutineScope {
        val discover = try {
            listRepository.getHomeDiscoverFeed(isLoggedIn)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to load TV category sponsored ads")
            return@coroutineScope emptyList()
        }
        val region = resolveRegionOrFallback(discover) ?: return@coroutineScope emptyList()
        val replacements = regionReplacements(discover, region)
        discover.layout
            .transformWithRegion(region, replacements, context.resources)
            .filter { it.categoryId == categoryId && it.sponsored && it.source.isNotBlank() && (isLoggedIn || it.authenticated != true) }
            .map { row -> async { loadSponsoredPodcasts(row) } }
            .awaitAll()
            .flatten()
    }

    private fun mergeCategorySponsored(podcasts: List<TvDiscoverPodcast>, sponsored: List<TvDiscoverPodcast>): List<TvDiscoverPodcast> {
        if (sponsored.isEmpty()) return podcasts
        val sponsoredUuids = sponsored.mapTo(mutableSetOf(), TvDiscoverPodcast::uuid)
        val rest = podcasts.filterNot { it.uuid in sponsoredUuids }
        val position = minOf(SPONSORED_CATEGORY_POSITION, rest.size)
        return rest.toMutableList().apply { addAll(position, sponsored) }
    }

    suspend fun buildRows(discover: Discover, isLoggedIn: Boolean, includeHomeSections: Boolean = false): List<TvDiscoverRow> = coroutineScope {
        val region = resolveRegion(discover)
        val replacements = regionReplacements(discover, region)

        discover.layout
            .transformWithRegion(region, replacements, context.resources)
            .filter { it.categoryId == null } // Rows with a category ID are sponsored ads for the category pages.
            .filter { isLoggedIn || it.authenticated != true }
            .map { row -> async { loadRow(row, includeHomeSections, replacements) } }
            .awaitAll()
            .filterNotNull()
            .let { rows ->
                // Dedup after loading so a duplicate id whose feed failed or came back empty falls back to a populated one.
                val populatedIds = rows.filterNot { it is TvDiscoverRow.Failed }.mapTo(mutableSetOf(), TvDiscoverRow::id)
                rows.filterNot { it is TvDiscoverRow.Failed && it.id in populatedIds }
                    .distinctBy(TvDiscoverRow::id)
            }
    }

    suspend fun reloadHomeRow(rowId: String, isLoggedIn: Boolean): TvDiscoverRow? {
        return reloadRow(listRepository.getHomeDiscoverFeed(isLoggedIn), rowId, includeHomeSections = true)
    }

    suspend fun reloadSearchRow(rowId: String, isLoggedIn: Boolean): TvDiscoverRow? {
        return reloadRow(listRepository.getSearchDiscoverFeed(), rowId, includeHomeSections = false)
    }

    private suspend fun reloadRow(discover: Discover, rowId: String, includeHomeSections: Boolean): TvDiscoverRow? {
        val region = resolveRegion(discover)
        val replacements = regionReplacements(discover, region)
        val row = discover.layout
            .transformWithRegion(region, replacements, context.resources)
            .firstOrNull { it.rowId() == rowId } ?: return null
        return loadRow(row, includeHomeSections, replacements)
    }

    private suspend fun loadRow(row: DiscoverRow, includeHomeSections: Boolean, replacements: Map<String, String>): TvDiscoverRow? {
        val bannerType = row.type
        if (bannerType is ListType.Unknown && bannerType.value == BANNER_TYPE) {
            if (!includeHomeSections) return null
            val banner = TvDiscoverBanner.fromId(row.id) ?: return null
            return TvDiscoverRow.Banner(id = banner.id, title = row.title, banner = banner)
        }
        return when (row.type) {
            is ListType.PodcastList -> if (row.source.isBlank()) null else loadRowOrFailed(row) { loadPodcastsRow(row) }

            is ListType.EpisodeList -> if (row.source.isBlank()) null else loadRowOrFailed(row) { loadEpisodesRow(row) }

            is ListType.Categories -> if (includeHomeSections) loadCategoriesRow(row, replacements) else null

            is ListType.Unknown -> {
                Timber.w("Dropping unknown TV discover row type '${(row.type as ListType.Unknown).value}' for ${row.rowId()}")
                null
            }
        }
    }

    private suspend fun loadRowOrFailed(row: DiscoverRow, block: suspend () -> TvDiscoverRow?): TvDiscoverRow? {
        return try {
            block()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to load TV discover row ${row.rowId()}")
            TvDiscoverRow.Failed(id = row.rowId(), title = row.title)
        }
    }

    private suspend fun loadPodcastsRow(row: DiscoverRow): TvDiscoverRow? = coroutineScope {
        val feedDeferred = async { listRepository.getListFeedResult(row.source, row.authenticated).getOrThrow() }
        val insertionsDeferred = async { loadSponsoredInsertions(row) }
        val feed = feedDeferred.await() ?: return@coroutineScope null
        val basePodcasts = feed.podcasts.orEmpty()
            .distinctBy(DiscoverPodcast::uuid)
            .map { it.toTvDiscoverPodcast(isSponsored = row.sponsored) }
        val podcasts = insertSponsored(basePodcasts, insertionsDeferred.await())
        if (podcasts.isEmpty()) return@coroutineScope null
        val feedTitle = feed.displayTitle(fallbackTitle = row.title)
        val listId = row.analyticsListId(feed)
        val listDatetime = feed.date
        when (row.displayStyle) {
            is DisplayStyle.Carousel -> TvDiscoverRow.FeaturedPodcasts(id = row.rowId(), title = feedTitle, podcasts = podcasts, listId = listId, listDatetime = listDatetime)

            is DisplayStyle.SinglePodcast -> {
                val title = if (row.sponsored) context.getString(LR.string.tv_sponsored_podcast_section_title) else feedTitle
                TvDiscoverRow.SinglePodcast(id = row.rowId(), title = title, podcasts = podcasts, listId = listId, listDatetime = listDatetime)
            }

            else -> TvDiscoverRow.Podcasts(id = row.rowId(), title = feedTitle, podcasts = podcasts, listId = listId, listDatetime = listDatetime)
        }
    }

    private suspend fun loadSponsoredInsertions(row: DiscoverRow): Map<Int, TvDiscoverPodcast> = coroutineScope {
        row.sponsoredPodcasts
            .mapNotNull { sponsored ->
                val source = sponsored.source?.takeIf(String::isNotBlank) ?: return@mapNotNull null
                val position = sponsored.position ?: return@mapNotNull null
                async { listRepository.getListFeed(source, row.authenticated)?.podcasts?.firstOrNull()?.let { position to it } }
            }
            .awaitAll()
            .filterNotNull()
            .associate { (position, podcast) -> position to podcast.toTvDiscoverPodcast(isSponsored = true) }
    }

    private suspend fun loadSponsoredPodcasts(row: DiscoverRow): List<TvDiscoverPodcast> {
        return listRepository.getListFeed(row.source, row.authenticated)?.podcasts.orEmpty()
            .distinctBy(DiscoverPodcast::uuid)
            .map { it.toTvDiscoverPodcast(isSponsored = true) }
    }

    private fun insertSponsored(podcasts: List<TvDiscoverPodcast>, insertions: Map<Int, TvDiscoverPodcast>): List<TvDiscoverPodcast> {
        if (insertions.isEmpty()) return podcasts
        val insertedUuids = insertions.values.mapTo(mutableSetOf(), TvDiscoverPodcast::uuid)
        val result = podcasts.filterNotTo(mutableListOf()) { it.uuid in insertedUuids }
        insertions.toSortedMap().forEach { (position, podcast) ->
            result.add(position.coerceIn(0, result.size), podcast)
        }
        return result
    }

    private suspend fun loadEpisodesRow(row: DiscoverRow): TvDiscoverRow? {
        val feed = listRepository.getListFeedResult(row.source, row.authenticated).getOrThrow() ?: return null
        val playsVideoPreview = row.displayStyle is DisplayStyle.VideoPreviewList
        val episodes = feed.episodes.orEmpty()
            .distinctBy(DiscoverEpisode::uuid)
            .map { episode ->
                val mediaUrl = episode.videoUrl
                TvDiscoverEpisode(
                    episodeUuid = episode.uuid,
                    episodeTitle = episode.title.orEmpty(),
                    podcastUuid = episode.podcast_uuid,
                    podcastTitle = episode.podcast_title.orEmpty(),
                    videoPreviewUrl = if (playsVideoPreview) mediaUrl else null,
                    mediaUrl = mediaUrl,
                    mediaType = episode.fileType,
                    durationSecs = episode.duration,
                    publishedDate = episode.published,
                    sizeInBytes = episode.size,
                )
            }
        if (episodes.isEmpty()) return null
        val title = feed.title?.takeIf { it.isNotBlank() } ?: row.title
        return TvDiscoverRow.Episodes(id = row.rowId(), title = title, episodes = episodes, listId = row.analyticsListId(feed), listDatetime = feed.date)
    }

    private suspend fun loadCategoriesRow(row: DiscoverRow, replacements: Map<String, String>): TvDiscoverRow? {
        if (row.source.isBlank()) return null
        val categories = try {
            listRepository.getCategoriesList(row.source).map { it.resolveSource(replacements) }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            Timber.e(exception, "Failed to load TV discover categories")
            return null
        }
        if (categories.isEmpty()) return null
        return TvDiscoverRow.Categories(
            id = row.rowId(),
            title = context.getString(LR.string.tv_search_browse_categories),
            categories = categories,
        )
    }

    private fun resolveRegionOrNull(discover: Discover): DiscoverRegion? {
        return discover.regions[settings.discoverCountryCode.value]
            ?: discover.regions[discover.defaultRegionCode]
    }

    private fun resolveRegionOrFallback(discover: Discover): DiscoverRegion? {
        return resolveRegionOrNull(discover)
            ?: discover.regions[FALLBACK_REGION_CODE]
            ?: discover.regions.values.firstOrNull()
    }

    private fun resolveRegion(discover: Discover): DiscoverRegion {
        return resolveRegionOrFallback(discover)
            ?: error("Could not resolve discover region")
    }

    private fun regionReplacements(discover: Discover, region: DiscoverRegion? = resolveRegionOrFallback(discover)): Map<String, String> {
        if (region == null) return emptyMap()
        return mapOf(
            discover.regionCodeToken to region.code,
            discover.regionNameToken to region.name,
        )
    }

    private fun DiscoverCategory.resolveSource(replacements: Map<String, String>): DiscoverCategory {
        return transformWithReplacements(replacements, context.resources) as? DiscoverCategory ?: this
    }

    private fun ListFeed.displayTitle(fallbackTitle: String): String {
        val resources = context.resources
        val feedTitle = (title?.takeIf(String::isNotBlank) ?: fallbackTitle).tryToLocalise(resources)
        val feedSubtitle = subtitle?.takeIf(String::isNotBlank)?.tryToLocalise(resources) ?: return feedTitle
        return "$feedSubtitle: $feedTitle"
    }

    private fun DiscoverRow.rowId() = listUuid ?: id ?: title

    private fun DiscoverRow.analyticsListId(feed: ListFeed): String? = listUuid ?: feed.listId ?: id

    private fun DiscoverPodcast.toTvDiscoverPodcast(isSponsored: Boolean) = TvDiscoverPodcast(
        uuid = uuid,
        title = title.orEmpty(),
        author = author.orEmpty(),
        description = description.orEmpty(),
        isSponsored = isSponsored,
    )

    companion object {
        private const val BANNER_TYPE = "banner"
        private const val FALLBACK_REGION_CODE = "us"
        private const val SPONSORED_CATEGORY_POSITION = 5
        private const val COVER_ARTWORK_SIZE = 200
    }
}
