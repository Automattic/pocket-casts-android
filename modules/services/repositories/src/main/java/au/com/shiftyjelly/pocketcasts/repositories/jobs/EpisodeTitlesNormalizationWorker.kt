package au.com.shiftyjelly.pocketcasts.repositories.jobs

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import au.com.shiftyjelly.pocketcasts.models.db.dao.EpisodeDao
import au.com.shiftyjelly.pocketcasts.utils.extensions.unidecode
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay

@HiltWorker
class EpisodeTitlesNormalizationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val episodeDao: EpisodeDao,
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result {
        markEpisodesForNormalization()
        normalizeEpisodeTitles()
        return Result.success()
    }

    private tailrec suspend fun markEpisodesForNormalization() {
        val episodes = episodeDao
            .getEpisodesWithCleanTitleNotEqual(NORMALIZATION_TOKEN)
            .map { it.copy(title = NORMALIZATION_TOKEN) }
        if (episodes.isEmpty()) {
            return
        }
        episodeDao.updateAllCleanTitles(episodes)
        delay(1.seconds) // Give some space for other app tasks
        markEpisodesForNormalization()
    }

    private tailrec suspend fun normalizeEpisodeTitles() {
        val episodes = episodeDao
            .getEpisodesWithCleanTitleEqual(NORMALIZATION_TOKEN)
            .map { it.copy(title = it.title.unidecode()) }
        if (episodes.isEmpty()) {
            return
        }
        episodeDao.updateAllCleanTitles(episodes)
        delay(1.seconds) // Give some space for other app tasks
        normalizeEpisodeTitles()
    }

    companion object {
        // We're using the letter "ó" as a token as it is guaranteed to not be present
        // in the normalized name. It can be any letter that is cleaned up by String.unidecode().
        const val NORMALIZATION_TOKEN = "__ó__"

        private const val WORKER_TAG = "pocket_casts_episode_titles_normalization_worker_tag"

        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<EpisodeTitlesNormalizationWorker>()
                .addTag(WORKER_TAG)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(WORKER_TAG, ExistingWorkPolicy.KEEP, request)
        }
    }
}
