package au.com.shiftyjelly.pocketcasts.servers.model

import android.content.res.Resources
import android.graphics.Color
import android.os.Parcelable
import androidx.annotation.ColorInt
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.models.to.BundlePaidType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.util.Date
import java.util.UUID
import kotlinx.parcelize.Parcelize

interface NetworkLoadableList {
    val title: String
    val source: String
    val type: ListType
    val displayStyle: DisplayStyle
    val expandedStyle: ExpandedStyle
    val expandedTopItemLabel: String?
    val listUuid: String?
    val curated: Boolean
    val authenticated: Boolean?

    fun transformWithReplacements(replacements: Map<String, String>, resources: Resources): NetworkLoadableList

    fun inferredId() = when {
        listUuid != null -> listUuid as String
        source.lowercase().contains(TRENDING) -> TRENDING
        source.lowercase().contains(POPULAR) -> POPULAR
        else -> NONE
    }

    val adapterId: Long
        get() = (listUuid ?: title).hashCode().toLong()

    companion object {
        const val TRENDING = "trending"
        private const val POPULAR = "popular"
        const val NONE = "none"
    }
}

@JsonClass(generateAdapter = true)
data class Discover(
    @Json(name = "layout") val layout: List<DiscoverRow>,
    @Json(name = "regions") val regions: Map<String, DiscoverRegion>,
    @Json(name = "region_code_token") val regionCodeToken: String,
    @Json(name = "region_name_token") val regionNameToken: String,
    @Json(name = "default_region_code") val defaultRegionCode: String,
)

fun List<DiscoverRow>.transformWithRegion(region: DiscoverRegion, replacements: Map<String, String>, resources: Resources): List<DiscoverRow> {
    return this.filter { it.regions.contains(region.code) }.map { it.transformWithReplacements(replacements, resources) }
}

@JsonClass(generateAdapter = true)
data class DiscoverRow(
    @Json(name = "id") val id: String?,
    @Json(name = "type") override val type: ListType,
    @Json(name = "summary_style") override val displayStyle: DisplayStyle,
    @Json(name = "expanded_style") override val expandedStyle: ExpandedStyle,
    @Json(name = "expanded_top_item_label") override val expandedTopItemLabel: String?,
    @Json(name = "title") override val title: String,
    @Json(name = "source") override val source: String,
    @Json(name = "uuid") override val listUuid: String?,
    @Json(name = "category_id") val categoryId: Int?,
    @Json(name = "regions") val regions: List<String>,
    @Json(name = "sponsored") val sponsored: Boolean = false,
    @Json(name = "curated") override val curated: Boolean = false,
    @Json(name = "authenticated") override val authenticated: Boolean? = false,
    @Json(name = "sponsored_podcasts") val sponsoredPodcasts: List<SponsoredPodcast> = emptyList(),
    @Json(name = "popular") val mostPopularCategoriesId: List<Int>?,
    @Json(name = "sponsored_ids") val sponsoredCategoryIds: List<Int>?,
) : NetworkLoadableList {

    override fun transformWithReplacements(replacements: Map<String, String>, resources: Resources): DiscoverRow {
        var newTitle = title
        var newSource = source
        var newExpandedTopItemLabel = expandedTopItemLabel

        replacements.forEach { (key, replacement) ->
            // Try to localize the title. For example 'Popular in [regionName]'
            val replacementLocalised = replacement.tryToLocalise(resources = resources)
            if (newTitle.contains(key)) {
                newTitle = newTitle.tryToLocalise(resources = resources, args = listOf(replacementLocalised))
            }
            newTitle = newTitle.replace(key, replacementLocalised)
            newSource = newSource.replace(key, replacement)
            newExpandedTopItemLabel = newExpandedTopItemLabel?.replace(key, replacementLocalised)
        }

        newExpandedTopItemLabel = newExpandedTopItemLabel?.tryToLocalise(resources)

        return DiscoverRow(
            id = id,
            type = type,
            displayStyle = displayStyle,
            expandedStyle = expandedStyle,
            expandedTopItemLabel = newExpandedTopItemLabel,
            title = newTitle,
            source = newSource,
            listUuid = listUuid,
            regions = regions,
            sponsored = sponsored,
            curated = curated,
            categoryId = categoryId,
            sponsoredPodcasts = sponsoredPodcasts,
            mostPopularCategoriesId = mostPopularCategoriesId,
            authenticated = authenticated,
            sponsoredCategoryIds = sponsoredCategoryIds,
        )
    }
}

@Parcelize
@JsonClass(generateAdapter = true)
data class SponsoredPodcast(
    @Json(name = "position") val position: Int?,
    @Json(name = "source") val source: String?,
) : Parcelable

@JsonClass(generateAdapter = true)
data class ListFeed(
    @Json(name = "title") val title: String?,
    @Json(name = "subtitle") val subtitle: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "short_description") val shortDescription: String?,
    @Json(name = "datetime") val date: String?,
    @Json(name = "podcasts") var podcasts: List<DiscoverPodcast>?,
    @Json(name = "episodes") var episodes: List<DiscoverEpisode>?,
    @Json(name = "podroll") var podroll: List<DiscoverPodcast>?,
    @Json(name = "collection_image") var collectionImageUrl: String?,
    @Json(name = "collection_rectangle_image") var collectionRectangleImageUrl: String?,
    @Json(name = "feature_image") var featureImage: String?,
    @Json(name = "header_image") var headerImageUrl: String?,
    @Json(name = "colors") var tintColors: DiscoverFeedTintColors?,
    @Json(name = "collage_images") var collageImages: List<DiscoverFeedImage>?,
    @Json(name = "web_url") var webLinkUrl: String?,
    @Json(name = "web_title") var webLinkTitle: String?,
    @Json(name = "promotion") var promotion: DiscoverPromotion?,
    @Json(name = "payment") val payment: ListPayment? = null,
    @Json(name = "paid") val paid: Boolean? = false,
    @Json(name = "author") val author: String? = null,
    @Json(name = "list_id") val listId: String? = null,
) {
    val displayList: List<Any>
        get() {
            return if (promotion != null) {
                listOf(promotion!!) + (podcasts ?: emptyList())
            } else {
                podcasts ?: emptyList()
            }
        }
}

@JsonClass(generateAdapter = true)
data class ListPayment(
    @Json(name = "bundle_uuid") val bundleUuid: String,
    @Json(name = "url") val paymentUrl: String,
    @Json(name = "paid_type") val paidTypeRaw: String,
) {
    val paidType: BundlePaidType
        get() = BundlePaidType.valueOf(paidTypeRaw.uppercase())
}

@Parcelize
@JsonClass(generateAdapter = true)
data class DiscoverFeedImage(
    @Json(name = "key") val key: String,
    @Json(name = "image_url") val imageUrl: String,
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class DiscoverFeedTintColors(
    @Json(name = "onLightBackground") val lightTintColor: String,
    @Json(name = "onDarkBackground") val darkTintColor: String,
) : Parcelable {
    fun tintColorInt(isDarkTheme: Boolean): Int? {
        return if (isDarkTheme) {
            if (darkTintColor.isBlank()) {
                null
            } else {
                Color.parseColor(darkTintColor)
            }
        } else {
            if (lightTintColor.isBlank()) {
                null
            } else {
                Color.parseColor(lightTintColor)
            }
        }
    }
}

@Parcelize
@JsonClass(generateAdapter = true)
data class DiscoverPodcast(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "title") val title: String?,
    @Json(name = "url") val url: String?,
    @Json(name = "author") val author: String?,
    @Json(name = "category") val category: String?,
    @Json(name = "description") val description: String?,
    @Json(name = "language") val language: String?,
    @Json(name = "media_type") val mediaType: String?,
    val isSubscribed: Boolean = false,
    val isSponsored: Boolean = false,
    val listId: String? = null, // for carousel sponsored podcast
    @ColorInt var color: Int = 0,
) : Parcelable {
    val adapterId: Long
        get() = UUID.nameUUIDFromBytes(uuid.toByteArray()).mostSignificantBits

    fun updateIsSubscribed(value: Boolean): DiscoverPodcast {
        return copy(isSubscribed = value)
    }
}

@Parcelize
@JsonClass(generateAdapter = true)
data class DiscoverEpisode(
    @Json(name = "uuid") val uuid: String,
    @Json(name = "title") val title: String?,
    @Json(name = "url") val url: String?,
    @Json(name = "published") val published: Date?,
    @Json(name = "duration") val duration: Int?,
    @Json(name = "file_type") val fileType: String?,
    @Json(name = "size") val size: Long?,
    @Json(name = "podcast_uuid") val podcast_uuid: String,
    @Json(name = "podcast_title") val podcast_title: String?,
    @Json(name = "type") val type: String?,
    @Json(name = "season") val season: Int?,
    @Json(name = "number") val number: Int?,
    val isPlaying: Boolean = false,
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class DiscoverPromotion(
    @Json(name = "promotion_uuid") val promotionUuid: String,
    @Json(name = "podcast_uuid") val podcastUuid: String,
    @Json(name = "title") val title: String,
    @Json(name = "description") val description: String,
    var isSubscribed: Boolean = false,
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class DiscoverRegion(
    @Json(name = "name") val name: String,
    @Json(name = "flag") val flag: String,
    @Json(name = "code") val code: String,
) : Parcelable

@JsonClass(generateAdapter = true)
data class DiscoverCategory(
    @Json(name = "id") val id: Int,
    @Json(name = "name") val name: String,
    @Json(name = "icon") val icon: String,
    @Json(name = "popularity") val popularity: Int? = null,
    @Json(name = "source") override val source: String,
    @Json(name = "source_onboarding") val onboardingRecommendationsSource: String? = null,
    override val curated: Boolean = false,
    val totalVisits: Int = 0,
    val isSponsored: Boolean? = null,
    val featuredIndex: Int? = null,
) : NetworkLoadableList {
    override val title: String
        get() = name
    override val type: ListType
        get() = ListType.PodcastList
    override val displayStyle: DisplayStyle
        get() = DisplayStyle.SmallList()
    override val expandedStyle: ExpandedStyle
        get() = ExpandedStyle.PlainList()
    override val expandedTopItemLabel: String?
        get() = null
    override val listUuid: String?
        get() = null
    override val authenticated: Boolean = false

    override fun transformWithReplacements(replacements: Map<String, String>, resources: Resources): NetworkLoadableList {
        var newTitle = title
        var newSource = source

        replacements.forEach { (key, replacement) ->
            newTitle = newTitle.replace(key, replacement)
            newSource = newSource.replace(key, replacement)
        }

        return copy(
            id = id,
            name = newTitle,
            icon = icon,
            source = newSource,
        )
    }
}
