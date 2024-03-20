package au.com.shiftyjelly.pocketcasts.repositories.shownotes

import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ImageUrlUpdate
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServer
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesChapter
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesResponse
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.models.to.DbChapter as Chapter

class ShowNotesProcessor @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val episodeManager: EpisodeManager,
    private val chapterManager: ChapterManager,
    private val service: PodcastCacheServer,
) {
    fun process(episodeUuidForChapterUrl: String, showNotes: ShowNotesResponse) {
        updateImageUrls(showNotes)
        updateChapters(showNotes)
        updateChapterFromLink(episodeUuidForChapterUrl, showNotes)
    }

    private fun updateImageUrls(showNotes: ShowNotesResponse) = scope.launch {
        val imageUrlUpdates = showNotes.podcast?.episodes?.mapNotNull { episodeShowNotes ->
            episodeShowNotes.image?.let { image -> ImageUrlUpdate(episodeShowNotes.uuid, image) }
        }
        imageUrlUpdates?.let { episodeManager.updateImageUrls(it) }
    }

    private fun updateChapters(showNotes: ShowNotesResponse) = scope.launch {
        val chapters = showNotes.podcast?.episodes?.mapNotNull { episodeShowNotes ->
            val episodeUuid = episodeShowNotes.uuid
            val chapters = episodeShowNotes.chapters?.map { chapterShowNotes -> chapterShowNotes.toChapter(episodeUuid) }
            chapters?.let { episodeUuid to it }
        }
        chapters?.forEach { (episodeUuid, chapters) ->
            chapterManager.updateChapters(episodeUuid, chapters)
        }
    }

    private fun updateChapterFromLink(episodeUuid: String, showNotes: ShowNotesResponse) = scope.launch {
        val episode = showNotes.findEpisode(episodeUuid) ?: return@launch
        val chaptersUrl = episode.chaptersUrl?.takeIf { episode.chapters.isNullOrEmpty() } ?: return@launch

        val chapters = try {
            service.getShowNotesChapters(chaptersUrl).chapters?.map { it.toChapter(episodeUuid) }
        } catch (e: Throwable) {
            Timber.e(e, "Failed to fetch chapters for episode $episodeUuid from $chaptersUrl")
            null
        } ?: return@launch

        chapterManager.updateChapters(episodeUuid, chapters)
    }

    private fun ShowNotesChapter.toChapter(episodeUuid: String) = Chapter(
        episodeUuid = episodeUuid,
        startTimeMs = startTime.seconds.inWholeMilliseconds,
        endTimeMs = endTime?.seconds?.inWholeMilliseconds,
        title = title,
        imageUrl = image,
        url = url,
    )
}
