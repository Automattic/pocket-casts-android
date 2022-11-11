package au.com.shiftyjelly.pocketcasts.models.db.helper

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

data class ListenedCategory(
    val numberOfPodcasts: Int,
    val totalPlayedTime: Long,
    val category: String,
    val mostListenedPodcastId: String,
    val mostListenedPodcastTintColor: Int,
) {
    fun toPodcast() = Podcast(uuid = mostListenedPodcastId, tintColorForLightBg = mostListenedPodcastTintColor)
}
