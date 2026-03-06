package au.com.shiftyjelly.pocketcasts.repositories.shownotes

import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.servers.ShowNotesServiceManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptService
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.CacheControl
import timber.log.Timber

class ShowNotesManager @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val showNotesServiceManager: ShowNotesServiceManager,
    private val showNotesProcessor: ShowNotesProcessor,
    private val transcriptDao: TranscriptDao,
    private val transcriptsService: TranscriptService,
) {

    fun loadShowNotesFlow(podcastUuid: String, episodeUuid: String): Flow<ShowNotesState> = showNotesServiceManager.loadShowNotesFlow(
        podcastUuid = podcastUuid,
        episodeUuid = episodeUuid,
        processShowNotes = {
            scope.launch { showNotesProcessor.process(episodeUuid, it) }
        },
    ).also { Timber.tag("LOG_TAG").i("Load show notes flow") }

    suspend fun loadShowNotes(podcastUuid: String, episodeUuid: String): ShowNotesState = showNotesServiceManager.loadShowNotes(
        podcastUuid = podcastUuid,
        episodeUuid = episodeUuid,
        processShowNotes = {
            scope.launch { showNotesProcessor.process(episodeUuid, it) }
        },
    ).also { Timber.tag("LOG_TAG").i("Load show notes") }

    suspend fun downloadToCacheShowNotes(podcastUuid: String, episodeUuid: String) {
        showNotesServiceManager.downloadToCacheShowNotes(
            podcastUuid = podcastUuid,
            processShowNotes = {
                scope.launch {
                    showNotesProcessor.process(episodeUuid, it)
                    val transcripts = transcriptDao.observeTranscripts(episodeUuid).first()
                    for (transcript in transcripts) {
                        runCatching { transcriptsService.getTranscriptOrThrow(transcript.url, CacheControl.FORCE_NETWORK) }
                    }
                }
            },
        ).also { Timber.tag("LOG_TAG").i("Load show notes to cache") }
    }
}
