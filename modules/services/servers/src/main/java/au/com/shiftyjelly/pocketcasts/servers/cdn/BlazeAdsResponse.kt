package au.com.shiftyjelly.pocketcasts.servers.cdn

import au.com.shiftyjelly.pocketcasts.models.entity.BlazeAd
import au.com.shiftyjelly.pocketcasts.models.type.BlazeAdLocation
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BlazePromotionsResponse(
    @Json(name = "promotions") val promotions: List<BlazePromotionResponse>?,
) {
    fun toBlazeAds(): List<BlazeAd> {
        return promotions?.map { it.toBlazeAd() } ?: emptyList()
    }
}

@JsonClass(generateAdapter = true)
data class BlazePromotionResponse(
    @Json(name = "id") val id: String,
    @Json(name = "text") val text: String,
    @Json(name = "imageURL") val imageUrl: String,
    @Json(name = "urlTitle") val urlTitle: String,
    @Json(name = "urlAndroid") val url: String,
    @Json(name = "location") val location: BlazeAdLocation,
) {
    fun toBlazeAd(): BlazeAd {
        return BlazeAd(
            id = id,
            text = text,
            imageUrl = imageUrl,
            urlTitle = urlTitle,
            url = url,
            location = location,
        )
    }
}
