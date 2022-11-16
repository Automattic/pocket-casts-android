package au.com.shiftyjelly.pocketcasts.models.db.helper

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

data class LongestEpisode(
    val title: String,
    val duration: Double,
    val podcastUuid: String,
    val podcastTitle: String,
    val tintColorForLightBg: Int,
    val tintColorForDarkBg: Int,
) {
    fun toPodcast() = Podcast(
        uuid = podcastUuid,
        title = podcastTitle,
        tintColorForLightBg = tintColorForLightBg,
        tintColorForDarkBg = tintColorForDarkBg,
    )
}
