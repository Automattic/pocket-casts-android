package au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcastepisodes

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class InputQueryPodcastEpisodes @JvmOverloads constructor(
    @field:TaskerInputField("title_or_id", labelResIdName = "podcast_id_or_title") var titleOrId: String? = null
)
