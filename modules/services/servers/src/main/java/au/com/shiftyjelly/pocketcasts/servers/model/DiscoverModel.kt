package au.com.shiftyjelly.pocketcasts.servers.model

import android.content.res.Resources
import android.graphics.Color
import android.os.Parcelable
import androidx.annotation.ColorInt
import au.com.shiftyjelly.pocketcasts.localization.helper.tryToLocalise
import au.com.shiftyjelly.pocketcasts.models.to.BundlePaidType
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import kotlinx.parcelize.Parcelize
import java.util.Date

interface NetworkLoadableList {
    val title: String
    val source: String
    val type: ListType
    val displayStyle: DisplayStyle
    val expandedStyle: ExpandedStyle
    val expandedTopItemLabel: String?
    val listUuid: String?
    val curated: Boolean

    fun transformWithReplacements(replacements: Map<String, String>, resources: Resources): NetworkLoadableList

    fun inferredId() = when {
        listUuid != null -> listUuid as String
        source.lowercase().contains(TRENDING) -> TRENDING
        source.lowercase().contains(POPULAR) -> POPULAR
        else -> NONE
    }

    companion object {
        private const val TRENDING = "trending"
        private const val POPULAR = "popular"
        private const val NONE = "none"
    }
}

@JsonClass(generateAdapter = true)
data class Discover(
    @field:Json(name = "layout") val layout: List<DiscoverRow>,
    @field:Json(name = "regions") val regions: Map<String, DiscoverRegion>,
    @field:Json(name = "region_code_token") val regionCodeToken: String,
    @field:Json(name = "region_name_token") val regionNameToken: String,
    @field:Json(name = "default_region_code") val defaultRegionCode: String
)

fun List<DiscoverRow>.transformWithRegion(region: DiscoverRegion, replacements: Map<String, String>, resources: Resources): List<DiscoverRow> {
    return this.filter { it.regions.contains(region.code) }.map { it.transformWithReplacements(replacements, resources) }
}

@JsonClass(generateAdapter = true)
data class DiscoverRow(
    @field:Json(name = "id") val id: String?,
    @field:Json(name = "type") override val type: ListType,
    @field:Json(name = "summary_style") override val displayStyle: DisplayStyle,
    @field:Json(name = "expanded_style") override val expandedStyle: ExpandedStyle,
    @field:Json(name = "expanded_top_item_label") override val expandedTopItemLabel: String?,
    @field:Json(name = "title") override val title: String,
    @field:Json(name = "source") override val source: String,
    @field:Json(name = "uuid") override val listUuid: String?,
    @field:Json(name = "regions") val regions: List<String>,
    @field:Json(name = "sponsored") val sponsored: Boolean = false,
    @field:Json(name = "curated") override val curated: Boolean = false
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

        return DiscoverRow(id, type, displayStyle, expandedStyle, newExpandedTopItemLabel, newTitle, newSource, listUuid, regions, sponsored, curated)
    }
}

@JsonClass(generateAdapter = true)
data class ListFeed(
    @field:Json(name = "title") val title: String?,
    @field:Json(name = "subtitle") val subtitle: String?,
    @field:Json(name = "description") val description: String?,
    @field:Json(name = "datetime") val date: String?,
    @field:Json(name = "podcasts") var podcasts: List<DiscoverPodcast>?,
    @field:Json(name = "episodes") var episodes: List<DiscoverEpisode>?,
    @field:Json(name = "collection_image") var collectionImageUrl: String?,
    @field:Json(name = "header_image") var headerImageUrl: String?,
    @field:Json(name = "colors") var tintColors: DiscoverFeedTintColors?,
    @field:Json(name = "collage_images") var collageImages: List<DiscoverFeedImage>?,
    @field:Json(name = "web_url") var webLinkUrl: String?,
    @field:Json(name = "web_title") var webLinkTitle: String?,
    @field:Json(name = "promotion") var promotion: DiscoverPromotion?,
    @field:Json(name = "payment") val payment: ListPayment? = null,
    @field:Json(name = "paid") val paid: Boolean? = false,
    @field:Json(name = "author") val author: String? = null
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
    @field:Json(name = "bundle_uuid") val bundleUuid: String,
    @field:Json(name = "url") val paymentUrl: String,
    @field:Json(name = "paid_type") val paidTypeRaw: String
) {
    val paidType: BundlePaidType
        get() = BundlePaidType.valueOf(paidTypeRaw.uppercase())
}

@Parcelize
@JsonClass(generateAdapter = true)
data class DiscoverFeedImage(
    @field:Json(name = "key") val key: String,
    @field:Json(name = "image_url") val imageUrl: String
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class DiscoverFeedTintColors(
    @field:Json(name = "onLightBackground") val lightTintColor: String,
    @field:Json(name = "onDarkBackground") val darkTintColor: String
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
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "title") val title: String?,
    @field:Json(name = "url") val url: String?,
    @field:Json(name = "author") val author: String?,
    @field:Json(name = "category") val category: String?,
    @field:Json(name = "description") val description: String?,
    @field:Json(name = "language") val language: String?,
    @field:Json(name = "media_type") val mediaType: String?,
    val isSubscribed: Boolean = false,
    @ColorInt var color: Int = 0
) : Parcelable {
    fun updateIsSubscribed(value: Boolean): DiscoverPodcast {
        return copy(isSubscribed = value)
    }
}

@Parcelize
@JsonClass(generateAdapter = true)
data class DiscoverEpisode(
    @field:Json(name = "uuid") val uuid: String,
    @field:Json(name = "title") val title: String?,
    @field:Json(name = "url") val url: String?,
    @field:Json(name = "published") val published: Date?,
    @field:Json(name = "duration") val duration: Int?,
    @field:Json(name = "file_type") val fileType: String?,
    @field:Json(name = "size") val size: Long?,
    @field:Json(name = "podcast_uuid") val podcast_uuid: String,
    @field:Json(name = "podcast_title") val podcast_title: String?,
    @field:Json(name = "type") val type: String?,
    @field:Json(name = "season") val season: Int?,
    @field:Json(name = "number") val number: Int?,
    val isPlaying: Boolean = false,
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class DiscoverPromotion(
    @field:Json(name = "promotion_uuid") val promotionUuid: String,
    @field:Json(name = "podcast_uuid") val podcastUuid: String,
    @field:Json(name = "title") val title: String,
    @field:Json(name = "description") val description: String,
    var isSubscribed: Boolean = false
) : Parcelable

@Parcelize
@JsonClass(generateAdapter = true)
data class DiscoverRegion(
    @field:Json(name = "name") val name: String,
    @field:Json(name = "flag") val flag: String,
    @field:Json(name = "code") val code: String
) : Parcelable

@JsonClass(generateAdapter = true)
data class DiscoverCategory(
    @field:Json(name = "id") val id: Int,
    @field:Json(name = "name") val name: String,
    @field:Json(name = "icon") val icon: String,
    @field:Json(name = "source") override val source: String,
    override val curated: Boolean = false
) : NetworkLoadableList {
    override val title: String
        get() = name
    override val type: ListType
        get() = ListType.PodcastList()
    override val displayStyle: DisplayStyle
        get() = DisplayStyle.SmallList()
    override val expandedStyle: ExpandedStyle
        get() = ExpandedStyle.PlainList()
    override val expandedTopItemLabel: String?
        get() = null
    override val listUuid: String?
        get() = null

    override fun transformWithReplacements(replacements: Map<String, String>, resources: Resources): NetworkLoadableList {
        var newTitle = title
        var newSource = source

        replacements.forEach { (key, replacement) ->
            newTitle = newTitle.replace(key, replacement)
            newSource = newSource.replace(key, replacement)
        }

        return DiscoverCategory(id, newTitle, icon, newSource)
    }
}
