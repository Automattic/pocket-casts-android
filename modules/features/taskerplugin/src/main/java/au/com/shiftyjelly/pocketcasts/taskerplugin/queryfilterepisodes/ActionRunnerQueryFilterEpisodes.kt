package au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilterepisodes

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.OutputQueryEpisodes
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.findPlaylist
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playlistManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.nullIfEmpty
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ActionRunnerQueryFilterEpisodes : TaskerPluginRunnerAction<InputQueryFilterEpisodes, Array<OutputQueryEpisodes>>() {

    override fun run(context: Context, input: TaskerInput<InputQueryFilterEpisodes>): TaskerPluginResult<Array<OutputQueryEpisodes>> {
        val playlistManager = context.playlistManager
        val titleOrId = input.regular.titleOrId?.trim().nullIfEmpty ?: return TaskerPluginResultSucess()

        val playlistPreviews = runBlocking { playlistManager.playlistPreviewsFlow().first() }
        val playlistPreview = playlistPreviews.find { it.uuid.equals(titleOrId, ignoreCase = true) || it.title.contains(titleOrId, ignoreCase = true) } ?: return TaskerPluginResultSucess(arrayOf())
        val playlist = runBlocking { playlistManager.findPlaylist(playlistPreview.uuid) } ?: return TaskerPluginResultSucess(arrayOf())

        val output = playlist.episodes
            .mapNotNull(PlaylistEpisode::toPodcastEpisode)
            .take(50)
            .map { OutputQueryEpisodes(it) }
            .toTypedArray()
        return TaskerPluginResultSucess(output)
    }
}
