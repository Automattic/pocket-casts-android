package au.com.shiftyjelly.pocketcasts.repositories.download.task

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import au.com.shiftyjelly.pocketcasts.servers.di.ServersModule
import okhttp3.OkHttpClient

class UpdateShowNotesTask(val context: Context, val params: WorkerParameters) : Worker(context, params) {
    companion object {
        const val INPUT_EPISODE_UUID = "episode_uuid"
    }

    private val httpClient: OkHttpClient = ServersModule.getShowNotesClient(context)
    private val episodeUUID = inputData.getString(INPUT_EPISODE_UUID)!!

    override fun doWork(): Result {
        return try {
            val serverShowNotes = ServerShowNotesManager(httpClient)
            serverShowNotes.cacheShowNotes(episodeUUID).onErrorComplete().blockingAwait()
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
