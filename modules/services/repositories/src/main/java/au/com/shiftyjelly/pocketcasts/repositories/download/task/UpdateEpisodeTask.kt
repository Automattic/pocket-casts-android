package au.com.shiftyjelly.pocketcasts.repositories.download.task

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManagerImpl
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

class UpdateEpisodeTask(val context: Context, val params: WorkerParameters) : Worker(context, params) {
    companion object {
        const val INPUT_PODCAST_UUID = "podcast_uuid"
        const val INPUT_EPISODE_UUID = "episode_uuid"
    }

    private val moshi = Moshi.Builder().build()
    private val httpClient = OkHttpClient()
    private val retrofit = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .baseUrl(Settings.SERVER_CACHE_URL)
        .client(httpClient)
        .build()

    private val podcastUUID = inputData.getString(INPUT_PODCAST_UUID)
    private val episodeUUID = inputData.getString(INPUT_EPISODE_UUID)!!

    override fun doWork(): Result {
        try {
            if (podcastUUID == null) {
                return Result.success() // Nothing to do without a podcast
            }

            // TODO use @HiltWorker and inject these
            val serverPodcast = PodcastCacheServerManagerImpl(retrofit).getPodcastAndEpisode(podcastUUID, episodeUUID).blockingGet()
            val appDatabase = AppDatabase.getInstance(context)
            val episodeDao = appDatabase.episodeDao()

            val episode = episodeDao.findByUuid(episodeUUID)
            val serverEpisodeUrl = serverPodcast.episodes.firstOrNull()?.downloadUrl
            if (episode != null &&
                serverEpisodeUrl != null &&
                serverEpisodeUrl.isNotBlank() &&
                episode.downloadUrl != serverEpisodeUrl
            ) {
                episode.downloadUrl = serverEpisodeUrl

                episodeDao.updateDownloadUrl(serverEpisodeUrl, episode.uuid)
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
}
