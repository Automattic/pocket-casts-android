package au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilters

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputObject
import com.joaomgcd.taskerpluginlibrary.output.TaskerOutputVariable

@TaskerInputRoot
class InputQueryFilters
private const val OUTPUT_PREFIX = "filter_"
@TaskerOutputObject
class OutputQueryFilters constructor(
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}uuid", labelResIdName = "filter_id", htmlLabelResIdName = "filter_id_description") var id: String?,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}title", labelResIdName = "filter_title") var title: String?,
    @get:TaskerOutputVariable("${OUTPUT_PREFIX}episode_count", labelResIdName = "episode_count") var episodeCount: Int?,
)
