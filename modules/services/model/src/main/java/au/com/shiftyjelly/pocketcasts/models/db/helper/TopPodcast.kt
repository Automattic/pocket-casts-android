package au.com.shiftyjelly.pocketcasts.models.db.helper

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

data class TopPodcast(
    val uuid: String,
    val title: String,
    val author: String,
    val tintColorForLightBg: Int,
    val numberOfPlayedEpisodes: Int,
    val totalPlayedTime: Double,
) {
    fun toPodcast() = Podcast(uuid = uuid, title = title, author = author, tintColorForLightBg = tintColorForLightBg)
}
