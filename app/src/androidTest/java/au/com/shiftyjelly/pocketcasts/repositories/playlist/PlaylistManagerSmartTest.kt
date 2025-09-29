package au.com.shiftyjelly.pocketcasts.repositories.playlist

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.ANYTIME
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.AUDIO_VIDEO_FILTER_ALL
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.AUDIO_VIDEO_FILTER_AUDIO_ONLY
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.AUDIO_VIDEO_FILTER_VIDEO_ONLY
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_24_HOURS
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_2_WEEKS
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_3_DAYS
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_MONTH
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.LAST_WEEK
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.SYNC_STATUS_NOT_SYNCED
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.DownloadStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeDurationRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.EpisodeStatusRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.MediaTypeRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.PodcastsRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.ReleaseDateRule
import au.com.shiftyjelly.pocketcasts.models.type.SmartRules.StarredRule
import java.util.Date
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class PlaylistManagerSmartTest {
    @get:Rule
    val dsl = PlaylistManagerDsl()

    @Test
    fun createPlaylist() = dsl.test {
        val drafts = listOf(
            playlistDraft(index = 0),
            playlistDraft(index = 1) {
                it.copy(episodeStatus = EpisodeStatusRule(unplayed = true, inProgress = false, completed = false))
            },
            playlistDraft(index = 2) {
                it.copy(episodeStatus = EpisodeStatusRule(unplayed = false, inProgress = true, completed = false))
            },
            playlistDraft(index = 3) {
                it.copy(episodeStatus = EpisodeStatusRule(unplayed = false, inProgress = false, completed = true))
            },
            playlistDraft(index = 4) {
                it.copy(downloadStatus = DownloadStatusRule.Any)
            },
            playlistDraft(index = 5) {
                it.copy(downloadStatus = DownloadStatusRule.Downloaded)
            },
            playlistDraft(index = 6) {
                it.copy(downloadStatus = DownloadStatusRule.NotDownloaded)
            },
            playlistDraft(index = 7) {
                it.copy(mediaType = MediaTypeRule.Any)
            },
            playlistDraft(index = 8) {
                it.copy(mediaType = MediaTypeRule.Audio)
            },
            playlistDraft(index = 9) {
                it.copy(mediaType = MediaTypeRule.Video)
            },
            playlistDraft(index = 10) {
                it.copy(releaseDate = ReleaseDateRule.AnyTime)
            },
            playlistDraft(index = 11) {
                it.copy(releaseDate = ReleaseDateRule.Last24Hours)
            },
            playlistDraft(index = 12) {
                it.copy(releaseDate = ReleaseDateRule.Last3Days)
            },
            playlistDraft(index = 13) {
                it.copy(releaseDate = ReleaseDateRule.LastWeek)
            },
            playlistDraft(index = 14) {
                it.copy(releaseDate = ReleaseDateRule.Last2Weeks)
            },
            playlistDraft(index = 15) {
                it.copy(releaseDate = ReleaseDateRule.LastMonth)
            },
            playlistDraft(index = 16) {
                it.copy(starred = StarredRule.Any)
            },
            playlistDraft(index = 17) {
                it.copy(starred = StarredRule.Starred)
            },
            playlistDraft(index = 18) {
                it.copy(podcasts = PodcastsRule.Any)
            },
            playlistDraft(index = 19) {
                it.copy(podcasts = PodcastsRule.Selected(uuids = setOf("id-1", "id-2")))
            },
            playlistDraft(index = 20) {
                it.copy(episodeDuration = EpisodeDurationRule.Any)
            },
            playlistDraft(index = 21) {
                it.copy(episodeDuration = EpisodeDurationRule.Constrained(longerThan = 50.minutes, shorterThan = 60.minutes))
            },
        )

        val playlistUuids = drafts.map { draft -> manager.createSmartPlaylist(draft) }

        fun unsyncedPlaylist(index: Int, builder: (PlaylistEntity) -> PlaylistEntity = { it }): PlaylistEntity {
            return builder(
                smartPlaylistEntity(index) {
                    it.copy(
                        uuid = playlistUuids[index],
                        syncStatus = SYNC_STATUS_NOT_SYNCED,
                        sortPosition = drafts.size - index - 1,
                    )
                },
            )
        }

        expectPlaylist(
            unsyncedPlaylist(index = 0),
        )
        expectPlaylist(
            unsyncedPlaylist(index = 1) { it.copy(unplayed = true, partiallyPlayed = false, finished = false) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 2) { it.copy(unplayed = false, partiallyPlayed = true, finished = false) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 3) { it.copy(unplayed = false, partiallyPlayed = false, finished = true) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 4) { it.copy(downloaded = true, notDownloaded = true) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 5) { it.copy(downloaded = true, notDownloaded = false) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 6) { it.copy(downloaded = false, notDownloaded = true) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 7) { it.copy(audioVideo = AUDIO_VIDEO_FILTER_ALL) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 8) { it.copy(audioVideo = AUDIO_VIDEO_FILTER_AUDIO_ONLY) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 9) { it.copy(audioVideo = AUDIO_VIDEO_FILTER_VIDEO_ONLY) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 10) { it.copy(filterHours = ANYTIME) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 11) { it.copy(filterHours = LAST_24_HOURS) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 12) { it.copy(filterHours = LAST_3_DAYS) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 13) { it.copy(filterHours = LAST_WEEK) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 14) { it.copy(filterHours = LAST_2_WEEKS) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 15) { it.copy(filterHours = LAST_MONTH) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 16) { it.copy(starred = false) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 17) { it.copy(starred = true) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 18) { it.copy(allPodcasts = true, podcastUuids = null) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 19) { it.copy(allPodcasts = false, podcastUuids = "id-1,id-2") },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 20) { it.copy(filterDuration = false, longerThan = 20, shorterThan = 40) },
        )
        expectPlaylist(
            unsyncedPlaylist(index = 21) { it.copy(filterDuration = true, longerThan = 50, shorterThan = 60) },
        )
    }

    @Test
    fun createDefaultNewReleasesPlaylist() = dsl.test {
        manager.createSmartPlaylist(SmartPlaylistDraft.NewReleases)

        expectPlaylist(
            smartPlaylistEntity(index = 0) {
                it.copy(
                    uuid = Playlist.NEW_RELEASES_UUID,
                    title = "New Releases",
                    filterHours = LAST_2_WEEKS,
                    iconId = 10,
                    sortPosition = 0,
                    syncStatus = SYNC_STATUS_NOT_SYNCED,
                )
            },
        )
    }

    @Test
    fun createDefaultInProgressPlaylist() = dsl.test {
        manager.createSmartPlaylist(SmartPlaylistDraft.InProgress)

        expectPlaylist(
            smartPlaylistEntity(index = 0) {
                it.copy(
                    uuid = Playlist.IN_PROGRESS_UUID,
                    title = "In Progress",
                    filterHours = LAST_MONTH,
                    unplayed = false,
                    partiallyPlayed = true,
                    finished = false,
                    iconId = 23,
                    sortPosition = 0,
                    syncStatus = SYNC_STATUS_NOT_SYNCED,
                )
            },
        )
    }

    @Test
    fun observeEpisodes() = dsl.test {
        val rules = smartRules {
            it.copy(
                episodeStatus = EpisodeStatusRule(
                    unplayed = true,
                    inProgress = true,
                    completed = false,
                ),
                mediaType = MediaTypeRule.Audio,
                episodeDuration = EpisodeDurationRule.Constrained(longerThan = 10.minutes, shorterThan = 35.minutes),
            )
        }

        insertPodcast(index = 0)

        manager.smartEpisodesFlow(rules).test {
            assertEquals(emptyList<PlaylistEpisode.Available>(), awaitItem())

            val episode1 = insertPodcastEpisode(index = 0, podcastIndex = 0) {
                it.copy(
                    playingStatus = EpisodePlayingStatus.NOT_PLAYED,
                    fileType = "audio/mp3",
                    duration = 15.minutes.inWholeSeconds.toDouble(),
                )
            }
            assertEquals(listOf(episode1).map(PlaylistEpisode::Available), awaitItem())

            val episode2 = insertPodcastEpisode(index = 1, podcastIndex = 0) {
                it.copy(
                    playingStatus = EpisodePlayingStatus.IN_PROGRESS,
                    fileType = "audio/mp3",
                    duration = 30.minutes.inWholeSeconds.toDouble(),
                )
            }
            assertEquals(listOf(episode1, episode2).map(PlaylistEpisode::Available), awaitItem())

            insertPodcastEpisode(index = 2, podcastIndex = 0) {
                it.copy(
                    playingStatus = EpisodePlayingStatus.COMPLETED,
                    fileType = "audio/mp3",
                    duration = 30.minutes.inWholeSeconds.toDouble(),
                )
            }
            expectNoEvents()

            updatePodcastEpisode(episode1.copy(fileType = "video/mov"))
            assertEquals(listOf(episode2).map(PlaylistEpisode::Available), awaitItem())

            updatePodcastEpisode(episode2.copy(duration = 0.0))
            assertEquals(emptyList<PlaylistEpisode.Available>(), awaitItem())
        }
    }

    @Test
    fun sortEpisodes() = dsl.test {
        insertPodcast(index = 0)
        val episodes1 = insertPodcastEpisode(index = 0, podcastIndex = 0) {
            it.copy(publishedDate = Date(0), addedDate = Date(0), duration = 1.0)
        }
        val episodes2 = insertPodcastEpisode(index = 1, podcastIndex = 0) {
            it.copy(publishedDate = Date(1), addedDate = Date(1), duration = 3.0)
        }
        val episodes3 = insertPodcastEpisode(index = 2, podcastIndex = 0) {
            it.copy(publishedDate = Date(2), addedDate = Date(2), duration = 2.0)
        }
        val episodes4 = insertPodcastEpisode(index = 3, podcastIndex = 0) {
            it.copy(publishedDate = Date(3), addedDate = Date(3), duration = 4.0)
        }

        manager.smartEpisodesFlow(smartRules(), sortType = PlaylistEpisodeSortType.NewestToOldest).test {
            assertEquals(
                listOf(episodes4, episodes3, episodes2, episodes1).map(PlaylistEpisode::Available),
                awaitItem(),
            )
        }

        manager.smartEpisodesFlow(smartRules(), sortType = PlaylistEpisodeSortType.OldestToNewest).test {
            assertEquals(
                listOf(episodes1, episodes2, episodes3, episodes4).map(PlaylistEpisode::Available),
                awaitItem(),
            )
        }

        manager.smartEpisodesFlow(smartRules(), sortType = PlaylistEpisodeSortType.ShortestToLongest).test {
            assertEquals(
                listOf(episodes1, episodes3, episodes2, episodes4).map(PlaylistEpisode::Available),
                awaitItem(),
            )
        }

        manager.smartEpisodesFlow(smartRules(), sortType = PlaylistEpisodeSortType.LongestToShortest).test {
            assertEquals(
                listOf(episodes4, episodes2, episodes3, episodes1).map(PlaylistEpisode::Available),
                awaitItem(),
            )
        }
    }

    @Test
    fun searchEpisodes() = dsl.test {
        insertPodcast(index = 0)
        insertPodcast(index = 1)
        val episodes = List(episodeLimit * 2) { index ->
            insertPodcastEpisode(
                index = index,
                podcastIndex = index % 2,
            )
        }
        val percentEpisode = insertPodcastEpisode(index = 100000, podcastIndex = 0) {
            it.copy(title = "Episode % title")
        }
        val underscoreEpisode = insertPodcastEpisode(index = 200000, podcastIndex = 0) {
            it.copy(title = "Episode _ title")
        }
        val backslashEpisode = insertPodcastEpisode(index = 300000, podcastIndex = 0) {
            it.copy(title = "Episode \\ title")
        }

        manager.smartEpisodesFlow(smartRules(), searchTerm = null).test {
            assertEquals(
                "null search term",
                episodes.take(episodeLimit).map(PlaylistEpisode::Available),
                awaitItem(),
            )
        }

        manager.smartEpisodesFlow(smartRules(), searchTerm = " ").test {
            assertEquals(
                "blank search term",
                episodes.take(episodeLimit).map(PlaylistEpisode::Available),
                awaitItem(),
            )
        }

        manager.smartEpisodesFlow(smartRules(), searchTerm = "podcast title 0").test {
            assertEquals(
                "podcast title search",
                episodes.filterIndexed { index, _ -> index % 2 == 0 }.map(PlaylistEpisode::Available),
                awaitItem(),
            )
        }

        manager.smartEpisodesFlow(smartRules(), searchTerm = "title 7").test {
            assertEquals(
                "episode title search",
                listOf(episodes[7]).map(PlaylistEpisode::Available),
                awaitItem(),
            )
        }

        manager.smartEpisodesFlow(smartRules(), searchTerm = "title 14").test {
            assertEquals(
                "search above episode limit",
                listOf(episodes[14]).map(PlaylistEpisode::Available),
                awaitItem(),
            )
        }

        manager.smartEpisodesFlow(smartRules(), searchTerm = "%").test {
            assertEquals(
                "percent character",
                listOf(percentEpisode).map(PlaylistEpisode::Available),
                awaitItem(),
            )
        }

        manager.smartEpisodesFlow(smartRules(), searchTerm = "_").test {
            assertEquals(
                "underscore character",
                listOf(underscoreEpisode).map(PlaylistEpisode::Available),
                awaitItem(),
            )
        }

        manager.smartEpisodesFlow(smartRules(), searchTerm = "\\").test {
            assertEquals(
                "backslash character",
                listOf(backslashEpisode).map(PlaylistEpisode::Available),
                awaitItem(),
            )
        }
    }

    @Test
    fun observePlaylist() = dsl.test {
        insertPodcast(index = 0)
        insertPodcast(index = 1)

        manager.smartPlaylistFlow("playlist-id-0").test {
            assertNull(awaitItem())

            insertSmartPlaylist(index = 0) {
                it.copy(starred = true)
            }
            val basePlaylist = smartPlaylist(index = 0) { playlist ->
                playlist.copy(smartRules = smartRules { it.copy(starred = StarredRule.Starred) })
            }
            assertEquals(basePlaylist, awaitItem())

            val episodes = transaction {
                listOf(
                    insertPodcastEpisode(index = 0, podcastIndex = 0) { it.copy(isStarred = true) },
                    insertPodcastEpisode(index = 1, podcastIndex = 0) { it.copy(isStarred = true) },
                    insertPodcastEpisode(index = 2, podcastIndex = 1) { it.copy(isStarred = true) },
                    insertPodcastEpisode(index = 3, podcastIndex = 1),
                )
            }
            assertEquals(
                basePlaylist.copy(
                    episodes = episodes.take(3).map(PlaylistEpisode::Available),
                    metadata = basePlaylist.metadata.copy(
                        totalEpisodeCount = 3,
                        displayedEpisodeCount = 3,
                        displayedAvailableEpisodeCount = 3,
                        artworkUuids = listOf("podcast-id-0", "podcast-id-1"),
                    ),
                ),
                awaitItem(),
            )

            manager.updateSmartRules("playlist-id-0", smartRules())
            assertEquals(
                basePlaylist.copy(
                    episodes = episodes.map(PlaylistEpisode::Available),
                    smartRules = smartRules(),
                    metadata = basePlaylist.metadata.copy(
                        totalEpisodeCount = 4,
                        displayedEpisodeCount = 4,
                        displayedAvailableEpisodeCount = 4,
                        artworkUuids = listOf("podcast-id-0", "podcast-id-1"),
                    ),
                ),
                awaitItem(),
            )

            manager.updateSortType("playlist-id-0", PlaylistEpisodeSortType.OldestToNewest)
            assertEquals(
                basePlaylist.copy(
                    episodes = episodes.reversed().map(PlaylistEpisode::Available),
                    smartRules = smartRules(),
                    settings = basePlaylist.settings.copy(
                        sortType = PlaylistEpisodeSortType.OldestToNewest,
                    ),
                    metadata = basePlaylist.metadata.copy(
                        totalEpisodeCount = 4,
                        displayedEpisodeCount = 4,
                        displayedAvailableEpisodeCount = 4,
                        artworkUuids = listOf("podcast-id-1", "podcast-id-0"),
                    ),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun searchPlaylist() = dsl.test {
        insertSmartPlaylist(index = 0)
        insertPodcast(index = 0)
        insertPodcast(index = 1)
        val episodes = List(episodeLimit * 2) { index ->
            insertPodcastEpisode(
                index = index,
                podcastIndex = index % 2,
            )
        }
        val percentEpisode = insertPodcastEpisode(index = 100000, podcastIndex = 0) {
            it.copy(title = "Episode % title")
        }
        val underscoreEpisode = insertPodcastEpisode(index = 200000, podcastIndex = 0) {
            it.copy(title = "Episode _ title")
        }
        val backslashEpisode = insertPodcastEpisode(index = 300000, podcastIndex = 0) {
            it.copy(title = "Episode \\ title")
        }

        manager.smartPlaylistFlow("playlist-id-0", searchTerm = null).test {
            assertEquals(
                "null search term",
                episodes.take(episodeLimit).map(PlaylistEpisode::Available),
                awaitItem()?.episodes,
            )
        }

        manager.smartPlaylistFlow("playlist-id-0", searchTerm = " ").test {
            assertEquals(
                "blank search term",
                episodes.take(episodeLimit).map(PlaylistEpisode::Available),
                awaitItem()?.episodes,
            )
        }

        manager.smartPlaylistFlow("playlist-id-0", searchTerm = "podcast title 0").test {
            assertEquals(
                "podcast title search",
                episodes.filterIndexed { index, _ -> index % 2 == 0 }.map(PlaylistEpisode::Available),
                awaitItem()?.episodes,
            )
        }

        manager.smartPlaylistFlow("playlist-id-0", searchTerm = "title 7").test {
            assertEquals(
                "episode title search",
                listOf(episodes[7]).map(PlaylistEpisode::Available),
                awaitItem()?.episodes,
            )
        }

        manager.smartPlaylistFlow("playlist-id-0", searchTerm = "title 17").test {
            assertEquals(
                "search above episode limit",
                listOf(episodes[17]).map(PlaylistEpisode::Available),
                awaitItem()?.episodes,
            )
        }

        manager.smartPlaylistFlow("playlist-id-0", searchTerm = "%").test {
            assertEquals(
                "percent character",
                listOf(percentEpisode).map(PlaylistEpisode::Available),
                awaitItem()?.episodes,
            )
        }

        manager.smartPlaylistFlow("playlist-id-0", searchTerm = "_").test {
            assertEquals(
                "underscore character",
                listOf(underscoreEpisode).map(PlaylistEpisode::Available),
                awaitItem()?.episodes,
            )
        }

        manager.smartPlaylistFlow("playlist-id-0", searchTerm = "\\").test {
            assertEquals(
                "backslash character",
                listOf(backslashEpisode).map(PlaylistEpisode::Available),
                awaitItem()?.episodes,
            )
        }
    }

    @Test
    fun showFullEpisodeMetadata() = dsl.test {
        insertSmartPlaylist(index = 0)
        insertPodcast(index = 0)
        repeat(episodeLimit * 2) { index ->
            insertPodcastEpisode(index = index, podcastIndex = 0) {
                it.copy(duration = 1.0)
            }
        }

        manager.smartPlaylistFlow("playlist-id-0").test {
            val playlist = awaitItem()
            assertEquals(episodeLimit * 2, playlist?.metadata?.totalEpisodeCount)
            assertEquals(episodeLimit, playlist?.episodes?.size)
            assertEquals((episodeLimit * 2).seconds, playlist?.metadata?.playbackDurationLeft)
        }
    }

    @Test
    fun computeTotalPlaybackDurationLeft() = dsl.test {
        insertSmartPlaylist(index = 0)
        insertPodcast(index = 0)

        manager.smartPlaylistFlow("playlist-id-0").test {
            assertEquals(0.seconds, awaitItem()?.metadata?.playbackDurationLeft)

            insertPodcastEpisode(index = 0, podcastIndex = 0) {
                it.copy(duration = 0.0)
            }
            assertEquals(0.seconds, awaitItem()?.metadata?.playbackDurationLeft)

            insertPodcastEpisode(index = 1, podcastIndex = 0) {
                it.copy(duration = 20.0)
            }
            assertEquals(20.seconds, awaitItem()?.metadata?.playbackDurationLeft)

            insertPodcastEpisode(index = 2, podcastIndex = 0) {
                it.copy(duration = 15.0)
            }
            assertEquals(35.seconds, awaitItem()?.metadata?.playbackDurationLeft)

            insertPodcastEpisode(index = 3, podcastIndex = 0) {
                it.copy(duration = 15.0, playedUpTo = 10.0)
            }
            assertEquals(40.seconds, awaitItem()?.metadata?.playbackDurationLeft)

            // Check when the duration is unknown and playedUpTo can get above it
            insertPodcastEpisode(index = 4, podcastIndex = 0) {
                it.copy(duration = 0.0, playedUpTo = 10.0)
            }
            assertEquals(40.seconds, awaitItem()?.metadata?.playbackDurationLeft)
        }
    }
}
