package au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcasts

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerInputRoot
class InputQueryPodcasts
private const val OUTPUT_PREFIX = "podcast_"
@TaskerOutputObject
class OutputQueryPodcasts constructor(
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}uuid", labelResIdName = "podcast_id", htmlLabelResIdName = "podcast_id_description") var id: String?,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}title", labelResIdName = "podcast_title") var title: String?,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}author", labelResIdName = "author") var author: String,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}url", labelResIdName = "podcast_url") var podcastUrl: String?,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}thumbnail_url", labelResIdName = "thumbnail_url") var thumbnailUrl: String?,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}category", labelResIdName = "category") var podcastCategory: String?,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}added_date", labelResIdName = "added_date") var addedDate: String?,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}latest_episode_date", labelResIdName = "latest_episode_date") var latestEpisodeDate: String?
)
