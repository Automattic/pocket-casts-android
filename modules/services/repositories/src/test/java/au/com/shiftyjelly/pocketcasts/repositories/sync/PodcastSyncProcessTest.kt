package au.com.shiftyjelly.pocketcasts.repositories.sync

import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import java.time.Duration
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class PodcastSyncProcessTest {

    @Mock
    lateinit var statsManager: StatsManager

    @Mock
    lateinit var settings: Settings

    @Test
    fun podcastToRecord() {
        val addedDateSinceEpoch = Duration.ofSeconds(123)
        val record = PodcastSyncProcess.toRecord(
            mockPodcast(
                addedDateSinceEpoch = addedDateSinceEpoch,
                folderUuid = "folderUuid",
                uuid = "uuid",
                startFromSecs = 11,
                skipLastSecs = 22,
                sortPosition = 3,
                autoAddToUpNext = Podcast.AutoAddUpNext.PLAY_NEXT,
                overrideGlobalEffects = true,
                playbackSpeed = 2.0,
                trimMode = TrimMode.HIGH,
                isVolumeBoosted = true,
            ),
        ).podcast

        assertEquals(addedDateSinceEpoch.seconds, record.dateAdded.seconds)
        assertEquals("folderUuid", record.folderUuid.value)
        assertEquals("uuid", record.uuid)
        assertEquals(11, record.settings.autoStartFrom.value.value)
        assertEquals(22, record.settings.autoSkipLast.value.value)
        assertEquals(true, record.settings.addToUpNext.value.value)
        assertEquals(1, record.settings.addToUpNextPosition.value.value)
        assertEquals(true, record.settings.playbackEffects.value.value)
        assertEquals(2.0, record.settings.playbackSpeed.value.value, 0.01)
        assertEquals(3, record.settings.trimSilence.value.value)
        assertEquals(true, record.settings.volumeBoost.value.value)
    }

    @Test
    fun podcastToRecord_subscribed() {
        val record = PodcastSyncProcess.toRecord(
            mockPodcast(isSubscribed = true),
        ).podcast

        assertFalse(record.isDeleted.value)
        assertTrue(record.subscribed.value)
    }

    @Test
    fun podcastToRecord_unsubscribed() {
        val record = PodcastSyncProcess.toRecord(
            mockPodcast(isSubscribed = false),
        ).podcast

        assertTrue(record.isDeleted.value)
        assertFalse(record.subscribed.value)
    }

    @Test
    fun podcastEpisodeToRecord_hasFields() {
        val record = PodcastSyncProcess.toRecord(
            mockPodcastEpisode(
                uuid = "uuid",
                podcastUuid = "podcastUuid",
                playingStatusModified = 111,
                episodePlayingStatus = EpisodePlayingStatus.IN_PROGRESS,
                playedUpToModified = 333,
                playedUpTo = 12.0,
                durationModified = 444,
                duration = 13.0,
            ),
        ).episode

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
        val record = PodcastSyncProcess.toRecord(
            mockPodcastEpisode(
                playingStatusModified = null,
                playedUpToModified = null,
                durationModified = null,
                starredModified = null,
                archiveModified = null,
            ),
        ).episode

        assertFalse(record.hasPlayingStatus())
        assertFalse(record.hasPlayedUpTo())
        assertFalse(record.hasDuration())
        assertFalse(record.hasStarred())
        assertFalse(record.hasIsDeleted()) // archived
    }

    @Test
    fun podcastEpisodeToRecord_starred() {
        val record = PodcastSyncProcess.toRecord(
            mockPodcastEpisode(
                starredModified = 123,
                isStarred = true,
            ),
        ).episode

        assertEquals(123, record.starredModified.value)
        assertTrue(record.starred.value)
    }

    @Test
    fun podcastEpisodeToRecord_notStarred() {
        val record = PodcastSyncProcess.toRecord(
            mockPodcastEpisode(
                starredModified = 222,
                isStarred = false,
            ),
        ).episode

        assertEquals(222, record.starredModified.value)
        assertFalse(record.starred.value)
    }

    @Test
    fun podcastEpisodeToRecord_archived() {
        val record = PodcastSyncProcess.toRecord(
            mockPodcastEpisode(
                archiveModified = 123,
                isArchived = true,
            ),
        ).episode

        assertEquals(123, record.isDeletedModified.value)
        assertTrue(record.isDeleted.value)
    }

    @Test
    fun podcastEpisodeToRecord_notArchived() {
        val record = PodcastSyncProcess.toRecord(
            mockPodcastEpisode(
                archiveModified = 222,
                isArchived = false,
            ),
        ).episode

        assertEquals(222, record.isDeletedModified.value)
        assertFalse(record.isDeleted.value)
    }

    @Test
    fun bookmarkToRecord() {
        val createdDateSinceEpoch = Duration.ofSeconds(123)
        val record = PodcastSyncProcess.toRecord(
            mockBookmark(
                uuid = "uuid",
                podcastUuid = "podcastUuid",
                episodeUuid = "episodeUuid",
                timeSecs = 11,
                createdDateSinceEpoch = createdDateSinceEpoch,
                titleModified = 22,
                title = "title",
                deletedModified = 33,
                deleted = true,
            ),
        ).bookmark

        assertEquals("uuid", record.bookmarkUuid)
        assertEquals("podcastUuid", record.podcastUuid)
        assertEquals("episodeUuid", record.episodeUuid)
        assertEquals(11, record.time.value)
        assertEquals(createdDateSinceEpoch.seconds, record.createdAt.seconds)
        assertEquals(22, record.titleModified.value)
        assertEquals("title", record.title.value)
        assertEquals(33, record.isDeletedModified.value)
        assertTrue(record.isDeleted.value)
    }

    @Test
    fun bookmarkToRecord_notDeleted() {
        val record = PodcastSyncProcess.toRecord(
            mockBookmark(
                deletedModified = 0,
                deleted = false,
            ),
        ).bookmark
        assertFalse(record.isDeleted.value)
    }

    @Test
    fun bookmarkToRecord_deletedNotModified() {
        val record = PodcastSyncProcess.toRecord(
            mockBookmark(deletedModified = null),
        ).bookmark
        assertFalse(record.hasIsDeleted())
    }

    @Test
    fun bookmarkToRecord_titleNotModified() {
        val record = PodcastSyncProcess.toRecord(
            mockBookmark(titleModified = null),
        ).bookmark
        assertFalse(record.hasTitle())
    }

    @Test
    fun syncDeviceIfStatsChanged() {
        whenever(statsManager.isSynced(any())).thenReturn(false)
        whenever(statsManager.isEmpty).thenReturn(false)
        whenever(settings.getUniqueDeviceId()).thenReturn("deviceId")
        whenever(statsManager.timeSavedSilenceRemovalSecs).thenReturn(1)
        whenever(statsManager.timeSavedSkippingSecs).thenReturn(2)
        whenever(statsManager.timeSavedSkippingIntroSecs).thenReturn(3)
        whenever(statsManager.timeSavedVariableSpeedSecs).thenReturn(4)
        whenever(statsManager.totalListeningTimeSecs).thenReturn(5)
        whenever(statsManager.statsStartTimeSecs).thenReturn(6)

        val result = getPodcastSyncProcess().getSyncUserDevice()

        assertEquals("deviceId", result?.deviceId?.value)
        assertEquals(PodcastSyncProcess.ANDROID_DEVICE_TYPE, result?.deviceType?.value)
        assertEquals(1L, result?.timeSilenceRemoval?.value)
        assertEquals(2L, result?.timeSkipping?.value)
        assertEquals(3L, result?.timeIntroSkipping?.value)
        assertEquals(4L, result?.timeVariableSpeed?.value)
        assertEquals(5L, result?.timeListened?.value)
        assertEquals(6L, result?.timesStartedAt?.value)
    }

    @Test
    fun noSyncDeviceIfAlreadySynced() {
        whenever(statsManager.isSynced(any())).thenReturn(true)
        val result = getPodcastSyncProcess().getSyncUserDevice()
        assertNull(result)
    }

    @Test
    fun noSyncDeviceIfStatsEmpty() {
        whenever(statsManager.isEmpty).thenReturn(true)
        val result = getPodcastSyncProcess().getSyncUserDevice()
        assertNull(result)
    }

    private fun getPodcastSyncProcess() = PodcastSyncProcess(
        context = mock(),
        applicationScope = mock(),
        settings = settings,
        episodeManager = mock(),
        podcastManager = mock(),
        playlistManager = mock(),
        bookmarkManager = mock(),
        statsManager = statsManager,
        fileStorage = mock(),
        playbackManager = mock(),
        podcastCacheServerManager = mock(),
        userEpisodeManager = mock(),
        subscriptionManager = mock(),
        folderManager = mock(),
        syncManager = mock(),
    )

    private fun mockPodcast(
        addedDateSinceEpoch: Duration = Duration.ZERO,
        folderUuid: String = "",
        uuid: String = "",
        startFromSecs: Int = 0,
        skipLastSecs: Int = 0,
        sortPosition: Int = 0,
        isSubscribed: Boolean = false,
        autoAddToUpNext: Podcast.AutoAddUpNext = Podcast.AutoAddUpNext.OFF,
        overrideGlobalEffects: Boolean = false,
        playbackSpeed: Double = 1.0,
        trimMode: TrimMode = TrimMode.OFF,
        isVolumeBoosted: Boolean = false,
    ) = mock<Podcast> {
        on { this.addedDate } doReturn Date(addedDateSinceEpoch.toMillis())
        on { this.folderUuid } doReturn folderUuid
        on { this.uuid } doReturn uuid
        on { this.startFromSecs } doReturn startFromSecs
        on { this.skipLastSecs } doReturn skipLastSecs
        on { this.sortPosition } doReturn sortPosition
        on { this.isSubscribed } doReturn isSubscribed
        on { this.autoAddToUpNext } doReturn autoAddToUpNext
        on { this.overrideGlobalEffects } doReturn overrideGlobalEffects
        on { this.playbackSpeed } doReturn playbackSpeed
        on { this.trimMode } doReturn trimMode
        on { this.isVolumeBoosted } doReturn isVolumeBoosted
    }

    private fun mockPodcastEpisode(
        uuid: String = "",
        podcastUuid: String = "",
        playingStatusModified: Long? = null,
        episodePlayingStatus: EpisodePlayingStatus = EpisodePlayingStatus.IN_PROGRESS,
        playedUpToModified: Long? = null,
        playedUpTo: Double = 0.0,
        durationModified: Long? = null,
        duration: Double = 0.0,
        starredModified: Long? = null,
        isStarred: Boolean = false,
        archiveModified: Long? = null,
        isArchived: Boolean = false,
    ) = mock<PodcastEpisode> {
        on { this.uuid } doReturn uuid
        on { this.podcastUuid } doReturn podcastUuid

        on { this.playingStatusModified } doReturn playingStatusModified
        on { this.playingStatus } doReturn episodePlayingStatus

        on { this.playedUpToModified } doReturn playedUpToModified
        on { this.playedUpTo } doReturn playedUpTo

        on { this.durationModified } doReturn durationModified
        on { this.duration } doReturn duration

        on { this.starredModified } doReturn starredModified
        on { this.isStarred } doReturn isStarred

        on { this.archivedModified } doReturn archiveModified
        on { this.isArchived } doReturn isArchived
    }

    private fun mockBookmark(
        uuid: String = "",
        podcastUuid: String = "",
        episodeUuid: String = "",
        timeSecs: Int = 0,
        createdDateSinceEpoch: Duration = Duration.ZERO,
        titleModified: Long? = null,
        title: String = "",
        deletedModified: Long? = null,
        deleted: Boolean = false,
    ) = mock<Bookmark> {
        on { this.uuid } doReturn uuid
        on { this.podcastUuid } doReturn podcastUuid
        on { this.episodeUuid } doReturn episodeUuid
        on { this.timeSecs } doReturn timeSecs
        on { this.createdAt } doReturn Date(createdDateSinceEpoch.toMillis())
        on { this.titleModified } doReturn titleModified
        on { this.title } doReturn title
        on { this.deletedModified } doReturn deletedModified
        on { this.deleted } doReturn deleted
    }
}
