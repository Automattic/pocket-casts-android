package au.com.shiftyjelly.pocketcasts.servers.cdn

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast

data class ArtworkColors(
    val background: Int = 0xFFC2C2C3.toInt(),
    val tintForLightBg: Int = 0,
    val tintForDarkBg: Int = 0,
    val fabForLightBg: Int = 0,
    val fabForDarkBg: Int = 0,
    val linkForLightBg: Int = 0,
    val linkForDarkBg: Int = 0,
    val timeDownloadedMs: Long = 0
) {

    fun copyToPodcast(podcast: Podcast): Podcast {
        podcast.backgroundColor = background
        podcast.tintColorForLightBg = tintForLightBg
        podcast.tintColorForDarkBg = tintForDarkBg
        podcast.fabColorForLightBg = fabForLightBg
        podcast.fabColorForDarkBg = fabForDarkBg
        podcast.linkColorForLightBg = linkForLightBg
        podcast.linkColorForDarkBg = linkForDarkBg
        podcast.colorLastDownloaded = timeDownloadedMs
        return podcast
    }
}
