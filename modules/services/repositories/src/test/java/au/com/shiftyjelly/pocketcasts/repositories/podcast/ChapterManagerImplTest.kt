package au.com.shiftyjelly.pocketcasts.repositories.podcast

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.db.dao.ChapterDao
import au.com.shiftyjelly.pocketcasts.models.entity.ChapterIndices
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Chapter
import au.com.shiftyjelly.pocketcasts.models.to.ChapterOrigin
import au.com.shiftyjelly.pocketcasts.models.to.Chapters
import au.com.shiftyjelly.pocketcasts.models.to.DbChapter
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.Lazy
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ChapterManagerImplTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val chapterDao = mock<ChapterDao>()
    private val episodeManager = mock<EpisodeManager>()

    // Non-Phone platform keeps alignment off, so these tests observe the raw reference timeline.
    private val chapterManager = ChapterManagerImpl(
        chapterDao = chapterDao,
        episodeManager = episodeManager,
        fingerprintTimingManager = Lazy { mock<FingerprintTimingManager>() },
        appPlatform = AppPlatform.Automotive,
    )

    @Test
    fun `observe no chapters`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.001)

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(emptyList()))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            assertEquals(Chapters(emptyList()), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe single chapter`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.001)
        val dbChapter = DbChapter(
            index = 0,
            episodeUuid = "id",
            startTimeMs = 0,
            endTimeMs = 1,
            title = "Title",
            imageUrl = "Image",
            url = "https://pocketcasts.com/",
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(listOf(dbChapter)))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = Chapter(
                index = 0,
                uiIndex = 1,
                title = "Title",
                startTime = 0.milliseconds,
                endTime = 1.milliseconds,
                url = "https://pocketcasts.com/".toHttpUrl(),
                imagePath = "Image",
                selected = true,
            )
            assertEquals(Chapters(listOf(expected)), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapter origin`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.001)
        val dbChapter = DbChapter(
            index = 0,
            episodeUuid = "id",
            startTimeMs = 0,
            endTimeMs = 1,
            title = "Title",
            origin = ChapterOrigin.PodcastIndex,
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(listOf(dbChapter)))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            assertEquals(ChapterOrigin.PodcastIndex, awaitItem().origin)

            awaitComplete()
        }
    }

    @Test
    fun `observe multiple chapters`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.003)
        val dbChapters = listOf(
            DbChapter(
                index = 0,
                episodeUuid = "id",
                startTimeMs = 0,
                endTimeMs = 1,
                title = "Title 1",
            ),
            DbChapter(
                index = 1,
                episodeUuid = "id",
                startTimeMs = 1,
                endTimeMs = 2,
                title = "Title 2",
            ),
            DbChapter(
                index = 3,
                episodeUuid = "id",
                startTimeMs = 2,
                endTimeMs = 3,
                title = "Title 3",
            ),
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(dbChapters))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = listOf(
                Chapter(
                    index = 0,
                    uiIndex = 1,
                    title = "Title 1",
                    startTime = 0.milliseconds,
                    endTime = 1.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 1,
                    uiIndex = 2,
                    title = "Title 2",
                    startTime = 1.milliseconds,
                    endTime = 2.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 3,
                    uiIndex = 3,
                    title = "Title 3",
                    startTime = 2.milliseconds,
                    endTime = 3.milliseconds,
                    selected = true,
                ),
            )
            assertEquals(Chapters(expected), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapter without title`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.001)
        val dbChapter = DbChapter(
            index = 0,
            episodeUuid = "id",
            startTimeMs = 0,
            endTimeMs = 1,
            title = null,
            imageUrl = "Image",
            url = "https://pocketcasts.com/",
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(listOf(dbChapter)))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = Chapter(
                index = 0,
                uiIndex = 1,
                title = "",
                startTime = 0.milliseconds,
                endTime = 1.milliseconds,
                url = "https://pocketcasts.com/".toHttpUrl(),
                imagePath = "Image",
                selected = true,
            )
            assertEquals(Chapters(listOf(expected)), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapter without image URL`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.001)
        val dbChapter = DbChapter(
            index = 0,
            episodeUuid = "id",
            startTimeMs = 0,
            endTimeMs = 1,
            title = "Title",
            imageUrl = null,
            url = "https://pocketcasts.com/",
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(listOf(dbChapter)))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = Chapter(
                index = 0,
                uiIndex = 1,
                title = "Title",
                startTime = 0.milliseconds,
                endTime = 1.milliseconds,
                url = "https://pocketcasts.com/".toHttpUrl(),
                imagePath = null,
                selected = true,
            )
            assertEquals(Chapters(listOf(expected)), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapter without URL`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.001)
        val dbChapter = DbChapter(
            index = 0,
            episodeUuid = "id",
            startTimeMs = 0,
            endTimeMs = 1,
            title = "Title",
            imageUrl = "Image",
            url = null,
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(listOf(dbChapter)))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = Chapter(
                index = 0,
                uiIndex = 1,
                title = "Title",
                startTime = 0.milliseconds,
                endTime = 1.milliseconds,
                url = null,
                imagePath = "Image",
                selected = true,
            )
            assertEquals(Chapters(listOf(expected)), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapter with invalid URL`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.001)
        val dbChapter = DbChapter(
            index = 0,
            episodeUuid = "id",
            startTimeMs = 0,
            endTimeMs = 1,
            title = "Title",
            imageUrl = "Image",
            url = "Invalid Url",
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(listOf(dbChapter)))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = Chapter(
                index = 0,
                uiIndex = 1,
                title = "Title",
                startTime = 0.milliseconds,
                endTime = 1.milliseconds,
                url = null,
                imagePath = "Image",
                selected = true,
            )
            assertEquals(Chapters(listOf(expected)), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe deselected chapters`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.004, deselectedChapters = ChapterIndices(listOf(0, 3)))
        val dbChapters = listOf(
            DbChapter(
                index = 0,
                episodeUuid = "id",
                startTimeMs = 0,
                endTimeMs = 1,
                title = "Title 1",
            ),
            DbChapter(
                index = 1,
                episodeUuid = "id",
                startTimeMs = 1,
                endTimeMs = 2,
                title = "Title 2",
            ),
            DbChapter(
                index = 2,
                episodeUuid = "id",
                startTimeMs = 2,
                endTimeMs = 3,
                title = "Title 3",
            ),
            DbChapter(
                index = 3,
                episodeUuid = "id",
                startTimeMs = 3,
                endTimeMs = 4,
                title = "Title 4",
            ),
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(dbChapters))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = listOf(
                Chapter(
                    index = 0,
                    uiIndex = 1,
                    title = "Title 1",
                    startTime = 0.milliseconds,
                    endTime = 1.milliseconds,
                    selected = false,
                ),
                Chapter(
                    index = 1,
                    uiIndex = 2,
                    title = "Title 2",
                    startTime = 1.milliseconds,
                    endTime = 2.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 2,
                    uiIndex = 3,
                    title = "Title 3",
                    startTime = 2.milliseconds,
                    endTime = 3.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 3,
                    uiIndex = 4,
                    title = "Title 4",
                    startTime = 3.milliseconds,
                    endTime = 4.milliseconds,
                    selected = false,
                ),
            )
            assertEquals(Chapters(expected), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapters with unknown end timestamps`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.003)
        val dbChapters = listOf(
            DbChapter(
                index = 0,
                episodeUuid = "id",
                startTimeMs = 0,
                endTimeMs = null,
                title = "Title 1",
            ),
            DbChapter(
                index = 1,
                episodeUuid = "id",
                startTimeMs = 1,
                endTimeMs = 2,
                title = "Title 2",
            ),
            DbChapter(
                index = 2,
                episodeUuid = "id",
                startTimeMs = 2,
                endTimeMs = null,
                title = "Title 3",
            ),
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(dbChapters))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = listOf(
                Chapter(
                    index = 0,
                    uiIndex = 1,
                    title = "Title 1",
                    startTime = 0.milliseconds,
                    endTime = 1.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 1,
                    uiIndex = 2,
                    title = "Title 2",
                    startTime = 1.milliseconds,
                    endTime = 2.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 2,
                    uiIndex = 3,
                    title = "Title 3",
                    startTime = 2.milliseconds,
                    endTime = 3.milliseconds,
                    selected = true,
                ),
            )
            assertEquals(Chapters(expected), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapters with invalid timestamps`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.003)
        val dbChapters = listOf(
            DbChapter(
                index = 0,
                episodeUuid = "id",
                startTimeMs = 0,
                endTimeMs = 3, // end time is larger than next start time
                title = "Title 1",
            ),
            DbChapter(
                index = 1,
                episodeUuid = "id",
                startTimeMs = 2,
                endTimeMs = 3,
                title = "Title 2",
            ),
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(dbChapters))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = listOf(
                Chapter(
                    index = 0,
                    uiIndex = 1,
                    title = "Title 1",
                    startTime = 0.milliseconds,
                    endTime = 2.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 1,
                    uiIndex = 2,
                    title = "Title 2",
                    startTime = 2.milliseconds,
                    endTime = 3.milliseconds,
                    selected = true,
                ),
            )
            assertEquals(Chapters(expected), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapter selection changes`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.001)
        val episodesFlow = MutableStateFlow(episode)
        val dbChapter = DbChapter(
            index = 0,
            episodeUuid = "id",
            startTimeMs = 0,
            endTimeMs = 1,
            title = "Title",
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(listOf(dbChapter)))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(episodesFlow)

        chapterManager.observerChaptersForEpisode("id").test {
            val expectedSelected = Chapter(
                index = 0,
                uiIndex = 1,
                title = "Title",
                startTime = 0.milliseconds,
                endTime = 1.milliseconds,
                selected = true,
            )
            assertEquals(Chapters(listOf(expectedSelected)), awaitItem())

            episodesFlow.value = episode.copy(deselectedChapters = ChapterIndices(listOf(0)))

            val expectedNotSelected = expectedSelected.copy(selected = false)
            assertEquals(Chapters(listOf(expectedNotSelected)), awaitItem())

            cancel()
        }
    }

    @Test
    fun `observe chapters with 0 duration`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.003)
        val dbChapters = listOf(
            DbChapter(
                index = 0,
                episodeUuid = "id",
                startTimeMs = 0,
                endTimeMs = 0,
                title = "Title 1",
            ),
            DbChapter(
                index = 1,
                episodeUuid = "id",
                startTimeMs = 1,
                endTimeMs = 1,
                title = "Title 2",
            ),
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(dbChapters))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = listOf(
                Chapter(
                    index = 0,
                    uiIndex = 1,
                    title = "Title 1",
                    startTime = 0.milliseconds,
                    endTime = 1.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 1,
                    uiIndex = 2,
                    title = "Title 2",
                    startTime = 1.milliseconds,
                    endTime = 3.milliseconds,
                    selected = true,
                ),
            )
            assertEquals(Chapters(expected), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `observe chapters with negative duration`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.003)
        val dbChapters = listOf(
            DbChapter(
                index = 0,
                episodeUuid = "id",
                startTimeMs = 0,
                endTimeMs = -2,
                title = "Title 1",
            ),
            DbChapter(
                index = 1,
                episodeUuid = "id",
                startTimeMs = 1,
                endTimeMs = 1,
                title = "Title 2",
            ),
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(dbChapters))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = listOf(
                Chapter(
                    index = 0,
                    uiIndex = 1,
                    title = "Title 1",
                    startTime = 0.milliseconds,
                    endTime = 1.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 1,
                    uiIndex = 2,
                    title = "Title 2",
                    startTime = 1.milliseconds,
                    endTime = 3.milliseconds,
                    selected = true,
                ),
            )
            assertEquals(Chapters(expected), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `filter chapters with no duration`() = runBlocking {
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.003)
        val dbChapters = listOf(
            DbChapter(
                index = 0,
                episodeUuid = "id",
                startTimeMs = 0,
                endTimeMs = 0,
                title = "Title 1",
            ),
            DbChapter(
                index = 1,
                episodeUuid = "id",
                startTimeMs = 0,
                endTimeMs = 0,
                title = "Title 2",
            ),
            DbChapter(
                index = 2,
                episodeUuid = "id",
                startTimeMs = 1,
                endTimeMs = 1,
                title = "Title 3",
            ),
            DbChapter(
                index = 3,
                episodeUuid = "id",
                startTimeMs = 2,
                endTimeMs = 2,
                title = "Title 4",
            ),
            DbChapter(
                index = 4,
                episodeUuid = "id",
                startTimeMs = 2,
                endTimeMs = 2,
                title = "Title 5",
            ),
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(dbChapters))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            val expected = listOf(
                Chapter(
                    index = 1,
                    uiIndex = 1,
                    title = "Title 2",
                    startTime = 0.milliseconds,
                    endTime = 1.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 2,
                    uiIndex = 2,
                    title = "Title 3",
                    startTime = 1.milliseconds,
                    endTime = 2.milliseconds,
                    selected = true,
                ),
                Chapter(
                    index = 4,
                    uiIndex = 3,
                    title = "Title 5",
                    startTime = 2.milliseconds,
                    endTime = 3.milliseconds,
                    selected = true,
                ),
            )
            assertEquals(Chapters(expected), awaitItem())

            awaitComplete()
        }
    }

    @Test
    fun `align translates generated chapter times`() {
        val chapters = Chapters(
            listOf(
                Chapter(title = "A", startTime = 0.seconds, endTime = 10.seconds, index = 0, uiIndex = 1, origin = ChapterOrigin.Generated),
                Chapter(title = "B", startTime = 10.seconds, endTime = 20.seconds, index = 1, uiIndex = 2, origin = ChapterOrigin.Generated),
            ),
        )

        // Simulate a 30s ad inserted before playback: every reference time shifts later by 30s.
        val aligned = ChapterManagerImpl.alignGeneratedChapters(chapters) { it + 30.seconds }

        assertEquals(
            Chapters(
                listOf(
                    Chapter(title = "A", startTime = 30.seconds, endTime = 40.seconds, index = 0, uiIndex = 1, origin = ChapterOrigin.Generated),
                    Chapter(title = "B", startTime = 40.seconds, endTime = 50.seconds, index = 1, uiIndex = 2, origin = ChapterOrigin.Generated),
                ),
            ),
            aligned,
        )
    }

    @Test
    fun `align leaves embedded chapters untouched`() {
        val chapters = Chapters(
            listOf(
                Chapter(title = "Embedded", startTime = 0.seconds, endTime = 10.seconds, index = 0, uiIndex = 1, origin = ChapterOrigin.Unknown),
                Chapter(title = "Generated", startTime = 10.seconds, endTime = 20.seconds, index = 1, uiIndex = 2, origin = ChapterOrigin.Generated),
            ),
        )

        val aligned = ChapterManagerImpl.alignGeneratedChapters(chapters) { it + 30.seconds }

        assertEquals(0.seconds, aligned[0].startTime)
        assertEquals(10.seconds, aligned[0].endTime)
        assertEquals(40.seconds, aligned[1].startTime)
        assertEquals(50.seconds, aligned[1].endTime)
    }

    @Test
    fun `align keeps original time when mapping returns null`() {
        val chapters = Chapters(
            listOf(
                Chapter(title = "A", startTime = 5.seconds, endTime = 15.seconds, index = 0, uiIndex = 1, origin = ChapterOrigin.Generated),
            ),
        )

        val aligned = ChapterManagerImpl.alignGeneratedChapters(chapters) { null }

        assertEquals(5.seconds, aligned[0].startTime)
        assertEquals(15.seconds, aligned[0].endTime)
    }

    @Test
    fun `align drops collapsed chapters and re-derives uiIndex`() {
        val chapters = Chapters(
            listOf(
                Chapter(title = "A", startTime = 0.seconds, endTime = 10.seconds, index = 0, uiIndex = 1, origin = ChapterOrigin.Generated),
                Chapter(title = "B", startTime = 10.seconds, endTime = 20.seconds, index = 1, uiIndex = 2, origin = ChapterOrigin.Generated),
            ),
        )

        // Collapse everything up to 10s onto a single instant; chapter A becomes zero-duration.
        val aligned = ChapterManagerImpl.alignGeneratedChapters(chapters) { time -> if (time <= 10.seconds) 10.seconds else time }

        assertEquals(1, aligned.size)
        assertEquals("B", aligned[0].title)
        assertEquals(1, aligned[0].index) // DB index preserved
        assertEquals(1, aligned[0].uiIndex) // UI index re-derived
    }

    @Test
    fun `align returns chapters unchanged when none are generated`() {
        val chapters = Chapters(
            listOf(
                Chapter(title = "A", startTime = 0.seconds, endTime = 10.seconds, index = 0, uiIndex = 1, origin = ChapterOrigin.Unknown),
            ),
        )

        val aligned = ChapterManagerImpl.alignGeneratedChapters(chapters) { it + 30.seconds }

        assertEquals(chapters, aligned)
    }

    @Test
    fun `observe hides already-saved generated chapters when feature is off`() = runBlocking {
        FeatureFlag.setEnabled(Feature.GENERATED_CHAPTERS, false)
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.002)
        val dbChapters = listOf(
            DbChapter(index = 0, episodeUuid = "id", startTimeMs = 0, endTimeMs = 1, title = "Embedded", origin = ChapterOrigin.PodcastIndex),
            DbChapter(index = 1, episodeUuid = "id", startTimeMs = 1, endTimeMs = 2, title = "Generated", origin = ChapterOrigin.Generated),
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(dbChapters))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            val chapters = awaitItem()
            assertEquals(listOf("Embedded"), chapters.map { it.title })
            awaitComplete()
        }
    }

    @Test
    fun `observe keeps generated chapters when feature is on`() = runBlocking {
        FeatureFlag.setEnabled(Feature.GENERATED_CHAPTERS, true)
        val episode = PodcastEpisode("id", publishedDate = Date(), duration = 0.002)
        val dbChapters = listOf(
            DbChapter(index = 0, episodeUuid = "id", startTimeMs = 0, endTimeMs = 1, title = "Embedded", origin = ChapterOrigin.PodcastIndex),
            DbChapter(index = 1, episodeUuid = "id", startTimeMs = 1, endTimeMs = 2, title = "Generated", origin = ChapterOrigin.Generated),
        )

        whenever(chapterDao.observeChaptersForEpisode("id")).thenReturn(flowOf(dbChapters))
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(flowOf(episode))

        chapterManager.observerChaptersForEpisode("id").test {
            val chapters = awaitItem()
            assertEquals(listOf("Embedded", "Generated"), chapters.map { it.title })
            awaitComplete()
        }
    }
}
