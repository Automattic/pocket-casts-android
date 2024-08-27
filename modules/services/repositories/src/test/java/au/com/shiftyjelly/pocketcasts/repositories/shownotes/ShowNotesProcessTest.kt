package au.com.shiftyjelly.pocketcasts.repositories.shownotes

import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ImageUrlUpdate
import au.com.shiftyjelly.pocketcasts.repositories.podcast.LoadTranscriptSource
import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptsManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheService
import au.com.shiftyjelly.pocketcasts.servers.podcast.RawChaptersResponse
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesChapter
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesEpisode
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesPodcast
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesResponse
import au.com.shiftyjelly.pocketcasts.servers.podcast.ShowNotesTranscript
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import au.com.shiftyjelly.pocketcasts.models.to.DbChapter as Chapter

@OptIn(ExperimentalCoroutinesApi::class)
class ShowNotesProcessTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val episodeManager = mock<EpisodeManager>()
    private val chapterManager = mock<ChapterManager>()
    private val transcriptsManager = mock<TranscriptsManager>()
    private val service = mock<PodcastCacheService>()

    @Test
    fun `update episodes with image URLs`() = runTest(coroutineRule.testDispatcher) {
        val processor = ShowNotesProcessor(this, episodeManager, chapterManager, transcriptsManager, service)
        val episodeWithImage1 = ShowNotesEpisode(
            uuid = "episode_uuid1",
            showNotes = "show_notes1",
            image = "image1",
        )
        val episodeWithImage2 = ShowNotesEpisode(
            uuid = "episode_uuid2",
            showNotes = "show_notes2",
            image = "image2",
        )
        val episodeWithoutImage = ShowNotesEpisode(
            uuid = "episode_uuid3",
            showNotes = "show_notes3",
            image = null,
        )

        val showNotesResponse = ShowNotesResponse(
            podcast = ShowNotesPodcast(
                uuid = "podcast_uuid1",
                episodes = listOf(
                    episodeWithImage1,
                    episodeWithImage2,
                    episodeWithoutImage,
                ),
            ),
        )

        processor.process("episode_uuid1", showNotesResponse)

        val imageUrlUpdateForEpisode = { episode: ShowNotesEpisode ->
            ImageUrlUpdate(
                episodeUuid = episode.uuid,
                imageUrl = episode.image!!,
            )
        }
        val expectedUpdates = listOf(
            imageUrlUpdateForEpisode(episodeWithImage1),
            imageUrlUpdateForEpisode(episodeWithImage2),
        )

        verify(episodeManager).updateImageUrls(expectedUpdates)
    }

    @Test
    fun `update episodes with chapters`() = runTest(coroutineRule.testDispatcher) {
        val processor = ShowNotesProcessor(this, episodeManager, chapterManager, transcriptsManager, service)
        val episodeWithChapters1 = ShowNotesEpisode(
            uuid = "episode-id-1",
            chapters = listOf(
                ShowNotesChapter(
                    startTime = 0.0,
                    endTime = 20.25,
                    title = "Title 1",
                    image = "Image 1",
                    url = "Url 1",
                ),
                ShowNotesChapter(
                    startTime = 30.1,
                    title = "Title 2",
                    image = "Image 2",
                    url = "Url 2",
                ),
            ),
        )
        val episodeWithChapters2 = ShowNotesEpisode(
            uuid = "episode-id-2",
            chapters = listOf(
                ShowNotesChapter(
                    startTime = 15.5,
                ),
            ),
        )
        val showNotes = ShowNotesResponse(
            podcast = ShowNotesPodcast(
                uuid = "podcast-id",
                episodes = listOf(
                    episodeWithChapters1,
                    episodeWithChapters2,
                ),
            ),
        )

        processor.process("episode-id-1", showNotes)

        val expected1 = listOf(
            Chapter(
                episodeUuid = "episode-id-1",
                startTimeMs = 0,
                endTimeMs = 20250,
                title = "Title 1",
                imageUrl = "Image 1",
                url = "Url 1",
            ),
            Chapter(
                episodeUuid = "episode-id-1",
                startTimeMs = 30100,
                title = "Title 2",
                imageUrl = "Image 2",
                url = "Url 2",
            ),
        )
        verify(chapterManager).updateChapters("episode-id-1", expected1)

        val expected2 = listOf(
            Chapter(
                episodeUuid = "episode-id-2",
                startTimeMs = 15500,
            ),
        )
        verify(chapterManager).updateChapters("episode-id-2", expected2)
    }

    @Test
    fun `update episodes with no chapters`() = runTest(coroutineRule.testDispatcher) {
        val processor = ShowNotesProcessor(this, episodeManager, chapterManager, transcriptsManager, service)
        val episodeWithNoChapters = ShowNotesEpisode(
            uuid = "episode-id",
            chapters = emptyList(),
        )
        val showNotesResponse = ShowNotesResponse(
            podcast = ShowNotesPodcast(
                uuid = "podcast-id",
                episodes = listOf(episodeWithNoChapters),
            ),
        )

        processor.process("episode-id", showNotesResponse)

        verify(chapterManager).updateChapters("episode-id", emptyList())
    }

    @Test
    fun `update episodes without chapters`() = runTest(coroutineRule.testDispatcher) {
        val processor = ShowNotesProcessor(this, episodeManager, chapterManager, transcriptsManager, service)
        val episodeWithNoChapters = ShowNotesEpisode(
            uuid = "episode-id",
            chapters = null,
        )
        val showNotes = ShowNotesResponse(
            podcast = ShowNotesPodcast(
                uuid = "podcast-id",
                episodes = listOf(episodeWithNoChapters),
            ),
        )

        processor.process("episode-id", showNotes)

        verify(chapterManager, never()).updateChapters("episode-id", emptyList())
    }

    @Test
    fun `update episode with chapters from URL when chapters are null`() = runTest(coroutineRule.testDispatcher) {
        val processor = ShowNotesProcessor(this, episodeManager, chapterManager, transcriptsManager, service)
        val episode = ShowNotesEpisode(
            uuid = "episode-id",
            chaptersUrl = "url",
            chapters = null,
        )
        val showNotes = ShowNotesResponse(
            podcast = ShowNotesPodcast(
                uuid = "podcast-id",
                episodes = listOf(episode),
            ),
        )

        val urlChapters = listOf(
            ShowNotesChapter(
                startTime = 0.0,
                endTime = 20.25,
                title = "Title 1",
                image = "Image 1",
                url = "Url 1",
            ),
            ShowNotesChapter(
                startTime = 30.1,
                title = "Title 2",
                image = "Image 2",
                url = "Url 2",
            ),
        )
        whenever(service.getShowNotesChapters("url")).doSuspendableAnswer { RawChaptersResponse(urlChapters) }

        processor.process("episode-id", showNotes)

        val expected = listOf(
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 0,
                endTimeMs = 20250,
                title = "Title 1",
                imageUrl = "Image 1",
                url = "Url 1",
            ),
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 30100,
                title = "Title 2",
                imageUrl = "Image 2",
                url = "Url 2",
            ),
        )
        verify(chapterManager).updateChapters("episode-id", expected)
    }

    @Test
    fun `update episode with chapters from URL when chapters are empty`() = runTest(coroutineRule.testDispatcher) {
        val processor = ShowNotesProcessor(this, episodeManager, chapterManager, transcriptsManager, service)
        val episode = ShowNotesEpisode(
            uuid = "episode-id",
            chaptersUrl = "url",
            chapters = emptyList(),
        )
        val showNotes = ShowNotesResponse(
            podcast = ShowNotesPodcast(
                uuid = "podcast-id",
                episodes = listOf(episode),
            ),
        )

        val urlChapters = listOf(
            ShowNotesChapter(
                startTime = 0.0,
                endTime = 20.25,
                title = "Title 1",
                image = "Image 1",
                url = "Url 1",
            ),
            ShowNotesChapter(
                startTime = 30.1,
                title = "Title 2",
                image = "Image 2",
                url = "Url 2",
            ),
        )
        whenever(service.getShowNotesChapters("url")).doSuspendableAnswer { RawChaptersResponse(urlChapters) }

        processor.process("episode-id", showNotes)

        val expected = listOf(
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 0,
                endTimeMs = 20250,
                title = "Title 1",
                imageUrl = "Image 1",
                url = "Url 1",
            ),
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 30100,
                title = "Title 2",
                imageUrl = "Image 2",
                url = "Url 2",
            ),
        )
        verify(chapterManager).updateChapters("episode-id", expected)
    }

    @Test
    fun `use direct chapters when there is less URL chapters`() = runTest(coroutineRule.testDispatcher) {
        val processor = ShowNotesProcessor(this, episodeManager, chapterManager, transcriptsManager, service)
        val episode = ShowNotesEpisode(
            uuid = "episode-id",
            chapters = listOf(
                ShowNotesChapter(
                    startTime = 0.0,
                    title = "Title 1",
                ),
                ShowNotesChapter(
                    startTime = 1.0,
                    title = "Title 2",
                ),
            ),
            chaptersUrl = "url",
        )
        val showNotesResponse = ShowNotesResponse(
            podcast = ShowNotesPodcast(
                uuid = "podcast-id",
                episodes = listOf(episode),
            ),
        )

        val urlChapters = listOf(
            ShowNotesChapter(
                startTime = 2.0,
                title = "Title 3",
            ),
        )
        whenever(service.getShowNotesChapters("url")).doSuspendableAnswer { RawChaptersResponse(urlChapters) }

        processor.process("episode-id", showNotesResponse)

        val expected = listOf(
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 0,
                title = "Title 1",
            ),
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 1000,
                title = "Title 2",
            ),
        )
        verify(chapterManager).updateChapters("episode-id", expected)
    }

    @Test
    fun `use URL chapters when there is less direct chapters`() = runTest(coroutineRule.testDispatcher) {
        val processor = ShowNotesProcessor(this, episodeManager, chapterManager, transcriptsManager, service)
        val episode = ShowNotesEpisode(
            uuid = "episode-id",
            chapters = listOf(
                ShowNotesChapter(
                    startTime = 0.0,
                    title = "Title 1",
                ),
            ),
            chaptersUrl = "url",
        )
        val showNotesResponse = ShowNotesResponse(
            podcast = ShowNotesPodcast(
                uuid = "podcast-id",
                episodes = listOf(episode),
            ),
        )

        val urlChapters = listOf(
            ShowNotesChapter(
                startTime = 1.0,
                title = "Title 2",
            ),
            ShowNotesChapter(
                startTime = 2.0,
                title = "Title 3",
            ),
        )
        whenever(service.getShowNotesChapters("url")).doSuspendableAnswer { RawChaptersResponse(urlChapters) }

        processor.process("episode-id", showNotesResponse)

        val expected = listOf(
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 1000,
                title = "Title 2",
            ),
            Chapter(
                episodeUuid = "episode-id",
                startTimeMs = 2000,
                title = "Title 3",
            ),
        )
        verify(chapterManager).updateChapters("episode-id", expected)
    }

    @Test
    fun `fetch chapters only for specified episode`() = runTest(coroutineRule.testDispatcher) {
        val processor = ShowNotesProcessor(this, episodeManager, chapterManager, transcriptsManager, service)
        val episode1 = ShowNotesEpisode(
            uuid = "episode-id-1",
            chaptersUrl = "url1",
        )
        val episode2 = ShowNotesEpisode(
            uuid = "episode-id-2",
            chaptersUrl = "url2",
        )
        val showNotesResponse = ShowNotesResponse(
            podcast = ShowNotesPodcast(
                uuid = "podcast-id",
                episodes = listOf(episode1, episode2),
            ),
        )

        processor.process("episode-id-1", showNotesResponse)

        verifyBlocking(service) { getShowNotesChapters("url1") }
        verifyBlocking(service, never()) { getShowNotesChapters("url2") }
    }

    @Test
    fun `update episodes with transcripts having url and type`() = runTest(coroutineRule.testDispatcher) {
        val processor = ShowNotesProcessor(this, episodeManager, chapterManager, transcriptsManager, service)
        val episodeWithTranscripts1 = ShowNotesEpisode(
            uuid = "episode-id",
            transcripts = listOf(
                ShowNotesTranscript(
                    url = "Url 1",
                    type = "Type 1",
                    language = "Language 1",
                ),
                ShowNotesTranscript(
                    url = "Url 2",
                    type = "Type 2",
                    language = "Language 2",
                ),
            ),
        )
        val showNotes = ShowNotesResponse(
            podcast = ShowNotesPodcast(
                uuid = "podcast-id",
                episodes = listOf(
                    episodeWithTranscripts1,
                ),
            ),
        )

        processor.process("episode-id", showNotes)

        val expected1 = listOf(
            Transcript(
                episodeUuid = "episode-id",
                url = "Url 1",
                type = "Type 1",
                language = "Language 1",
            ),
            Transcript(
                episodeUuid = "episode-id",
                url = "Url 2",
                type = "Type 2",
                language = "Language 2",
            ),
        )
        verify(transcriptsManager).updateTranscripts("podcast-id", "episode-id", expected1, LoadTranscriptSource.DEFAULT)
    }

    @Test
    fun `update episodes with transcripts without url or type`() = runTest(coroutineRule.testDispatcher) {
        val processor = ShowNotesProcessor(this, episodeManager, chapterManager, transcriptsManager, service)
        val episodeWithTranscripts2 = ShowNotesEpisode(
            uuid = "episode-id",
            transcripts = listOf(
                ShowNotesTranscript(
                    url = "Url",
                ),
                ShowNotesTranscript(
                    type = "Type",
                ),
            ),
        )
        val showNotes = ShowNotesResponse(
            podcast = ShowNotesPodcast(
                uuid = "podcast-id",
                episodes = listOf(
                    episodeWithTranscripts2,
                ),
            ),
        )
        processor.process("episode-id", showNotes)

        val expected2 = emptyList<Transcript>()
        verify(transcriptsManager).updateTranscripts("podcast-id", "episode-id", expected2, LoadTranscriptSource.DEFAULT)
    }

    @Test
    fun `update episodes without transcripts`() = runTest(coroutineRule.testDispatcher) {
        val processor = ShowNotesProcessor(this, episodeManager, chapterManager, transcriptsManager, service)
        val episodeWithTranscripts = ShowNotesEpisode(
            uuid = "episode-id",
            transcripts = null,
        )
        val showNotes = ShowNotesResponse(
            podcast = ShowNotesPodcast(
                uuid = "podcast-id",
                episodes = listOf(
                    episodeWithTranscripts,
                ),
            ),
        )
        processor.process("episode-id", showNotes)

        val expected2 = emptyList<Transcript>()
        verify(transcriptsManager, never()).updateTranscripts("podcast-id", "episode-id", expected2, LoadTranscriptSource.DEFAULT)
    }
}
