package au.com.shiftyjelly.pocketcasts.repositories.sync

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import java.time.Duration
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class PodcastSyncProcessTest {

    @Test
    fun podcastToRecord() {
        val addedDateSinceEpoch = Duration.ofSeconds(123)
        val podcastMock = mock<Podcast> {
            on { addedDate } doReturn Date(addedDateSinceEpoch.toMillis())
            on { folderUuid } doReturn "folderUuid"
            on { uuid } doReturn "uuid"
            on { startFromSecs } doReturn 11
            on { skipLastSecs } doReturn 22
            on { sortPosition } doReturn 3
        }

        val record = PodcastSyncProcess.toRecord(podcastMock).podcast

        assertEquals(addedDateSinceEpoch.seconds, record.dateAdded.seconds)
        assertEquals("folderUuid", record.folderUuid.value)
        assertEquals("uuid", record.uuid)
        assertEquals(11, record.settings.autoStartFrom.value.value)
        assertEquals(22, record.settings.autoSkipLast.value.value)
    }

    @Test
    fun podcastToRecord_subscribed() {
        val podcastMock = mock<Podcast> {
            on { folderUuid } doReturn "folderUuid"
            on { uuid } doReturn "uuid"
            on { isSubscribed } doReturn true
        }

        val record = PodcastSyncProcess.toRecord(podcastMock).podcast

        assertFalse(record.isDeleted.value)
        assertTrue(record.subscribed.value)
    }

    @Test
    fun podcastToRecord_unsubscribed() {
        val podcastMock = mock<Podcast> {
            on { folderUuid } doReturn "folderUuid"
            on { uuid } doReturn "uuid"
            on { isSubscribed } doReturn false
        }
        val record = PodcastSyncProcess.toRecord(podcastMock).podcast

        assertTrue(record.isDeleted.value)
        assertFalse(record.subscribed.value)
    }

    @Test
    fun podcastEpisodeToRecord_hasFields() {
        val podcastEpisodeMock = mock<PodcastEpisode> {
            on { uuid } doReturn "uuid"
            on { podcastUuid } doReturn "podcastUuid"

            on { playingStatusModified } doReturn 111
            on { playingStatus } doReturn EpisodePlayingStatus.IN_PROGRESS

            on { playedUpToModified } doReturn 333
            on { playedUpTo } doReturn 12.0

            on { durationModified } doReturn 444
            on { duration } doReturn 13.0
        }

        val record = PodcastSyncProcess.toRecord(podcastEpisodeMock).episode

        assertEquals("uuid", record.uuid)
        assertEquals("podcastUuid", record.podcastUuid)

        assertEquals(111, record.playingStatusModified.value)
        assertEquals(EpisodePlayingStatus.IN_PROGRESS.toInt(), record.playingStatus.value)

        assertEquals(333, record.playedUpToModified.value)
        assertEquals(12, record.playedUpTo.value)

        assertEquals(444, record.durationModified.value)
        assertEquals(13, record.duration.value)
    }

    @Test
    fun podcastEpisodeToRecord_missingFields() {
        val podcastEpisodeMock = mock<PodcastEpisode> {
            on { uuid } doReturn "uuid"
            on { podcastUuid } doReturn "podcastUuid"

            on { playingStatusModified } doReturn null
            on { playedUpToModified } doReturn null
            on { durationModified } doReturn null
        }

        val record = PodcastSyncProcess.toRecord(podcastEpisodeMock).episode

        assertFalse(record.hasPlayingStatus())
        assertFalse(record.hasPlayedUpTo())
        assertFalse(record.hasDuration())
    }

    @Test
    fun podcastEpisodeToRecord_starred() {
        val podcastEpisodeMock = mock<PodcastEpisode> {
            on { uuid } doReturn "uuid"
            on { podcastUuid } doReturn "podcastUuid"
            on { playingStatusModified } doReturn null // avoids NPE

            on { starredModified } doReturn 123
            on { isStarred } doReturn true
        }

        val record = PodcastSyncProcess.toRecord(podcastEpisodeMock).episode

        assertEquals(123, record.starredModified.value)
        assertTrue(record.starred.value)
    }

    @Test
    fun podcastEpisodeToRecord_notStarred() {
        val podcastEpisodeMock = mock<PodcastEpisode> {
            on { uuid } doReturn "uuid"
            on { podcastUuid } doReturn "podcastUuid"
            on { playingStatusModified } doReturn null // avoids NPE

            on { starredModified } doReturn 222
            on { isStarred } doReturn false
        }

        val record = PodcastSyncProcess.toRecord(podcastEpisodeMock).episode

        assertEquals(222, record.starredModified.value)
        assertFalse(record.starred.value)
    }

    @Test
    fun podcastEpisodeToRecord_starredNotModified() {
        val podcastEpisodeMock = mock<PodcastEpisode> {
            on { uuid } doReturn "uuid"
            on { podcastUuid } doReturn "podcastUuid"
            on { playingStatusModified } doReturn null // avoids NPE

            on { starredModified } doReturn null
        }

        val record = PodcastSyncProcess.toRecord(podcastEpisodeMock).episode

        assertFalse(record.hasStarred())
    }

    @Test
    fun podcastEpisodeToRecord_archived() {
        val podcastEpisodeMock = mock<PodcastEpisode> {
            on { uuid } doReturn "uuid"
            on { podcastUuid } doReturn "podcastUuid"
            on { playingStatusModified } doReturn null // avoids NPE

            on { archivedModified } doReturn 123
            on { isArchived } doReturn true
        }

        val record = PodcastSyncProcess.toRecord(podcastEpisodeMock).episode

        assertEquals(123, record.isDeletedModified.value)
        assertTrue(record.isDeleted.value)
    }

    @Test
    fun podcastEpisodeToRecord_notArchived() {
        val podcastEpisodeMock = mock<PodcastEpisode> {
            on { uuid } doReturn "uuid"
            on { podcastUuid } doReturn "podcastUuid"
            on { playingStatusModified } doReturn null // avoids NPE

            on { archivedModified } doReturn 222
            on { isArchived } doReturn false
        }

        val record = PodcastSyncProcess.toRecord(podcastEpisodeMock).episode

        assertEquals(222, record.isDeletedModified.value)
        assertFalse(record.isDeleted.value)
    }

    @Test
    fun podcastEpisodeToRecord_archivedNotModified() {
        val podcastEpisodeMock = mock<PodcastEpisode> {
            on { uuid } doReturn "uuid"
            on { podcastUuid } doReturn "podcastUuid"
            on { playingStatusModified } doReturn null // avoids NPE

            on { archivedModified } doReturn null
        }

        val record = PodcastSyncProcess.toRecord(podcastEpisodeMock).episode

        assertFalse(record.hasIsDeleted())
    }
}
