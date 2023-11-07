package au.com.shiftyjelly.pocketcasts.models.db.helper

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

data class TopPodcast(
    val episodeId: String,
    val uuid: String,
    val title: String,
    val author: String,
    val tintColorForLightBg: Int,
    val tintColorForDarkBg: Int,
    val numberOfPlayedEpisodes: Int,
    val totalPlayedTime: Double,
    val weighted: Double,
) {
    fun toPodcast() = Podcast(
        uuid = uuid,
        title = title,
        author = author,
        tintColorForLightBg = tintColorForLightBg,
        tintColorForDarkBg = tintColorForDarkBg
    )
}
