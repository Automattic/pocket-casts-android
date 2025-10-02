package au.com.shiftyjelly.pocketcasts.repositories.playlist

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity.Companion.SYNC_STATUS_NOT_SYNCED
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaylistManagerManualTest {
    @get:Rule
    val dsl = PlaylistManagerDsl()

    @Test
    fun createPlaylist() = dsl.test {
        val playlistUuid = manager.createManualPlaylist("Playlist name")

        expectPlaylist(
            manualPlaylistEntity(index = 0) {
                it.copy(
                    uuid = playlistUuid,
                    title = "Playlist name",
                    sortPosition = 0,
                    syncStatus = SYNC_STATUS_NOT_SYNCED,
                )
            },
        )
    }

    @Test
    fun observePlaylist() = dsl.test {
        manager.manualPlaylistFlow("playlist-id-0").test {
            assertNull(awaitItem())

            insertManualPlaylist(index = 0)
            assertEquals(manualPlaylist(index = 0), awaitItem())

            insertManualEpisode(index = 0, podcastIndex = 0, playlistIndex = 0)
            assertEquals(
                manualPlaylist(index = 0) {
                    it.copy(
                        metadata = it.metadata.copy(
                            totalEpisodeCount = 1,
                            displayedEpisodeCount = 1,
                            displayedAvailableEpisodeCount = 0,
                        ),
                        episodes = listOf(
                            unavailableManualEpisode(index = 0, podcastIndex = 0, playlistIndex = 0),
                        ),
                    )
                },
                awaitItem(),
            )

            insertManualEpisode(index = 1, podcastIndex = 1, playlistIndex = 0)
            assertEquals(
                manualPlaylist(index = 0) {
                    it.copy(
                        metadata = it.metadata.copy(
                            totalEpisodeCount = 2,
                            displayedEpisodeCount = 2,
                            displayedAvailableEpisodeCount = 0,
                        ),
                        episodes = listOf(
                            unavailableManualEpisode(index = 0, podcastIndex = 0, playlistIndex = 0),
                            unavailableManualEpisode(index = 1, podcastIndex = 1, playlistIndex = 0),
                        ),
                    )
                },
                awaitItem(),
            )

            insertPodcastEpisode(index = 1, podcastIndex = 1)
            assertEquals(
                manualPlaylist(index = 0) {
                    it.copy(
                        metadata = it.metadata.copy(
                            totalEpisodeCount = 2,
                            displayedEpisodeCount = 2,
                            displayedAvailableEpisodeCount = 1,
                            artworkUuids = listOf("podcast-id-1"),
                        ),
                        episodes = listOf(
                            unavailableManualEpisode(index = 0, podcastIndex = 0, playlistIndex = 0),
                            availablePlaylistEpisode(index = 1, podcastIndex = 1),
                        ),
                    )
                },
                awaitItem(),
            )
        }
    }

    @Test
    fun sortPlaylistEpisodes() = dsl.test {
        insertManualPlaylist(index = 0)

        insertManualEpisode(index = 0, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 2) }
        insertManualEpisode(index = 1, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 3) }
        insertManualEpisode(index = 2, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 1) }
        insertManualEpisode(index = 3, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 0) }
        insertPodcastEpisode(index = 0, podcastIndex = 0) { it.copy(duration = 10.0) }
        insertPodcastEpisode(index = 2, podcastIndex = 0) { it.copy(duration = 60.0) }

        manager.manualPlaylistFlow("playlist-id-0").test {
            skipItems(1)

            manager.updateSortType("playlist-id-0", PlaylistEpisodeSortType.OldestToNewest)
            assertEquals(
                listOf(
                    unavailableManualEpisode(index = 3, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 0) },
                    availablePlaylistEpisode(index = 2, podcastIndex = 0) { it.copy(duration = 60.0) },
                    unavailableManualEpisode(index = 1, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 3) },
                    availablePlaylistEpisode(index = 0, podcastIndex = 0) { it.copy(duration = 10.0) },
                ),
                awaitItem()?.episodes,
            )

            manager.updateSortType("playlist-id-0", PlaylistEpisodeSortType.NewestToOldest)
            assertEquals(
                listOf(
                    availablePlaylistEpisode(index = 0, podcastIndex = 0) { it.copy(duration = 10.0) },
                    unavailableManualEpisode(index = 1, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 3) },
                    availablePlaylistEpisode(index = 2, podcastIndex = 0) { it.copy(duration = 60.0) },
                    unavailableManualEpisode(index = 3, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 0) },
                ),
                awaitItem()?.episodes,
            )

            manager.updateSortType("playlist-id-0", PlaylistEpisodeSortType.ShortestToLongest)
            assertEquals(
                listOf(
                    availablePlaylistEpisode(index = 0, podcastIndex = 0) { it.copy(duration = 10.0) },
                    availablePlaylistEpisode(index = 2, podcastIndex = 0) { it.copy(duration = 60.0) },
                    unavailableManualEpisode(index = 1, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 3) },
                    unavailableManualEpisode(index = 3, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 0) },
                ),
                awaitItem()?.episodes,
            )

            manager.updateSortType("playlist-id-0", PlaylistEpisodeSortType.LongestToShortest)
            assertEquals(
                listOf(
                    availablePlaylistEpisode(index = 2, podcastIndex = 0) { it.copy(duration = 60.0) },
                    availablePlaylistEpisode(index = 0, podcastIndex = 0) { it.copy(duration = 10.0) },
                    unavailableManualEpisode(index = 1, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 3) },
                    unavailableManualEpisode(index = 3, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 0) },
                ),
                awaitItem()?.episodes,
            )

            manager.updateSortType("playlist-id-0", PlaylistEpisodeSortType.DragAndDrop)
            assertEquals(
                listOf(
                    unavailableManualEpisode(index = 3, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 0) },
                    availablePlaylistEpisode(index = 2, podcastIndex = 0) { it.copy(duration = 60.0) },
                    availablePlaylistEpisode(index = 0, podcastIndex = 0) { it.copy(duration = 10.0) },
                    unavailableManualEpisode(index = 1, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 3) },
                ),
                awaitItem()?.episodes,
            )
        }
    }

    @Test
    fun searchPlaylist() = dsl.test {
        insertManualPlaylist(index = 0)
        insertPodcast(index = 0) { it.copy(title = "Podcast ABC") }
        insertPodcast(index = 1) { it.copy(title = "Podcast def") }
        insertPodcastEpisode(index = 0, podcastIndex = 0) { it.copy(title = "Episode ABC") }
        insertPodcastEpisode(index = 1, podcastIndex = 0) { it.copy(title = "Episode def") }
        insertPodcastEpisode(index = 2, podcastIndex = 1) { it.copy(title = "epi % sode") }
        insertPodcastEpisode(index = 3, podcastIndex = 1) { it.copy(title = "epi _ sode") }
        insertPodcastEpisode(index = 4, podcastIndex = 1) { it.copy(title = "epi \\ sode") }
        insertManualEpisode(index = 0, podcastIndex = 0, playlistIndex = 0)
        insertManualEpisode(index = 1, podcastIndex = 0, playlistIndex = 0)
        insertManualEpisode(index = 2, podcastIndex = 1, playlistIndex = 0)
        insertManualEpisode(index = 3, podcastIndex = 1, playlistIndex = 0)
        insertManualEpisode(index = 4, podcastIndex = 1, playlistIndex = 0)
        insertManualEpisode(index = 5, podcastIndex = 0, playlistIndex = 0)

        manager.manualPlaylistFlow("playlist-id-0", searchTerm = null).test {
            assertEquals(
                "null search term",
                listOf(
                    availablePlaylistEpisode(index = 0, podcastIndex = 0) { it.copy(title = "Episode ABC") },
                    availablePlaylistEpisode(index = 1, podcastIndex = 0) { it.copy(title = "Episode def") },
                    availablePlaylistEpisode(index = 2, podcastIndex = 1) { it.copy(title = "epi % sode") },
                    availablePlaylistEpisode(index = 3, podcastIndex = 1) { it.copy(title = "epi _ sode") },
                    availablePlaylistEpisode(index = 4, podcastIndex = 1) { it.copy(title = "epi \\ sode") },
                    unavailableManualEpisode(index = 5, podcastIndex = 0, playlistIndex = 0),
                ),
                awaitItem()?.episodes,
            )
        }

        manager.manualPlaylistFlow("playlist-id-0", searchTerm = " ").test {
            assertEquals(
                "blank term",
                listOf(
                    availablePlaylistEpisode(index = 0, podcastIndex = 0) { it.copy(title = "Episode ABC") },
                    availablePlaylistEpisode(index = 1, podcastIndex = 0) { it.copy(title = "Episode def") },
                    availablePlaylistEpisode(index = 2, podcastIndex = 1) { it.copy(title = "epi % sode") },
                    availablePlaylistEpisode(index = 3, podcastIndex = 1) { it.copy(title = "epi _ sode") },
                    availablePlaylistEpisode(index = 4, podcastIndex = 1) { it.copy(title = "epi \\ sode") },
                    unavailableManualEpisode(index = 5, podcastIndex = 0, playlistIndex = 0),
                ),
                awaitItem()?.episodes,
            )
        }

        manager.manualPlaylistFlow("playlist-id-0", searchTerm = "cast aBc").test {
            assertEquals(
                "podcast search",
                listOf(
                    availablePlaylistEpisode(index = 0, podcastIndex = 0) { it.copy(title = "Episode ABC") },
                    availablePlaylistEpisode(index = 1, podcastIndex = 0) { it.copy(title = "Episode def") },
                    unavailableManualEpisode(index = 5, podcastIndex = 0, playlistIndex = 0),
                ),
                awaitItem()?.episodes,
            )
        }

        manager.manualPlaylistFlow("playlist-id-0", searchTerm = "sode abc").test {
            assertEquals(
                "episode search",
                listOf(
                    availablePlaylistEpisode(index = 0, podcastIndex = 0) { it.copy(title = "Episode ABC") },
                ),
                awaitItem()?.episodes,
            )
        }

        manager.manualPlaylistFlow("playlist-id-0", searchTerm = "%").test {
            assertEquals(
                "percent character",
                listOf(
                    availablePlaylistEpisode(index = 2, podcastIndex = 1) { it.copy(title = "epi % sode") },
                ),
                awaitItem()?.episodes,
            )
        }

        manager.manualPlaylistFlow("playlist-id-0", searchTerm = "_").test {
            assertEquals(
                "underscore character",
                listOf(
                    availablePlaylistEpisode(index = 3, podcastIndex = 1) { it.copy(title = "epi _ sode") },
                ),
                awaitItem()?.episodes,
            )
        }

        manager.manualPlaylistFlow("playlist-id-0", searchTerm = "\\").test {
            assertEquals(
                "backslash character",
                listOf(
                    availablePlaylistEpisode(index = 4, podcastIndex = 1) { it.copy(title = "epi \\ sode") },
                ),
                awaitItem()?.episodes,
            )
        }
    }

    @Test
    fun getEpisodeSources() = dsl.test {
        insertFolder(index = 0)
        insertFolder(index = 1)
        insertFolder(index = 2) { it.copy(deleted = true) }
        insertPodcast(index = 0)
        insertPodcast(index = 1)
        insertPodcast(index = 2) { it.copy(isSubscribed = false) }
        insertPodcast(index = 3, folderIndex = 0)
        insertPodcast(index = 4, folderIndex = 2)
        insertPodcast(index = 5, folderIndex = 0) { it.copy(isSubscribed = false) }

        setNoSubscription()
        assertEquals(
            listOf(
                podcastEpisodeSource(index = 0),
                podcastEpisodeSource(index = 1),
                podcastEpisodeSource(index = 3),
                podcastEpisodeSource(index = 4),
            ),
            manager.getManualEpisodeSources(),
        )

        setPlusSubscription()
        assertEquals(
            listOf(
                podcastEpisodeSource(index = 0),
                podcastEpisodeSource(index = 1),
                podcastEpisodeSource(index = 4),
                folderEpisodeSource(index = 0, podcastIndices = listOf(3)),
            ),
            manager.getManualEpisodeSources(),
        )
    }

    @Test
    fun searchEpisodeSources() = dsl.test {
        setPlusSubscription()

        insertFolder(index = 0) { it.copy(name = "Folder AbC 0") }
        insertPodcast(index = 0, folderIndex = 0) { it.copy(title = "Podcast ABC 0") }
        insertPodcast(index = 1) { it.copy(title = "Podcast abc 1") }
        insertPodcast(index = 2, folderIndex = 0) { it.copy(title = "Podcast DEF 2") }
        insertPodcast(index = 3) { it.copy(title = "Podcast def 3") }

        assertEquals(
            listOf(
                podcastEpisodeSource(index = 1) { it.copy(title = "Podcast abc 1") },
                folderEpisodeSource(index = 0, podcastIndices = listOf(0, 2)) { folderSource ->
                    folderSource.copy(title = "Folder AbC 0")
                },
            ),
            manager.getManualEpisodeSources(searchTerm = "ABC"),
        )

        assertEquals(
            listOf(
                podcastEpisodeSource(index = 3) { it.copy(title = "Podcast def 3") },
                folderEpisodeSource(index = 0, podcastIndices = listOf(0, 2)) { folderSource ->
                    folderSource.copy(title = "Folder AbC 0")
                },
            ),
            manager.getManualEpisodeSources(searchTerm = "def"),
        )

        insertFolder(index = 1) { it.copy(name = "fol % der") }
        insertPodcast(index = 4, folderIndex = 1)
        insertPodcast(index = 5) { it.copy(title = "pod % cast") }

        assertEquals(
            listOf(
                podcastEpisodeSource(index = 5) { it.copy(title = "pod % cast") },
                folderEpisodeSource(index = 1, podcastIndices = listOf(4)) { folderSource ->
                    folderSource.copy(title = "fol % der")
                },
            ),
            manager.getManualEpisodeSources(searchTerm = "%"),
        )

        insertFolder(index = 2) { it.copy(name = "fol _ der") }
        insertPodcast(index = 6, folderIndex = 2)
        insertPodcast(index = 7) { it.copy(title = "pod _ cast") }

        assertEquals(
            listOf(
                podcastEpisodeSource(index = 7) { it.copy(title = "pod _ cast") },
                folderEpisodeSource(index = 2, podcastIndices = listOf(6)) { folderSource ->
                    folderSource.copy(title = "fol _ der")
                },
            ),
            manager.getManualEpisodeSources(searchTerm = "_"),
        )

        insertFolder(index = 3) { it.copy(name = "fol \\ der") }
        insertPodcast(index = 8, folderIndex = 3)
        insertPodcast(index = 9) { it.copy(title = "pod \\ cast") }

        assertEquals(
            listOf(
                podcastEpisodeSource(index = 9) { it.copy(title = "pod \\ cast") },
                folderEpisodeSource(index = 3, podcastIndices = listOf(8)) { folderSource ->
                    folderSource.copy(title = "fol \\ der")
                },
            ),
            manager.getManualEpisodeSources(searchTerm = "\\"),
        )
    }

    @Test
    fun getEpisodeSourcesForFolder() = dsl.test {
        insertFolder(index = 0)
        insertFolder(index = 1)
        insertPodcast(index = 0, folderIndex = 0)
        insertPodcast(index = 1, folderIndex = 0)
        insertPodcast(index = 2, folderIndex = 1)

        assertEquals(
            listOf(
                podcastEpisodeSource(index = 0),
                podcastEpisodeSource(index = 1),
            ),
            manager.getManualEpisodeSourcesForFolder("folder-id-0"),
        )
    }

    @Test
    fun searchEpisodeSourcesForFolder() = dsl.test {
        insertFolder(index = 0)
        insertPodcast(index = 0, folderIndex = 0) { it.copy(title = "Podcast abc 0") }
        insertPodcast(index = 1, folderIndex = 0) { it.copy(title = "pod % cast") }
        insertPodcast(index = 2, folderIndex = 0) { it.copy(title = "pod _ cast") }
        insertPodcast(index = 3, folderIndex = 0) { it.copy(title = "pod \\ cast") }

        assertEquals(
            listOf(
                podcastEpisodeSource(index = 0) { it.copy(title = "Podcast abc 0") },
            ),
            manager.getManualEpisodeSourcesForFolder("folder-id-0", "ABC"),
        )

        assertEquals(
            listOf(
                podcastEpisodeSource(index = 1) { it.copy(title = "pod % cast") },
            ),
            manager.getManualEpisodeSourcesForFolder("folder-id-0", "%"),
        )

        assertEquals(
            listOf(
                podcastEpisodeSource(index = 2) { it.copy(title = "pod _ cast") },
            ),
            manager.getManualEpisodeSourcesForFolder("folder-id-0", "_"),
        )

        assertEquals(
            listOf(
                podcastEpisodeSource(index = 3) { it.copy(title = "pod \\ cast") },
            ),
            manager.getManualEpisodeSourcesForFolder("folder-id-0", "\\"),
        )
    }

    @Test
    fun observeNotAddedEpisodes() = dsl.test {
        insertManualPlaylist(index = 0)

        manager.notAddedManualEpisodesFlow("playlist-id-0", "podcast-id-0").test {
            assertEquals(emptyList<PodcastEpisode>(), awaitItem())

            val episode1 = insertPodcastEpisode(index = 0, podcastIndex = 0)
            assertEquals(listOf(episode1), awaitItem())

            val episode2 = insertPodcastEpisode(index = 1, podcastIndex = 0)
            assertEquals(listOf(episode1, episode2), awaitItem())

            insertPodcastEpisode(index = 2, podcastIndex = 1)
            expectNoEvents()

            insertManualEpisode(index = 1, podcastIndex = 1, playlistIndex = 0)
            expectNoEvents()

            insertManualEpisode(index = 1, podcastIndex = 0, playlistIndex = 0)
            assertEquals(listOf(episode1), awaitItem())
        }
    }

    @Test
    fun searchNotAddedEpisodes() = dsl.test {
        insertManualPlaylist(index = 0)
        manager.notAddedManualEpisodesFlow("playlist-id-0", "podcast-id-0", searchTerm = "abc").test {
            skipItems(1)

            val episode1 = insertPodcastEpisode(index = 0, podcastIndex = 0) { it.copy(title = "episode abc 0") }
            assertEquals(listOf(episode1), awaitItem())

            val episode2 = insertPodcastEpisode(index = 1, podcastIndex = 0) { it.copy(title = "ABC episode 1") }
            assertEquals(listOf(episode1, episode2), awaitItem())

            insertPodcastEpisode(index = 2, podcastIndex = 0) { it.copy(title = "AB episode 2") }
            expectNoEvents()
        }

        insertManualPlaylist(index = 1)
        manager.notAddedManualEpisodesFlow("playlist-id-1", "podcast-id-0", searchTerm = "%").test {
            skipItems(1)

            val episode1 = insertPodcastEpisode(index = 3, podcastIndex = 0) { it.copy(title = "ti % tle") }
            assertEquals(listOf(episode1), awaitItem())
        }

        insertManualPlaylist(index = 2)
        manager.notAddedManualEpisodesFlow("playlist-id-2", "podcast-id-0", searchTerm = "_").test {
            skipItems(1)

            val episode1 = insertPodcastEpisode(index = 4, podcastIndex = 0) { it.copy(title = "ti _ tle") }
            assertEquals(listOf(episode1), awaitItem())
        }

        insertManualPlaylist(index = 3)
        manager.notAddedManualEpisodesFlow("playlist-id-3", "podcast-id-0", searchTerm = "\\").test {
            skipItems(1)

            val episode1 = insertPodcastEpisode(index = 5, podcastIndex = 0) { it.copy(title = "ti \\ tle") }
            assertEquals(listOf(episode1), awaitItem())
        }
    }

    @Test
    fun addEpisodes() = dsl.test {
        insertManualPlaylist(index = 0)
        insertPodcast(index = 0)
        insertPodcastEpisode(index = 0, podcastIndex = 0)
        insertPodcastEpisode(index = 1, podcastIndex = 1)

        val isAdded = manager.addManualEpisode("playlist-id-0", "episode-id-0")
        assertTrue(isAdded)

        expectManualEpisodes(
            playlistIndex = 0,
            manualPlaylistEpisode(index = 0, podcastIndex = 0, playlistIndex = 0) {
                it.copy(
                    episodeSlug = "episode-slug-0",
                    podcastSlug = "podcast-slug-0",
                    isSynced = false,
                )
            },
        )

        manager.addManualEpisode("playlist-id-0", "episode-id-1")
        expectManualEpisodes(
            playlistIndex = 0,
            manualPlaylistEpisode(index = 0, podcastIndex = 0, playlistIndex = 0) {
                it.copy(
                    episodeSlug = "episode-slug-0",
                    podcastSlug = "podcast-slug-0",
                    isSynced = false,
                )
            },
            manualPlaylistEpisode(index = 1, podcastIndex = 1, playlistIndex = 0) {
                it.copy(
                    episodeSlug = "episode-slug-1",
                    podcastSlug = "",
                    isSynced = false,
                )
            },
        )
    }

    @Test
    fun fixSortOrderWhenAddingEpisodes() = dsl.test {
        insertManualPlaylist(index = 0)
        insertPodcast(index = 0)
        repeat(4) { index ->
            insertPodcastEpisode(index = index, podcastIndex = 0)
            insertPodcastEpisode(index = index, podcastIndex = 1)
        }

        manager.addManualEpisode("playlist-id-0", "episode-id-0")
        manager.addManualEpisode("playlist-id-0", "episode-id-1")
        manager.addManualEpisode("playlist-id-0", "episode-id-2")
        manager.deleteManualEpisode("playlist-id-0", "episode-id-1")

        manager.addManualEpisode("playlist-id-0", "episode-id-3")
        expectManualEpisodes(
            playlistIndex = 0,
            manualPlaylistEpisode(index = 0, podcastIndex = 0, playlistIndex = 0) {
                it.copy(
                    sortPosition = 0,
                    isSynced = false,
                )
            },
            manualPlaylistEpisode(index = 2, podcastIndex = 0, playlistIndex = 0) {
                it.copy(
                    sortPosition = 2,
                    isSynced = false,
                )
            },
            manualPlaylistEpisode(index = 3, podcastIndex = 0, playlistIndex = 0) {
                it.copy(
                    sortPosition = 3,
                    isSynced = false,
                )
            },
        )
    }

    @Test
    fun doNotAddUnavailableEpisodes() = dsl.test {
        insertManualPlaylist(index = 0)
        insertPodcast(index = 0)

        val isAdded = manager.addManualEpisode("playlist-id-0", "episode-id-0")
        assertFalse(isAdded)

        expectNoManualEpisodes(playlistIndex = 0)
    }

    @Test
    fun doNotAddEpisodesAboveLimit() = dsl.test {
        insertManualPlaylist(index = 0)
        repeat(episodeLimit) { index ->
            insertManualEpisode(index = index, podcastIndex = 0, playlistIndex = 0)
            insertPodcastEpisode(index = index, podcastIndex = 0)
        }
        insertPodcastEpisode(index = episodeLimit + 1, podcastIndex = 0)

        val isAdded = manager.addManualEpisode("playlist-id-0", "episode-id-${episodeLimit + 1}")
        assertFalse(isAdded)

        expectNoManualEpisodesCount(playlistIndex = 0, count = episodeLimit)
    }

    @Test
    fun doNotFailToAddEpisodeThatIsAlreadyAdded() = dsl.test {
        insertManualPlaylist(index = 0)
        insertPodcastEpisode(index = 0, podcastIndex = 0)
        insertManualEpisode(index = 0, podcastIndex = 0, playlistIndex = 0)

        val isAdded = manager.addManualEpisode("playlist-id-0", "episode-id-0")
        assertTrue(isAdded)
    }

    @Test
    fun deleteEpisodes() = dsl.test {
        insertManualPlaylist(index = 0)
        insertManualEpisode(index = 0, podcastIndex = 0, playlistIndex = 0)
        insertManualEpisode(index = 1, podcastIndex = 0, playlistIndex = 0)
        insertManualEpisode(index = 2, podcastIndex = 0, playlistIndex = 0)
        insertManualEpisode(index = 3, podcastIndex = 0, playlistIndex = 0)
        insertPodcastEpisode(index = 1, podcastIndex = 0)
        insertPodcastEpisode(index = 3, podcastIndex = 0)

        manager.deleteManualEpisodes("playlist-id-0", setOf("episode-id-0", "episode-id-1"))

        expectManualEpisodes(
            playlistIndex = 0,
            manualPlaylistEpisode(index = 2, podcastIndex = 0, playlistIndex = 0),
            manualPlaylistEpisode(index = 3, podcastIndex = 0, playlistIndex = 0),
        )
    }

    @Test
    fun reorderEpisodes() = dsl.test {
        insertManualPlaylist(0) { it.copy(sortType = PlaylistEpisodeSortType.NewestToOldest) }
        insertManualEpisode(index = 0, podcastIndex = 0, playlistIndex = 0)
        insertManualEpisode(index = 1, podcastIndex = 0, playlistIndex = 0)
        insertManualEpisode(index = 2, podcastIndex = 0, playlistIndex = 0)
        insertManualEpisode(index = 3, podcastIndex = 0, playlistIndex = 0)

        manager.sortManualEpisodes(
            "playlist-id-0",
            listOf("episode-id-3", "episode-id-1", "episode-id-2", "episode-id-0"),
        )

        expectSortType(playlistIndex = 0, PlaylistEpisodeSortType.DragAndDrop)
        expectManualEpisodes(
            playlistIndex = 0,
            manualPlaylistEpisode(index = 3, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 0) },
            manualPlaylistEpisode(index = 1, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 1) },
            manualPlaylistEpisode(index = 2, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 2) },
            manualPlaylistEpisode(index = 0, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 3) },
        )
    }

    @Test
    fun reorderUnspecifiedEpisodesToBottom() = dsl.test {
        insertManualPlaylist(0) { it.copy(sortType = PlaylistEpisodeSortType.NewestToOldest) }
        insertManualEpisode(index = 0, podcastIndex = 0, playlistIndex = 0)
        insertManualEpisode(index = 1, podcastIndex = 0, playlistIndex = 0)
        insertManualEpisode(index = 2, podcastIndex = 0, playlistIndex = 0)
        insertManualEpisode(index = 3, podcastIndex = 0, playlistIndex = 0)

        manager.sortManualEpisodes(
            "playlist-id-0",
            listOf("episode-id-3", "episode-id-1"),
        )

        expectSortType(playlistIndex = 0, PlaylistEpisodeSortType.DragAndDrop)
        expectManualEpisodes(
            playlistIndex = 0,
            manualPlaylistEpisode(index = 3, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 0) },
            manualPlaylistEpisode(index = 1, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 1) },
            manualPlaylistEpisode(index = 0, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 2) },
            manualPlaylistEpisode(index = 2, podcastIndex = 0, playlistIndex = 0) { it.copy(sortPosition = 3) },
        )
    }

    @Test
    fun observeArchivedEpisodes() = dsl.test {
        insertManualPlaylist(0)
        insertManualEpisode(index = 0, podcastIndex = 1, playlistIndex = 0)
        insertManualEpisode(index = 1, podcastIndex = 0, playlistIndex = 0)
        insertManualEpisode(index = 2, podcastIndex = 0, playlistIndex = 0)
        insertPodcastEpisode(index = 0, podcastIndex = 1) { it.copy(isArchived = true) }
        insertPodcastEpisode(index = 1, podcastIndex = 0)

        manager.manualPlaylistFlow("playlist-id-0").test {
            var playlist = awaitItem()!!
            assertEquals(
                listOf(
                    availablePlaylistEpisode(index = 1, podcastIndex = 0),
                    unavailableManualEpisode(index = 2, podcastIndex = 0, playlistIndex = 0),
                ),
                playlist.episodes,
            )
            assertEquals(
                Playlist.Metadata(
                    playbackDurationLeft = Duration.ZERO,
                    artworkUuids = listOf("podcast-id-0"),
                    isShowingArchived = false,
                    totalEpisodeCount = 3,
                    displayedEpisodeCount = 2,
                    displayedAvailableEpisodeCount = 1,
                    archivedEpisodeCount = 1,
                ),
                playlist.metadata,
            )

            manager.toggleShowArchived("playlist-id-0")
            playlist = awaitItem()!!
            assertEquals(
                listOf(
                    availablePlaylistEpisode(index = 0, podcastIndex = 1) { it.copy(isArchived = true) },
                    availablePlaylistEpisode(index = 1, podcastIndex = 0),
                    unavailableManualEpisode(index = 2, podcastIndex = 0, playlistIndex = 0),
                ),
                playlist.episodes,
            )
            assertEquals(
                Playlist.Metadata(
                    playbackDurationLeft = Duration.ZERO,
                    artworkUuids = listOf("podcast-id-0"),
                    isShowingArchived = true,
                    totalEpisodeCount = 3,
                    displayedEpisodeCount = 3,
                    displayedAvailableEpisodeCount = 2,
                    archivedEpisodeCount = 1,
                ),
                playlist.metadata,
            )
        }
    }

    @Test
    fun observePlaylistPreviewsForEpisodes() = dsl.test {
        insertManualPlaylist(0)
        insertManualPlaylist(1)
        insertManualPlaylist(2)
        insertManualEpisode(index = 0, podcastIndex = 0, playlistIndex = 0)
        insertManualEpisode(index = 1, podcastIndex = 0, playlistIndex = 1)
        insertManualEpisode(index = 2, podcastIndex = 1, playlistIndex = 1)

        manager.playlistPreviewsForEpisodeFlow("episode-id-0").test {
            assertEquals(
                listOf(
                    playlistPreviewForEpisode(index = 0) {
                        it.copy(episodeCount = 1, hasEpisode = true)
                    },
                    playlistPreviewForEpisode(index = 1) {
                        it.copy(episodeCount = 2, hasEpisode = false)
                    },
                    playlistPreviewForEpisode(index = 2),
                ),
                awaitItem(),
            )

            insertManualEpisode(index = 0, podcastIndex = 0, playlistIndex = 1)
            assertEquals(
                listOf(
                    playlistPreviewForEpisode(index = 0) {
                        it.copy(episodeCount = 1, hasEpisode = true)
                    },
                    playlistPreviewForEpisode(index = 1) {
                        it.copy(episodeCount = 3, hasEpisode = true)
                    },
                    playlistPreviewForEpisode(index = 2),
                ),
                awaitItem(),
            )

            deleteManualEpisode(index = 0, playlistIndex = 0)
            assertEquals(
                listOf(
                    playlistPreviewForEpisode(index = 0) {
                        it.copy(episodeCount = 0, hasEpisode = false)
                    },
                    playlistPreviewForEpisode(index = 1) {
                        it.copy(episodeCount = 3, hasEpisode = true)
                    },
                    playlistPreviewForEpisode(index = 2),
                ),
                awaitItem(),
            )

            insertPodcast(index = 1)
            insertPodcastEpisode(index = 2, podcastIndex = 1)
            assertEquals(
                listOf(
                    playlistPreviewForEpisode(index = 0) {
                        it.copy(episodeCount = 0, hasEpisode = false)
                    },
                    playlistPreviewForEpisode(index = 1) {
                        it.copy(episodeCount = 3, hasEpisode = true, artworkPodcastUuids = listOf("podcast-id-1"))
                    },
                    playlistPreviewForEpisode(index = 2),
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun searchPlaylistPreviewsForEpisodes() = dsl.test {
        insertManualPlaylist(0) { it.copy(title = "abc") }
        insertManualPlaylist(1) { it.copy(title = "DeF") }
        insertManualPlaylist(2) { it.copy(title = "play % list") }
        insertManualPlaylist(3) { it.copy(title = "play _ list") }
        insertManualPlaylist(4) { it.copy(title = "play \\ list") }

        manager.playlistPreviewsForEpisodeFlow("episode-id-0", searchTerm = null).test {
            assertEquals(
                "null search term",
                listOf(
                    playlistPreviewForEpisode(index = 0) { it.copy(title = "abc") },
                    playlistPreviewForEpisode(index = 1) { it.copy(title = "DeF") },
                    playlistPreviewForEpisode(index = 2) { it.copy(title = "play % list") },
                    playlistPreviewForEpisode(index = 3) { it.copy(title = "play _ list") },
                    playlistPreviewForEpisode(index = 4) { it.copy(title = "play \\ list") },
                ),
                awaitItem(),
            )
        }

        manager.playlistPreviewsForEpisodeFlow("episode-id-0", searchTerm = " ").test {
            assertEquals(
                "blank term",
                listOf(
                    playlistPreviewForEpisode(index = 0) { it.copy(title = "abc") },
                    playlistPreviewForEpisode(index = 1) { it.copy(title = "DeF") },
                    playlistPreviewForEpisode(index = 2) { it.copy(title = "play % list") },
                    playlistPreviewForEpisode(index = 3) { it.copy(title = "play _ list") },
                    playlistPreviewForEpisode(index = 4) { it.copy(title = "play \\ list") },
                ),
                awaitItem(),
            )
        }

        manager.playlistPreviewsForEpisodeFlow("episode-id-0", searchTerm = "aBc").test {
            assertEquals(
                "playlist search",
                listOf(
                    playlistPreviewForEpisode(index = 0) { it.copy(title = "abc") },
                ),
                awaitItem(),
            )
        }

        manager.playlistPreviewsForEpisodeFlow("episode-id-0", searchTerm = "%").test {
            assertEquals(
                "percent character",
                listOf(
                    playlistPreviewForEpisode(index = 2) { it.copy(title = "play % list") },
                ),
                awaitItem(),
            )
        }

        manager.playlistPreviewsForEpisodeFlow("episode-id-0", searchTerm = "_").test {
            assertEquals(
                "underscore character",
                listOf(
                    playlistPreviewForEpisode(index = 3) { it.copy(title = "play _ list") },
                ),
                awaitItem(),
            )
        }

        manager.playlistPreviewsForEpisodeFlow("episode-id-0", searchTerm = "\\").test {
            assertEquals(
                "backslash character",

                listOf(
                    playlistPreviewForEpisode(index = 4) { it.copy(title = "play \\ list") },
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun computeTotalPlaybackDurationLeft() = dsl.test {
        insertManualPlaylist(index = 0)
        insertPodcast(index = 0)
        repeat(6) { index ->
            insertManualEpisode(index = index, podcastIndex = 0, playlistIndex = 0)
        }

        manager.manualPlaylistFlow("playlist-id-0").test {
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

            insertPodcastEpisode(index = 5, podcastIndex = 0) {
                it.copy(duration = 5.0, isArchived = true)
            }
            assertEquals(40.seconds, awaitItem()?.metadata?.playbackDurationLeft)

            manager.toggleShowArchived("playlist-id-0")
            assertEquals(45.seconds, awaitItem()?.metadata?.playbackDurationLeft)
        }
    }
}
