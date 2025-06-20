package au.com.shiftyjelly.pocketcasts.repositories.shownotes

import au.com.shiftyjelly.pocketcasts.models.db.dao.TranscriptDao
import au.com.shiftyjelly.pocketcasts.models.entity.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.DbChapter
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ImageUrlUpdate
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheService
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesChapter
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesResponse
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesTranscript
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class ShowNotesProcessor @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val chapterManager: ChapterManager,
    private val transcriptDao: TranscriptDao,
    private val service: PodcastCacheService,
) {
    suspend fun process(
        episodeUuid: String,
        showNotes: ShowNotesResponse,
    ) = coroutineScope {
        updateImageUrls(showNotes)
        updateChapters(episodeUuid, showNotes)
        updateChapterFromLink(episodeUuid, showNotes)
        updateTranscripts(episodeUuid, showNotes)
    }

    private suspend fun updateImageUrls(showNotes: ShowNotesResponse) {
        val imageUrlUpdates = showNotes.podcast?.episodes?.mapNotNull { episodeShowNotes ->
            episodeShowNotes.image?.let { image -> ImageUrlUpdate(episodeShowNotes.uuid, image) }
        }
        imageUrlUpdates?.let { episodeManager.updateImageUrls(it) }
    }

    private suspend fun updateChapters(episodeUuid: String, showNotes: ShowNotesResponse) {
        val chapters = showNotes.podcast?.episodes
            ?.filter { it.uuid != episodeUuid } // We handle requesting episode with URL link
            ?.mapNotNull { episodeShowNotes ->
                val mappingEpisodeId = episodeShowNotes.uuid
                val chapters = episodeShowNotes.chapters?.mapIndexedNotNull { index, chapter -> chapter.toDbChapter(index, mappingEpisodeId) }
                chapters?.let { episodeShowNotes.uuid to it }
            }
        chapters?.forEach { (episodeUuid, chapters) ->
            chapterManager.updateChapters(episodeUuid, chapters)
        }
    }

    private suspend fun updateChapterFromLink(episodeUuid: String, showNotes: ShowNotesResponse) {
        val episode = showNotes.findEpisode(episodeUuid) ?: return

        val podcastIndexChapters = try {
            episode.chaptersUrl?.let { url ->
                service.getShowNotesChapters(url).chapters?.mapIndexedNotNull { index, chapter -> chapter.toDbChapter(index, episodeUuid) }
            }
        } catch (e: Throwable) {
            Timber.e(e, "Failed to fetch chapters for episode $episodeUuid from ${episode.chaptersUrl}")
            null
        }
        val podLoveChapters = episode.chapters?.mapIndexedNotNull { index, chapter -> chapter.toDbChapter(index, episodeUuid) }

        val newChapters = if (podcastIndexChapters != null && podLoveChapters != null) {
            maxOf(podcastIndexChapters, podLoveChapters) { a, b -> a.size.compareTo(b.size) }
        } else {
            podcastIndexChapters ?: podLoveChapters
        }
        newChapters?.let { chapterManager.updateChapters(episodeUuid, it) }
    }

    private suspend fun updateTranscripts(episodeUuid: String, showNotes: ShowNotesResponse) {
        val transcripts = showNotes.findTranscripts(episodeUuid)
        if (transcripts != null) {
            transcriptDao.replaceAll(transcripts)
        }
    }

    private fun ShowNotesChapter.toDbChapter(index: Int, episodeUuid: String) = if (useInTableOfContents != false) {
        DbChapter(
            index = index,
            episodeUuid = episodeUuid,
            startTimeMs = startTime.seconds.inWholeMilliseconds,
            endTimeMs = endTime?.seconds?.inWholeMilliseconds,
            title = title,
            imageUrl = image,
            url = url,
            isEmbedded = false,
        )
    } else {
        null
    }
}

internal fun ShowNotesResponse.findTranscripts(episodeUuid: String): List<Transcript>? {
    val episode = podcast?.episodes?.firstOrNull { it.uuid == episodeUuid } ?: return null
    val transcripts = episode.transcripts?.mapNotNull { it.toTranscript(episodeUuid, isGenerated = false) }.orEmpty()
    val pocketCastsTranscripts = episode.pocketCastsTranscripts?.mapNotNull { it.toTranscript(episodeUuid, isGenerated = true) }.orEmpty()
    return transcripts + pocketCastsTranscripts
}

private fun ShowNotesTranscript.toTranscript(
    episodeUuid: String,
    isGenerated: Boolean,
) = if (url != null && type != null) {
    Transcript(
        episodeUuid = episodeUuid,
        url = requireNotNull(url),
        type = requireNotNull(type),
        isGenerated = isGenerated,
        language = language,
    )
} else {
    null
}
