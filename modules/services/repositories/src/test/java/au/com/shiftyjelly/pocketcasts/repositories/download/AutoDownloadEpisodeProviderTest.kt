package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.type.AutoDownloadLimitSetting
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.appreview.TestSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playlist.ManualPlaylist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import java.util.Date
import java.util.UUID

class AutoDownloadEpisodeProviderTest {
    private val podcastEpisodes = mutableMapOf<Podcast, List<PodcastEpisode>>()
    private val playlists = mutableListOf<Playlist>()
    private val upNextEpisodes = mutableListOf<BaseEpisode>()
    private val userEpisodes = mutableListOf<UserEpisode>()
    private val isAutoDownloadEnabled = TestSetting(Podcast.AUTO_DOWNLOAD_NEW_EPISODES)
    private val autoDownloadLimit = TestSetting(AutoDownloadLimitSetting.TEN_LATEST_EPISODE)
    private val isUpNextAutoDownloadEnabled = TestSetting(true)
    private val isCloudAutoDownloadEnabled = TestSetting(true)
    private val cachedSubscription = TestSetting<Subscription?>(Subscription.PlusPreview)

    private val podcastManager = mock<PodcastManager> {
        on { findSubscribedNoOrder() } doAnswer { podcastEpisodes.keys.toList() }
    }

    private val episodeManager = mock<EpisodeManager> {
        on { findEpisodesByPodcastOrderedSuspend(any()) } doAnswer { invocation ->
            podcastEpisodes[invocation.arguments[0]].orEmpty()
        }
    }

    private val playlistManager = mock<PlaylistManager> {
        on { getAutoDownloadPlaylists() } doAnswer { playlists }
    }

    private val upNextQueue = mock<UpNextQueue> {
        on { allEpisodes } doAnswer { upNextEpisodes }
    }

    private val userEpisodeManager = mock<UserEpisodeManager> {
        on { findUserEpisodes() } doAnswer { userEpisodes }
    }

    private val settings = mock<Settings> {
        on { autoDownloadNewEpisodes } doAnswer { isAutoDownloadEnabled }
        on { autoDownloadLimit } doAnswer { autoDownloadLimit }
        on { autoDownloadUpNext } doAnswer { isUpNextAutoDownloadEnabled }
        on { cloudAutoDownload } doAnswer { isCloudAutoDownloadEnabled }
        on { cachedSubscription } doAnswer { cachedSubscription }
    }

    private val provider = AutoDownloadEpisodeProvider(
        podcastManager = podcastManager,
        episodeManager = episodeManager,
        playlistManager = playlistManager,
        upNextQueue = upNextQueue,
        userEpisodeManager = userEpisodeManager,
        settings = settings,
    )

    @Test
    fun `provide no episodes`() = runTest {
        assertProviderEpisodes(emptySet())
    }

    @Test
    fun `provide podcast episodes`() = runTest {
        val podcast1 = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val podcast2 = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        podcastEpisodes += mapOf(
            podcast() to podcast1,
            podcast() to podcast2,
        )

        assertProviderEpisodes(
            expectedEpisodes = podcast1 + podcast2,
            newPodcastEpisodes = podcast1 + podcast2,
        )
    }

    @Test
    fun `ignore archived podcast episodes`() = runTest {
        val podcast = listOf(
            podcastEpisode(),
            podcastEpisode {
                isArchived = true
            },
        )
        podcastEpisodes += podcast() to podcast

        assertProviderEpisodes(
            expectedEpisodes = podcast.take(1),
            newPodcastEpisodes = podcast,
        )
    }

    @Test
    fun `ignore played podcast episodes`() = runTest {
        val podcast = listOf(
            podcastEpisode(),
            podcastEpisode {
                isCompleted = true
            },
        )
        podcastEpisodes += podcast() to podcast

        assertProviderEpisodes(
            expectedEpisodes = podcast.take(1),
            newPodcastEpisodes = podcast,
        )
    }

    @Test
    fun `ignore disallowed podcast episodes`() = runTest {
        val podcast = listOf(
            podcastEpisode(),
            podcastEpisode {
                isAutoDownloadDisabled = true
            },
        )
        podcastEpisodes += podcast() to podcast

        assertProviderEpisodes(
            expectedEpisodes = podcast.take(1),
            newPodcastEpisodes = podcast,
        )
    }

    @Test
    fun `provide playlist episodes`() = runTest {
        val playlistEpisodes = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val playlist = playlist {
            addEpisodes(playlistEpisodes)
        }
        playlists += playlist

        assertProviderEpisodes(playlistEpisodes)
    }

    @Test
    fun `ignore archived playlist episodes`() = runTest {
        val playlistEpisodes = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val playlist = playlist {
            addEpisodes(playlistEpisodes)
            addEpisode {
                isArchived = true
            }
        }
        playlists += playlist

        assertProviderEpisodes(playlistEpisodes)
    }

    @Test
    fun `ignore played playlist episodes`() = runTest {
        val playlistEpisodes = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val playlist = playlist {
            addEpisodes(playlistEpisodes)
            addEpisode {
                isCompleted = true
            }
        }
        playlists += playlist

        assertProviderEpisodes(playlistEpisodes)
    }

    @Test
    fun `ignore disallowed playlist episodes`() = runTest {
        val playlistEpisodes = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val playlist = playlist {
            addEpisodes(playlistEpisodes)
            addEpisode {
                isAutoDownloadDisabled = true
            }
        }
        playlists += playlist

        assertProviderEpisodes(playlistEpisodes)
    }

    @Test
    fun `ignore unavailable playlist episodes`() = runTest {
        val playlistEpisodes = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val playlist = playlist {
            addEpisodes(playlistEpisodes)
            addEpisode {
                isPlaylistAvailable = false
            }
        }
        playlists += playlist

        assertProviderEpisodes(playlistEpisodes)
    }

    @Test
    fun `respect playlist limit setting`() = runTest {
        val playlistEpisodes = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val playlist = playlist {
            autoDownloadLimit = 2
            addEpisode {
                isPlaylistAvailable = false
            }
            addEpisodes(playlistEpisodes)
            addEpisode {
                isPlaylistAvailable = false
            }
        }
        playlists += playlist

        assertProviderEpisodes(playlistEpisodes.take(2))
    }

    @Test
    fun `provide up next episodes`() = runTest {
        val upNext = listOf(podcastEpisode(), userEpisode(), podcastEpisode())
        upNextEpisodes += upNext

        assertProviderEpisodes(upNext)
    }

    @Test
    fun `ignore archived up next episodes`() = runTest {
        val upNext = listOf(
            podcastEpisode(),
            userEpisode(),
            podcastEpisode(),
            podcastEpisode {
                isArchived = true
            },
            userEpisode {
                isArchived = true
            },
        )
        upNextEpisodes += upNext

        assertProviderEpisodes(upNext.take(3))
    }

    @Test
    fun `ignore played up next episodes`() = runTest {
        val upNext = listOf(
            podcastEpisode(),
            userEpisode(),
            podcastEpisode(),
            podcastEpisode {
                isCompleted = true
            },
            userEpisode {
                isCompleted = true
            },
        )
        upNextEpisodes += upNext

        assertProviderEpisodes(upNext.take(3))
    }

    @Test
    fun `ignore disallowed up next episodes`() = runTest {
        val upNext = listOf(
            podcastEpisode(),
            userEpisode(),
            podcastEpisode(),
            podcastEpisode {
                isAutoDownloadDisabled = true
            },
            userEpisode {
                isAutoDownloadDisabled = true
            },
        )
        upNextEpisodes += upNext

        assertProviderEpisodes(upNext.take(3))
    }

    @Test
    fun `provide user episodes`() = runTest {
        val user = listOf(userEpisode(), userEpisode(), userEpisode())
        userEpisodes += user

        assertProviderEpisodes(user)
    }

    @Test
    fun `do not provide user episodes for unsigned user`() = runTest {
        val user = listOf(userEpisode(), userEpisode(), userEpisode())
        userEpisodes += user

        cachedSubscription.set(value = null)

        assertProviderEpisodes(emptySet())
    }

    @Test
    fun `ignore archived user episodes`() = runTest {
        val user = listOf(
            userEpisode(),
            userEpisode {
                isArchived = true
            },
        )
        userEpisodes += user

        assertProviderEpisodes(user.take(1))
    }

    @Test
    fun `ignore played user episodes`() = runTest {
        val user = listOf(
            userEpisode(),
            userEpisode {
                isCompleted = true
            },
        )
        userEpisodes += user

        assertProviderEpisodes(user.take(1))
    }

    @Test
    fun `ignore disallowed user episodes`() = runTest {
        val user = listOf(
            userEpisode(),
            userEpisode {
                isAutoDownloadDisabled = true
            },
        )
        userEpisodes += user

        assertProviderEpisodes(user.take(1))
    }

    @Test
    fun `provide episodes from all sources`() = runTest {
        val podcast = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        podcastEpisodes += podcast() to podcast

        val playlistEpisodes = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val playlist = playlist {
            addEpisodes(playlistEpisodes)
        }
        playlists += playlist

        val upNext = listOf(podcastEpisode(), userEpisode(), podcastEpisode())
        upNextEpisodes += upNext

        val user = listOf(userEpisode(), userEpisode(), userEpisode())
        userEpisodes += user

        assertProviderEpisodes(
            expectedEpisodes = podcast + playlistEpisodes + upNext + user,
            newPodcastEpisodes = podcast,
        )
    }

    @Test
    fun `do not duplicate episodes`() = runTest {
        val podcast = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val user = listOf(userEpisode(), userEpisode(), userEpisode())

        podcastEpisodes += podcast() to podcast
        playlists += playlist { addEpisodes(podcast) }
        upNextEpisodes += podcast + user
        userEpisodes += user

        assertProviderEpisodes(
            expectedEpisodes = podcast + user,
            newPodcastEpisodes = podcast,
        )
    }

    @Test
    fun `respect global podcast auto download setting`() = runTest {
        val podcast = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        podcastEpisodes += podcast() to podcast

        isAutoDownloadEnabled.set(Podcast.AUTO_DOWNLOAD_OFF)

        assertProviderEpisodes(
            expectedEpisodes = emptySet(),
            newPodcastEpisodes = podcast,
        )
    }

    @Test
    fun `do not override global podcast auto download setting`() = runTest {
        val podcast1 = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val podcast2 = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        podcastEpisodes += mapOf(
            podcast() to podcast1,
            podcast { isAutoDownloadEnabled = true } to podcast2,
        )

        isAutoDownloadEnabled.set(Podcast.AUTO_DOWNLOAD_OFF)

        assertProviderEpisodes(
            expectedEpisodes = emptySet(),
            newPodcastEpisodes = podcast1 + podcast2,
        )
    }

    @Test
    fun `ignore global podcast auto download setting for not podcast sources`() = runTest {
        val playlistEpisodes = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val playlist = playlist {
            addEpisodes(playlistEpisodes)
        }
        playlists += playlist

        val upNext = listOf(podcastEpisode(), userEpisode(), podcastEpisode())
        upNextEpisodes += upNext

        val user = listOf(userEpisode(), userEpisode(), userEpisode())
        userEpisodes += user

        isAutoDownloadEnabled.set(Podcast.AUTO_DOWNLOAD_OFF)

        assertProviderEpisodes(playlistEpisodes + upNext + user)
    }

    @Test
    fun `respect up next auto download setting`() = runTest {
        val upNext = listOf(podcastEpisode(), userEpisode(), podcastEpisode())
        upNextEpisodes += upNext

        isUpNextAutoDownloadEnabled.set(false)

        assertProviderEpisodes(emptySet())
    }

    @Test
    fun `ignore up next auto download setting for not up next sources`() = runTest {
        val podcast = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        podcastEpisodes += podcast() to podcast

        val playlistEpisodes = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val playlist = playlist {
            addEpisodes(playlistEpisodes)
        }
        playlists += playlist

        val user = listOf(userEpisode(), userEpisode(), userEpisode())
        userEpisodes += user

        isUpNextAutoDownloadEnabled.set(false)

        assertProviderEpisodes(
            expectedEpisodes = podcast + playlistEpisodes + user,
            newPodcastEpisodes = podcast,
        )
    }

    @Test
    fun `respect user auto download setting`() = runTest {
        val user = listOf(userEpisode(), userEpisode(), userEpisode())
        userEpisodes += user

        isCloudAutoDownloadEnabled.set(false)

        assertProviderEpisodes(emptySet())
    }

    @Test
    fun `ignore user auto download setting for not user sources`() = runTest {
        val podcast = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        podcastEpisodes += podcast() to podcast

        val playlistEpisodes = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val playlist = playlist {
            addEpisodes(playlistEpisodes)
        }
        playlists += playlist

        val upNext = listOf(podcastEpisode(), userEpisode(), podcastEpisode())
        upNextEpisodes += upNext

        isCloudAutoDownloadEnabled.set(false)

        assertProviderEpisodes(
            expectedEpisodes = podcast + playlistEpisodes + upNext,
            newPodcastEpisodes = podcast,
        )
    }

    @Test
    fun `respect global podcast auto download limit setting`() = runTest {
        val podcast1 = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val podcast2 = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        podcastEpisodes += mapOf(
            podcast() to podcast1,
            podcast() to podcast2,
        )

        autoDownloadLimit.set(AutoDownloadLimitSetting.LATEST_EPISODE)

        assertProviderEpisodes(
            expectedEpisodes = podcast1.take(1) + podcast2.take(1),
            newPodcastEpisodes = podcast1 + podcast2,
        )
    }

    @Test
    fun `do not count ignored episodes for global podcast auto download limit setting`() = runTest {
        val podcast = listOf(
            podcastEpisode(),
            podcastEpisode { isCompleted = true },
            podcastEpisode(),
            podcastEpisode { isArchived = true },
            podcastEpisode(),
            podcastEpisode(),
        )
        podcastEpisodes += podcast() to podcast

        autoDownloadLimit.set(AutoDownloadLimitSetting.THREE_LATEST_EPISODE)

        val expected = buildList {
            add(podcast[0])
            add(podcast[2])
            add(podcast[4])
        }
        assertProviderEpisodes(
            expectedEpisodes = expected,
            newPodcastEpisodes = podcast,
        )
    }

    @Test
    fun `ignore global podcast auto download limit setting for not podcast sources`() = runTest {
        val playlistEpisodes = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val playlist = playlist {
            addEpisodes(playlistEpisodes)
        }
        playlists += playlist

        val upNext = listOf(podcastEpisode(), userEpisode(), podcastEpisode())
        upNextEpisodes += upNext

        val user = listOf(userEpisode(), userEpisode(), userEpisode())
        userEpisodes += user

        autoDownloadLimit.set(AutoDownloadLimitSetting.LATEST_EPISODE)

        assertProviderEpisodes(playlistEpisodes + upNext + user)
    }

    @Test
    fun `provide podcast episodes only from new episodes collection`() = runTest {
        val newEpisodes1 = listOf(podcastEpisode(), podcastEpisode())
        val newEpisodes2 = listOf(podcastEpisode(), podcastEpisode())
        val podcast = newEpisodes1 + listOf(podcastEpisode(), podcastEpisode()) + newEpisodes2
        podcastEpisodes += podcast() to podcast

        autoDownloadLimit.set(AutoDownloadLimitSetting.THREE_LATEST_EPISODE)

        assertProviderEpisodes(
            newPodcastEpisodes = newEpisodes1 + newEpisodes2,
            expectedEpisodes = newEpisodes1 + newEpisodes2.take(1),
        )
    }

    @Test
    fun `provide podcast episodes only from new episodes collection in limit`() = runTest {
        val newEpisodes = listOf(podcastEpisode(), podcastEpisode())
        val podcast = newEpisodes + listOf(podcastEpisode(), podcastEpisode())
        podcastEpisodes += podcast() to podcast

        assertProviderEpisodes(
            newPodcastEpisodes = newEpisodes,
            expectedEpisodes = newEpisodes,
        )
    }

    private suspend fun assertProviderEpisodes(
        expectedEpisodes: Collection<BaseEpisode>,
        newPodcastEpisodes: Collection<BaseEpisode> = emptySet(),
    ) {
        val expected = expectedEpisodes.mapTo(mutableSetOf(), BaseEpisode::uuid)
        val actual = provider.getAll(newPodcastEpisodes.map(BaseEpisode::uuid))
        assertEquals(expectedEpisodes.size, actual.size)
        assertEquals(expected, actual)
    }
}

private fun podcast(block: PodcastDsl.() -> Unit = {}): Podcast {
    return PodcastDsl().apply(block).toPodcast()
}

private fun playlist(block: PlaylistDsl.() -> Unit = {}): Playlist {
    return PlaylistDsl().apply(block).toPlaylist()
}

private fun podcastEpisode(block: EpisodeDsl.() -> Unit = {}): PodcastEpisode {
    return EpisodeDsl().apply(block).toPodcastEpisode()
}

private fun userEpisode(block: EpisodeDsl.() -> Unit = {}): UserEpisode {
    return EpisodeDsl().apply(block).toUserEpisode()
}

@DslMarker private annotation class TestingDsl

@TestingDsl
private class PodcastDsl {
    val uuid = UUID.randomUUID().toString()
    var isAutoDownloadEnabled = true

    fun toPodcast() = Podcast(
        uuid = uuid,
        autoDownloadStatus = if (isAutoDownloadEnabled) {
            Podcast.AUTO_DOWNLOAD_NEW_EPISODES
        } else {
            Podcast.AUTO_DOWNLOAD_OFF
        },
    )
}

@TestingDsl
private class PlaylistDsl {
    val uuid = UUID.randomUUID().toString()
    var autoDownloadLimit = 10
    var episodes = emptyList<PlaylistEpisode>()

    fun addEpisodes(episodes: Collection<PodcastEpisode>) {
        this.episodes += episodes.map(PlaylistEpisode::Available)
    }

    fun addEpisode(block: EpisodeDsl.() -> Unit = {}) {
        episodes += EpisodeDsl().apply(block).toPlaylistEpisode()
    }

    fun toPlaylist() = ManualPlaylist(
        uuid = uuid,
        title = "",
        episodes = episodes.toList(),
        settings = Playlist.Settings.ForPreview.copy(autoDownloadLimit = autoDownloadLimit),
        metadata = Playlist.Metadata.ForPreview,
    )
}

@TestingDsl
private class EpisodeDsl {
    val uuid = UUID.randomUUID().toString()
    var isArchived = false
    var isCompleted = false
    var isPlaylistAvailable = true
    var isAutoDownloadDisabled = false

    fun toPodcastEpisode() = PodcastEpisode(
        uuid = uuid,
        publishedDate = Date(),
        isArchived = isArchived,
        playingStatus = if (isCompleted) {
            EpisodePlayingStatus.COMPLETED
        } else {
            EpisodePlayingStatus.NOT_PLAYED
        },
        autoDownloadStatus = if (isAutoDownloadDisabled) {
            BaseEpisode.AUTO_DOWNLOAD_STATUS_IGNORE
        } else {
            BaseEpisode.AUTO_DOWNLOAD_STATUS_ALLOW
        },
    )

    fun toUserEpisode() = UserEpisode(
        uuid = uuid,
        publishedDate = Date(),
        isArchived = isArchived,
        playingStatus = if (isCompleted) {
            EpisodePlayingStatus.COMPLETED
        } else {
            EpisodePlayingStatus.NOT_PLAYED
        },
        autoDownloadStatus = if (isAutoDownloadDisabled) {
            BaseEpisode.AUTO_DOWNLOAD_STATUS_IGNORE
        } else {
            BaseEpisode.AUTO_DOWNLOAD_STATUS_ALLOW
        },
    )

    fun toPlaylistEpisode() = if (isPlaylistAvailable) {
        PlaylistEpisode.Available(toPodcastEpisode())
    } else {
        PlaylistEpisode.Unavailable(ManualPlaylistEpisode.test(episodeUuid = uuid))
    }
}
