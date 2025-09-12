package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.AUDIO_VIDEO_FILTER_VIDEO_ONLY
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.SYNC_STATUS_NOT_SYNCED
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PlaylistManagerBaseTest {
    @get:Rule
    val dsl = PlaylistManagerDsl()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Test
    fun getPlaylistPreviews() = dsl.test {
        insertSmartPlaylist(index = 0)
        insertSmartPlaylist(index = 1)
        insertManualPlaylist(index = 2)

        expectPreviews(
            smartPreview(index = 0),
            smartPreview(index = 1),
            manualPreview(index = 2),
        )
    }

    @Test
    fun doNotGetDeletedPlaylistPreviews() = dsl.test {
        insertSmartPlaylist(index = 0) { it.copy(deleted = true) }
        insertManualPlaylist(index = 1) { it.copy(deleted = true) }

        expectNoPreviews()
    }

    @Test
    fun doNotGetDraftPlaylistPreviews() = dsl.test {
        insertSmartPlaylist(index = 0) { it.copy(draft = true) }
        insertManualPlaylist(index = 1) { it.copy(draft = true) }

        expectNoPreviews()
    }

    @Test
    fun sortPlaylistPreviews() = dsl.test {
        insertSmartPlaylist(index = 0) { it.copy(sortPosition = 0) }
        insertManualPlaylist(index = 1) { it.copy(sortPosition = 2) }
        insertSmartPlaylist(index = 2) { it.copy(sortPosition = 1) }
        insertManualPlaylist(index = 3) { it.copy(sortPosition = null) }

        expectPreviews(
            manualPreview(index = 3),
            smartPreview(index = 0),
            smartPreview(index = 2),
            manualPreview(index = 1),
        )
    }

    @Test
    fun useOnlyFollowedPodcastsForSmartPreviewArtwork() = dsl.test {
        insertSmartPlaylist(index = 0)
        insertPodcast(index = 0)
        insertPodcast(index = 1)
        insertPodcast(index = 2) { it.copy(isSubscribed = false) }

        expectPreviewEpisodeCount(playlistIndex = 0, count = 0)
        expectPreviewNoPodcasts(playlistIndex = 0)

        insertPodcastEpisode(index = 0, podcastIndex = 0)
        expectPreviewEpisodeCount(playlistIndex = 0, count = 1)
        expectPreviewPodcasts(playlistIndex = 0, podcasts = listOf("podcast-id-0"))

        insertPodcastEpisode(index = 1, podcastIndex = 1)
        expectPreviewEpisodeCount(playlistIndex = 0, count = 2)
        expectPreviewPodcasts(playlistIndex = 0, podcasts = listOf("podcast-id-0", "podcast-id-1"))

        insertPodcastEpisode(index = 2, podcastIndex = 2)
        expectPreviewEpisodeCount(playlistIndex = 0, count = 2)
        expectPreviewPodcasts(playlistIndex = 0, podcasts = listOf("podcast-id-0", "podcast-id-1"))
    }

    @Test
    fun ignoreArchivedEpisodesForSmartPreviews() = dsl.test {
        insertSmartPlaylist(index = 0)
        insertPodcast(index = 0)

        insertPodcastEpisode(index = 0, podcastIndex = 0) { it.copy(isArchived = true) }
        expectPreviewEpisodeCount(playlistIndex = 0, count = 0)
        expectPreviewNoPodcasts(playlistIndex = 0)
    }

    @Test
    fun useSmartRulesForSmartPreviewArtwork() = dsl.test {
        insertSmartPlaylist(index = 0) {
            it.copy(
                unplayed = true,
                partiallyPlayed = false,
                finished = true,
                audioVideo = AUDIO_VIDEO_FILTER_VIDEO_ONLY,
            )
        }
        repeat(4) { index ->
            insertPodcast(index = index)
        }
        insertPodcastEpisode(index = 0, podcastIndex = 0) {
            it.copy(playingStatus = EpisodePlayingStatus.NOT_PLAYED, fileType = "video/mp4")
        }
        insertPodcastEpisode(index = 1, podcastIndex = 1) {
            it.copy(playingStatus = EpisodePlayingStatus.COMPLETED, fileType = "video/mov")
        }
        insertPodcastEpisode(index = 2, podcastIndex = 2) {
            it.copy(playingStatus = EpisodePlayingStatus.NOT_PLAYED, fileType = "audio/mp3")
        }
        insertPodcastEpisode(index = 1, podcastIndex = 1) {
            it.copy(playingStatus = EpisodePlayingStatus.IN_PROGRESS, fileType = "video/mp4")
        }

        expectPreviewEpisodeCount(playlistIndex = 0, count = 2)
        expectPreviewPodcasts(playlistIndex = 0, podcasts = listOf("podcast-id-0", "podcast-id-1"))
    }

    @Test
    fun useOnlyAvailableEpisodesForManualPreviewArtwork() = dsl.test {
        insertManualPlaylist(index = 0)
        insertPodcast(index = 0)
        insertPodcast(index = 1) { it.copy(isSubscribed = false) }
        insertPodcastEpisode(index = 0, podcastIndex = 0)
        insertPodcastEpisode(index = 1, podcastIndex = 1)

        expectPreviewEpisodeCount(playlistIndex = 0, count = 0)
        expectPreviewNoPodcasts(playlistIndex = 0)

        insertManualEpisode(index = 0, podcastIndex = 0, playlistIndex = 0)
        expectPreviewEpisodeCount(playlistIndex = 0, count = 1)
        expectPreviewPodcasts(playlistIndex = 0, podcasts = listOf("podcast-id-0"))

        insertManualEpisode(index = 10, podcastIndex = 0, playlistIndex = 0)
        expectPreviewEpisodeCount(playlistIndex = 0, count = 2)
        expectPreviewPodcasts(playlistIndex = 0, podcasts = listOf("podcast-id-0"))

        insertManualEpisode(index = 1, podcastIndex = 1, playlistIndex = 0)
        expectPreviewEpisodeCount(playlistIndex = 0, count = 3)
        expectPreviewPodcasts(playlistIndex = 0, podcasts = listOf("podcast-id-0", "podcast-id-1"))
    }

    @Test
    fun sortPodcastsInSmartPreviews() = dsl.test {
        insertSmartPlaylist(index = 0)
        repeat(4) { index ->
            insertPodcast(index = index)
        }
        insertPodcastEpisode(index = 0, podcastIndex = 0) {
            it.copy(publishedDate = Date(0), addedDate = Date(0), duration = 1.0)
        }
        insertPodcastEpisode(index = 1, podcastIndex = 1) {
            it.copy(publishedDate = Date(1), addedDate = Date(1), duration = 3.0)
        }
        insertPodcastEpisode(index = 2, podcastIndex = 2) {
            it.copy(publishedDate = Date(2), addedDate = Date(2), duration = 2.0)
        }
        insertPodcastEpisode(index = 3, podcastIndex = 3) {
            it.copy(publishedDate = Date(3), addedDate = Date(3), duration = 4.0)
        }

        updateSortType(playlistIndex = 0, PlaylistEpisodeSortType.NewestToOldest)
        expectPreviewPodcasts(
            playlistIndex = 0,
            podcasts = listOf("podcast-id-3", "podcast-id-2", "podcast-id-1", "podcast-id-0"),
        )

        updateSortType(playlistIndex = 0, PlaylistEpisodeSortType.OldestToNewest)
        expectPreviewPodcasts(
            playlistIndex = 0,
            podcasts = listOf("podcast-id-0", "podcast-id-1", "podcast-id-2", "podcast-id-3"),
        )

        updateSortType(playlistIndex = 0, PlaylistEpisodeSortType.ShortestToLongest)
        expectPreviewPodcasts(
            playlistIndex = 0,
            podcasts = listOf("podcast-id-0", "podcast-id-2", "podcast-id-1", "podcast-id-3"),
        )

        updateSortType(playlistIndex = 0, PlaylistEpisodeSortType.LongestToShortest)
        expectPreviewPodcasts(
            playlistIndex = 0,
            podcasts = listOf("podcast-id-3", "podcast-id-1", "podcast-id-2", "podcast-id-0"),
        )
    }

    @Test
    fun sortPodcastsInManualPreviews() = dsl.test {
        insertManualPlaylist(index = 0)
        insertPodcastEpisode(index = 0, podcastIndex = 0) {
            it.copy(publishedDate = Date(0), addedDate = Date(0), duration = 1.0)
        }
        insertPodcastEpisode(index = 1, podcastIndex = 1) {
            it.copy(publishedDate = Date(1), addedDate = Date(1), duration = 3.0)
        }
        insertPodcastEpisode(index = 2, podcastIndex = 2) {
            it.copy(publishedDate = Date(2), addedDate = Date(2), duration = 2.0)
        }
        insertPodcastEpisode(index = 3, podcastIndex = 3) {
            it.copy(publishedDate = Date(3), addedDate = Date(3), duration = 4.0)
        }
        repeat(4) { index ->
            insertManualEpisode(index = index, podcastIndex = index, playlistIndex = 0)
        }

        updateSortType(playlistIndex = 0, PlaylistEpisodeSortType.NewestToOldest)
        expectPreviewPodcasts(
            playlistIndex = 0,
            podcasts = listOf("podcast-id-3", "podcast-id-2", "podcast-id-1", "podcast-id-0"),
        )

        updateSortType(playlistIndex = 0, PlaylistEpisodeSortType.OldestToNewest)
        expectPreviewPodcasts(
            playlistIndex = 0,
            podcasts = listOf("podcast-id-0", "podcast-id-1", "podcast-id-2", "podcast-id-3"),
        )

        updateSortType(playlistIndex = 0, PlaylistEpisodeSortType.ShortestToLongest)
        expectPreviewPodcasts(
            playlistIndex = 0,
            podcasts = listOf("podcast-id-0", "podcast-id-2", "podcast-id-1", "podcast-id-3"),
        )

        updateSortType(playlistIndex = 0, PlaylistEpisodeSortType.LongestToShortest)
        expectPreviewPodcasts(
            playlistIndex = 0,
            podcasts = listOf("podcast-id-3", "podcast-id-1", "podcast-id-2", "podcast-id-0"),
        )
    }

    @Test
    fun doNotDuplicatePodcastsInSmartPreviews() = dsl.test {
        insertSmartPlaylist(index = 0)
        repeat(10) { index ->
            insertPodcast(index = index)
        }
        repeat(10) { index ->
            insertPodcastEpisode(index = index, podcastIndex = index)
            insertPodcastEpisode(index = index + 100, podcastIndex = index)
        }

        expectPreviewEpisodeCount(playlistIndex = 0, count = 20)
        expectPreviewPodcasts(
            playlistIndex = 0,
            podcasts = listOf("podcast-id-0", "podcast-id-1", "podcast-id-2", "podcast-id-3"),
        )
    }

    @Test
    fun doNotDuplicatePodcastsInManualPreviews() = dsl.test {
        insertManualPlaylist(index = 0)
        repeat(10) { index ->
            insertPodcastEpisode(index = index, podcastIndex = index)
            insertPodcastEpisode(index = index + 100, podcastIndex = index)
        }
        repeat(10) { index ->
            insertManualEpisode(index = index, podcastIndex = index, playlistIndex = 0)
            insertManualEpisode(index = index + 100, podcastIndex = index, playlistIndex = 0)
        }

        expectPreviewEpisodeCount(playlistIndex = 0, count = 20)
        expectPreviewPodcasts(
            playlistIndex = 0,
            podcasts = listOf("podcast-id-0", "podcast-id-1", "podcast-id-2", "podcast-id-3"),
        )
    }

    @Test
    fun reorderPlaylists() = dsl.test {
        val playlistUuids = List(100) { index ->
            val playlist = if (index % 2 == 0) {
                insertSmartPlaylist(index = index)
            } else {
                insertManualPlaylist(index = index)
            }
            playlist.uuid
        }

        val shuffledPlaylistUuids = playlistUuids.shuffled()
        manager.sortPlaylists(shuffledPlaylistUuids)

        expectPreviewUuids(shuffledPlaylistUuids)
        repeat(100) { index ->
            expectSyncStatus(playlistIndex = index, SYNC_STATUS_NOT_SYNCED)
        }
    }

    @Test
    fun reorderUnspecifiedPlaylistsToBottom() = dsl.test {
        val playlists = List(5) { index ->
            insertSmartPlaylist(index = index)
        }

        val reorderedPlaylistUuids = listOf(
            playlists[4].uuid,
            playlists[1].uuid,
            playlists[3].uuid,
        )
        manager.sortPlaylists(reorderedPlaylistUuids)

        expectPreviewUuids(
            listOf(
                "playlist-id-4",
                "playlist-id-1",
                "playlist-id-3",
                "playlist-id-0",
                "playlist-id-2",
            ),
        )
    }

    @Test
    fun updateSortType() = dsl.test {
        insertSmartPlaylist(index = 0)
        insertManualPlaylist(index = 1)

        manager.updateSortType("playlist-id-0", PlaylistEpisodeSortType.ShortestToLongest)
        expectSortType(playlistIndex = 0, PlaylistEpisodeSortType.ShortestToLongest)

        manager.updateSortType("playlist-id-1", PlaylistEpisodeSortType.OldestToNewest)
        expectSortType(playlistIndex = 1, PlaylistEpisodeSortType.OldestToNewest)
    }

    @Test
    fun updateAutoDownload() = dsl.test {
        insertSmartPlaylist(index = 0)
        insertManualPlaylist(index = 1)

        manager.updateAutoDownload("playlist-id-0", true)
        expectAutoDownloadEnabled(playlistIndex = 0)

        manager.updateAutoDownload("playlist-id-0", false)
        expectAutoDownloadDisabled(playlistIndex = 0)

        manager.updateAutoDownload("playlist-id-1", true)
        expectAutoDownloadEnabled(playlistIndex = 1)

        manager.updateAutoDownload("playlist-id-1", false)
        expectAutoDownloadDisabled(playlistIndex = 1)
    }

    @Test
    fun updateAutoDownloadLimit() = dsl.test {
        insertSmartPlaylist(index = 0)
        insertManualPlaylist(index = 1)

        manager.updateAutoDownloadLimit("playlist-id-0", 100)
        expectAutoDownloadLimit(playlistIndex = 0, limit = 100)

        manager.updateAutoDownloadLimit("playlist-id-1", 200)
        expectAutoDownloadLimit(playlistIndex = 1, limit = 200)
    }

    @Test
    fun updateName() = dsl.test {
        insertSmartPlaylist(index = 0)
        insertManualPlaylist(index = 1)

        manager.updateName("playlist-id-0", "New name 1")
        expectName(playlistIndex = 0, name = "New name 1")

        manager.updateName("playlist-id-1", "New name 2")
        expectName(playlistIndex = 1, name = "New name 2")
    }

    @Test
    fun getSmartPlaylistAutoDownloadEpisodes() = dsl.test {
        insertSmartPlaylist(index = 0) { it.copy(autoDownload = true, autodownloadLimit = 3) }
        insertSmartPlaylist(index = 1) { it.copy(autoDownload = false, autodownloadLimit = 100) }
        insertPodcast(index = 0)
        insertPodcast(index = 1) { it.copy(isSubscribed = false) }
        repeat(10) { index ->
            insertPodcastEpisode(index = index, podcastIndex = index % 2)
        }

        val episodes = manager.getAutoDownloadEpisodes()
        assertEquals(
            listOf(
                podcastEpisode(index = 0, podcastIndex = 0),
                podcastEpisode(index = 2, podcastIndex = 0),
                podcastEpisode(index = 4, podcastIndex = 0),
            ),
            episodes,
        )
    }

    @Test
    fun getManualPlaylistAutoDownloadEpisodes() = dsl.test {
        insertManualPlaylist(index = 0) { it.copy(autoDownload = true, autodownloadLimit = 3) }
        insertManualPlaylist(index = 1) { it.copy(autoDownload = false, autodownloadLimit = 100) }
        repeat(10) { index ->
            if (index % 2 == 0) {
                insertPodcastEpisode(index = index, podcastIndex = 0)
            }
            insertManualEpisode(index = index, podcastIndex = 0, playlistIndex = 0)
            insertManualEpisode(index = index, podcastIndex = 0, playlistIndex = 1)
        }

        val episodes = manager.getAutoDownloadEpisodes()
        assertEquals(
            listOf(
                podcastEpisode(index = 0, podcastIndex = 0),
                podcastEpisode(index = 2, podcastIndex = 0),
                podcastEpisode(index = 4, podcastIndex = 0),
            ),
            episodes,
        )
    }

    @Test
    fun getDistinctAutoDownloadEpisodes() = dsl.test {
        insertManualPlaylist(index = 0) { it.copy(autoDownload = true, autodownloadLimit = 4) }
        insertManualPlaylist(index = 1) { it.copy(autoDownload = true, autodownloadLimit = 5) }
        repeat(10) { index ->
            insertPodcastEpisode(index = index, podcastIndex = 0)
            insertManualEpisode(index = index, podcastIndex = 0, playlistIndex = 0)
            if (index >= 2) {
                insertManualEpisode(index = index, podcastIndex = 0, playlistIndex = 1)
            }
        }

        val episodes = manager.getAutoDownloadEpisodes()
        assertEquals(
            listOf(
                podcastEpisode(index = 0, podcastIndex = 0),
                podcastEpisode(index = 1, podcastIndex = 0),
                podcastEpisode(index = 2, podcastIndex = 0),
                podcastEpisode(index = 3, podcastIndex = 0),
                podcastEpisode(index = 4, podcastIndex = 0),
                podcastEpisode(index = 5, podcastIndex = 0),
                podcastEpisode(index = 6, podcastIndex = 0),
            ),
            episodes,
        )
    }

    @Test
    fun toggleShowingArchivedEpisodes() = dsl.test {
        insertSmartPlaylist(index = 0)

        expectNotShowArchived(playlistIndex = 0)

        manager.toggleShowArchived("playlist-id-0")
        expectShowArchived(playlistIndex = 0)

        manager.toggleShowArchived("playlist-id-0")
        expectNotShowArchived(playlistIndex = 0)
    }
}
