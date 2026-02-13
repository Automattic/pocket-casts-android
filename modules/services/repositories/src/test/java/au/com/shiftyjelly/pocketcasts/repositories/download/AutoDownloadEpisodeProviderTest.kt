package au.com.shiftyjelly.pocketcasts.repositories.download

import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.AutoDownloadLimitSetting
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.appreview.TestSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock

class AutoDownloadEpisodeProviderTest {
    private val podcastEpisodes = mutableMapOf<Podcast, List<PodcastEpisode>>()
    private val playlistEpisodes = mutableListOf<PodcastEpisode>()
    private val upNextEpisodes = mutableListOf<BaseEpisode>()
    private val userEpisodes = mutableListOf<UserEpisode>()
    private val isAutoDownloadEnabled = TestSetting(Podcast.AUTO_DOWNLOAD_NEW_EPISODES)
    private val autoDownloadLimit = TestSetting(AutoDownloadLimitSetting.TEN_LATEST_EPISODE)
    private val isUpNextAutoDownloadEnabled = TestSetting(true)
    private val isCloudAutoDownloadEnabled = TestSetting(true)

    private val podcastManager = mock<PodcastManager> {
        on { findSubscribedNoOrder() } doAnswer { podcastEpisodes.keys.toList() }
    }

    private val episodeManager = mock<EpisodeManager> {
        on { findEpisodesByPodcastOrderedSuspend(any()) } doAnswer { invocation ->
            podcastEpisodes[invocation.arguments[0]].orEmpty()
        }
    }

    private val playlistManager = mock<PlaylistManager> {
        on { getAutoDownloadEpisodes() } doAnswer { playlistEpisodes }
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

        assertProviderEpisodes(podcast1 + podcast2)
    }

    @Test
    fun `ignore archived podcast episodes`() = runTest {
        val podcast = listOf(
            podcastEpisode(),
            podcastEpisode {
                isArchived = true
            },
        )
        podcastEpisodes += mapOf(podcast() to podcast)

        assertProviderEpisodes(podcast.take(1))
    }

    @Test
    fun `ignore played podcast episodes`() = runTest {
        val podcast = listOf(
            podcastEpisode(),
            podcastEpisode {
                isCompleted = true
            },
        )
        podcastEpisodes += mapOf(podcast() to podcast)

        assertProviderEpisodes(podcast.take(1))
    }

    @Test
    fun `provide playlist episodes`() = runTest {
        val playlist = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        playlistEpisodes += playlist

        assertProviderEpisodes(playlist)
    }

    @Test
    fun `provide up next episodes`() = runTest {
        val upNext = listOf(podcastEpisode(), userEpisode(), podcastEpisode())
        upNextEpisodes += upNext

        assertProviderEpisodes(upNext)
    }

    @Test
    fun `provide user episodes`() = runTest {
        val user = listOf(userEpisode(), userEpisode(), userEpisode())
        userEpisodes += user

        assertProviderEpisodes(user)
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
    fun `provide episodes from all sources`() = runTest {
        val podcast = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        podcastEpisodes += mapOf(podcast() to podcast)

        val playlist = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        playlistEpisodes += playlist

        val upNext = listOf(podcastEpisode(), userEpisode(), podcastEpisode())
        upNextEpisodes += upNext

        val user = listOf(userEpisode(), userEpisode(), userEpisode())
        userEpisodes += user

        assertProviderEpisodes(podcast + playlist + upNext + user)
    }

    @Test
    fun `do not duplicate episodes`() = runTest {
        val podcast = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val user = listOf(userEpisode(), userEpisode(), userEpisode())

        podcastEpisodes += mapOf(podcast() to podcast)
        playlistEpisodes += podcast
        upNextEpisodes += podcast + user
        userEpisodes += user

        assertProviderEpisodes(podcast + user)
    }

    @Test
    fun `respect global podcast auto download setting`() = runTest {
        val podcast = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        podcastEpisodes += mapOf(podcast() to podcast)

        isAutoDownloadEnabled.set(Podcast.AUTO_DOWNLOAD_OFF)

        assertProviderEpisodes(emptySet())
    }

    @Test
    fun `override global podcast auto download setting`() = runTest {
        val podcast1 = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        val podcast2 = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        podcastEpisodes += mapOf(
            podcast() to podcast1,
            podcast { isAutoDownloadEnabled = true } to podcast2,
        )

        isAutoDownloadEnabled.set(Podcast.AUTO_DOWNLOAD_OFF)

        assertProviderEpisodes(podcast2)
    }

    @Test
    fun `ignore global podcast auto download setting for not podcast sources`() = runTest {
        val playlist = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        playlistEpisodes += playlist

        val upNext = listOf(podcastEpisode(), userEpisode(), podcastEpisode())
        upNextEpisodes += upNext

        val user = listOf(userEpisode(), userEpisode(), userEpisode())
        userEpisodes += user

        isAutoDownloadEnabled.set(Podcast.AUTO_DOWNLOAD_OFF)

        assertProviderEpisodes(playlist + upNext + user)
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
        podcastEpisodes += mapOf(podcast() to podcast)

        val playlist = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        playlistEpisodes += playlist

        val user = listOf(userEpisode(), userEpisode(), userEpisode())
        userEpisodes += user

        isUpNextAutoDownloadEnabled.set(false)

        assertProviderEpisodes(podcast + playlist + user)
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
        podcastEpisodes += mapOf(podcast() to podcast)

        val playlist = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        playlistEpisodes += playlist

        val upNext = listOf(podcastEpisode(), userEpisode(), podcastEpisode())
        upNextEpisodes += upNext

        isCloudAutoDownloadEnabled.set(false)

        assertProviderEpisodes(podcast + playlist + upNext)
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

        assertProviderEpisodes(podcast1.take(1) + podcast2.take(1))
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
        podcastEpisodes += mapOf(podcast() to podcast)

        autoDownloadLimit.set(AutoDownloadLimitSetting.THREE_LATEST_EPISODE)

        val expected = buildList {
            add(podcast[0])
            add(podcast[2])
            add(podcast[4])
        }
        assertProviderEpisodes(expected)
    }

    @Test
    fun `ignore global podcast auto download limit setting for not podcast sources`() = runTest {
        val playlist = listOf(podcastEpisode(), podcastEpisode(), podcastEpisode())
        playlistEpisodes += playlist

        val upNext = listOf(podcastEpisode(), userEpisode(), podcastEpisode())
        upNextEpisodes += upNext

        val user = listOf(userEpisode(), userEpisode(), userEpisode())
        userEpisodes += user

        autoDownloadLimit.set(AutoDownloadLimitSetting.LATEST_EPISODE)

        assertProviderEpisodes(playlist + upNext + user)
    }

    private suspend fun assertProviderEpisodes(episodes: Collection<BaseEpisode>) {
        val expected = episodes.mapTo(mutableSetOf(), BaseEpisode::uuid)
        val actual = provider.getAll()
        assertEquals(episodes.size, actual.size)
        assertEquals(expected, actual)
    }
}

private fun podcast(block: PodcastDsl.() -> Unit = {}): Podcast {
    val dsl = PodcastDsl().apply(block)
    return Podcast(
        uuid = dsl.uuid,
        autoDownloadStatus = if (dsl.isAutoDownloadEnabled) {
            Podcast.AUTO_DOWNLOAD_NEW_EPISODES
        } else {
            Podcast.AUTO_DOWNLOAD_OFF
        },
    )
}

private fun podcastEpisode(block: PodcastEpisodeDsl.() -> Unit = {}): PodcastEpisode {
    val dsl = PodcastEpisodeDsl().apply(block)
    return PodcastEpisode(
        uuid = dsl.uuid,
        publishedDate = Date(),
        isArchived = dsl.isArchived,
        playingStatus = if (dsl.isCompleted) {
            EpisodePlayingStatus.COMPLETED
        } else {
            EpisodePlayingStatus.NOT_PLAYED
        },
    )
}

private fun userEpisode(block: PodcastEpisodeDsl.() -> Unit = {}): UserEpisode {
    val dsl = PodcastEpisodeDsl().apply(block)
    return UserEpisode(
        uuid = dsl.uuid,
        publishedDate = Date(),
        isArchived = dsl.isArchived,
        playingStatus = if (dsl.isCompleted) {
            EpisodePlayingStatus.COMPLETED
        } else {
            EpisodePlayingStatus.NOT_PLAYED
        },
    )
}

private class PodcastDsl {
    val uuid = UUID.randomUUID().toString()
    var isAutoDownloadEnabled = false
}

private class PodcastEpisodeDsl {
    val uuid = UUID.randomUUID().toString()
    var isArchived = false
    var isCompleted = false
}

