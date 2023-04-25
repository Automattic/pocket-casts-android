package au.com.shiftyjelly.pocketcasts.taskerplugin.base

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

private const val OUTPUT_PREFIX = "episode_"

class OutputQueryEpisodes(
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}uuid", labelResIdName = "episode_id", htmlLabelResIdName = "episode_id_description") var id: String?,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}title", labelResIdName = "episode_title") var title: String?,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}download_url", labelResIdName = "download_url") var downloadUrl: String?,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}playing_status", labelResIdName = "playing_status", htmlLabelResIdName = "playing_status_explained") var playingStatus: Int?,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}date_published", labelResIdName = "date_published") var datePublished: String?,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}duration", labelResIdName = "filters_duration") var duration: Double?,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}played_percentage", labelResIdName = "played_percentage") var playedPercentage: Int?,
) {
    constructor(episode: PodcastEpisode) : this(episode.uuid, episode.title.formattedForTasker, episode.downloadUrl, episode.playingStatus.ordinal, episode.publishedDate.formattedForTasker, episode.duration, episode.playedPercentage)
}
