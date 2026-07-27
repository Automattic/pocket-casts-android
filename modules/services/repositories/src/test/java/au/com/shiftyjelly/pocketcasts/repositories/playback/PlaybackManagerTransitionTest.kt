@file:OptIn(ExperimentalCoroutinesApi::class)

package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.work.testing.WorkManagerTestInitHelper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeDownloadStatus
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.history.upnext.UpNextHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.AlternateEnclosureManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.automattic.eventhorizon.EventHorizon
import io.reactivex.Maybe
import io.reactivex.Single
import java.io.IOException
import java.util.Date
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class PlaybackManagerTransitionTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val settings = mock<Settings>()
    private val podcastManager = mock<PodcastManager>()
    private val episodeManager = mock<EpisodeManager>()
    private val statsManager = mock<StatsManager>()
    private val playerFactory = mock<PlayerFactory>()
    private val castManager = mock<CastManager>()
    private val playlistManager = mock<PlaylistManager>()
    private val downloadQueue = mock<DownloadQueue>()
    private val upNextQueue = mock<UpNextQueue>()
    private val notificationHelper = mock<NotificationHelper>()
    private val userEpisodeManager = mock<UserEpisodeManager>()
    private val eventHorizon = mock<EventHorizon>()
    private val syncManager = mock<SyncManager>()
    private val bookmarkManager = mock<BookmarkManager>()
    private val showNotesManager = mock<ShowNotesManager>()
    private val chapterManager = mock<ChapterManager>()
    private val sleepTimer = mock<SleepTimer>()
    private val networkWatcherFactory = mock<PlaybackManagerNetworkWatcher.Factory>()
    private val crashLogging = mock<CrashLogging>()
    private val upNextHistoryManager = mock<UpNextHistoryManager>()
    private val notificationManager = mock<NotificationManager>()
    private val autoPlaySelector = mock<AutoPlaySelector>()
    private val browseTreeProvider = mock<BrowseTreeProvider>()
    private val alternateEnclosureManager = mock<AlternateEnclosureManager>()

    private lateinit var applicationScope: CoroutineScope
    private lateinit var playbackManager: PlaybackManager

    @Before
    fun setUp() {
        val application = RuntimeEnvironment.getApplication()
        WorkManagerTestInitHelper.initializeTestWorkManager(application)
        applicationScope = CoroutineScope(SupervisorJob() + coroutineRule.testDispatcher)
        runBlocking {
            whenever(castManager.isConnected()).thenReturn(false)
        }
        playbackManager = PlaybackManager(
            settings = settings,
            podcastManager = podcastManager,
            episodeManager = episodeManager,
            statsManager = statsManager,
            playerManager = playerFactory,
            castManager = castManager,
            application = application,
            playlistManager = playlistManager,
            downloadQueue = downloadQueue,
            upNextQueue = upNextQueue,
            notificationHelper = notificationHelper,
            userEpisodeManager = userEpisodeManager,
            eventHorizon = eventHorizon,
            syncManager = syncManager,
            bookmarkManager = bookmarkManager,
            showNotesManager = showNotesManager,
            chapterManager = chapterManager,
            sleepTimer = sleepTimer,
            playbackManagerNetworkWatcherFactory = networkWatcherFactory,
            applicationScope = applicationScope,
            crashLogging = crashLogging,
            upNextHistoryManager = upNextHistoryManager,
            notificationManager = notificationManager,
            autoPlaySelector = autoPlaySelector,
            browseTreeProvider = browseTreeProvider,
            alternateEnclosureManager = alternateEnclosureManager,
        )
    }

    @After
    fun tearDown() {
        applicationScope.cancel()
    }

    @Test
    fun `remove is not dropped when episode becomes current behind a newer command`() = runTest {
        val blocker = episode("blocker")
        val target = episode("target")
        val other = episode("other")
        var currentEpisode: BaseEpisode? = other
        whenever(upNextQueue.currentEpisode).thenAnswer { currentEpisode }

        val blockerEntered = CompletableDeferred<Unit>()
        val releaseBlocker = CompletableDeferred<Unit>()
        val targetRemoved = CompletableDeferred<Unit>()
        whenever(upNextQueue.removeEpisode(any(), any())).doSuspendableAnswer { invocation ->
            when ((invocation.arguments[0] as BaseEpisode).uuid) {
                blocker.uuid -> {
                    blockerEntered.complete(Unit)
                    releaseBlocker.await()
                }

                target.uuid -> targetRemoved.complete(Unit)
            }
            Unit
        }

        val blockerJob = requireNotNull(
            playbackManager.removeEpisodeAsync(blocker, SourceView.UNKNOWN, userInitiated = false),
        )
        awaitRealTime(blockerEntered)

        val targetJob = requireNotNull(
            playbackManager.removeEpisodeAsync(target, SourceView.UNKNOWN, userInitiated = false),
        )
        val pauseJob = async(start = CoroutineStart.UNDISPATCHED) {
            playbackManager.pauseSuspend(transientLoss = true)
        }
        currentEpisode = target
        releaseBlocker.complete(Unit)

        awaitRealTime(targetRemoved, blockerJob, targetJob, pauseJob)
        verify(upNextQueue, times(1)).removeEpisode(target, false)
        verifyNoInteractions(playerFactory)
    }

    @Test
    fun `current episode removal remains durable when superseded while waiting`() = runTest {
        val blocker = episode("blocker")
        val target = episode("target")
        val next = episode("next")
        var currentEpisode: BaseEpisode? = target
        whenever(upNextQueue.currentEpisode).thenAnswer { currentEpisode }
        val player = mock<Player>()
        whenever(player.episodeUuid).thenReturn(target.uuid)
        playbackManager.player = player
        playbackManager.playbackStateRelay.accept(
            PlaybackState(state = PlaybackState.State.PLAYING),
        )
        whenever(upNextQueue.size).thenReturn(2)
        val playerStopped = CompletableDeferred<Unit>()
        whenever(player.stop()).doSuspendableAnswer {
            playerStopped.complete(Unit)
            Unit
        }

        val blockerEntered = CompletableDeferred<Unit>()
        val releaseBlocker = CompletableDeferred<Unit>()
        val targetRemoved = CompletableDeferred<Unit>()
        whenever(upNextQueue.removeEpisode(any(), any())).doSuspendableAnswer { invocation ->
            when ((invocation.arguments[0] as BaseEpisode).uuid) {
                blocker.uuid -> {
                    blockerEntered.complete(Unit)
                    releaseBlocker.await()
                }

                target.uuid -> {
                    currentEpisode = next
                    targetRemoved.complete(Unit)
                }
            }
            Unit
        }

        val blockerJob = requireNotNull(
            playbackManager.removeEpisodeAsync(blocker, SourceView.UNKNOWN, userInitiated = false),
        )
        awaitRealTime(blockerEntered)

        val targetJob = requireNotNull(
            playbackManager.removeEpisodeAsync(target, SourceView.UNKNOWN, userInitiated = false),
        )
        val pauseJob = async(start = CoroutineStart.UNDISPATCHED) {
            playbackManager.pauseSuspend(transientLoss = true)
        }
        releaseBlocker.complete(Unit)

        awaitRealTime(targetRemoved, blockerJob, targetJob, pauseJob)
        awaitRealTime(playerStopped)
        verify(upNextQueue, times(1)).removeEpisode(target, false)
        verify(player, times(2)).pause()
        verify(player, times(1)).stop()
        assertNull(playbackManager.player)
        verifyNoInteractions(playerFactory)
    }

    @Test
    fun `failed episode switch does not retain outgoing player`() = runTest {
        val outgoingEpisode = episode("outgoing")
        val requestedEpisode = episode("requested")
        var currentEpisode: BaseEpisode? = outgoingEpisode
        whenever(upNextQueue.currentEpisode).thenAnswer { currentEpisode }
        val outgoingPlayer = mock<Player>()
        whenever(outgoingPlayer.episodeUuid).thenReturn(outgoingEpisode.uuid)
        playbackManager.player = outgoingPlayer
        whenever(upNextQueue.playNow(any(), anyOrNull(), any(), anyOrNull())).doSuspendableAnswer {
            currentEpisode = requestedEpisode
            Unit
        }
        val outgoingPlayerStopped = CompletableDeferred<Unit>()
        whenever(outgoingPlayer.stop()).doSuspendableAnswer {
            outgoingPlayerStopped.complete(Unit)
            Unit
        }

        playbackManager.playNowSuspend(requestedEpisode)

        awaitRealTime(outgoingPlayerStopped)
        verify(outgoingPlayer, times(1)).pause()
        verify(outgoingPlayer, times(1)).stop()
        assertNull(playbackManager.player)
        verifyNoInteractions(playerFactory)
    }

    @Test
    fun `failed player configuration tears down partially bound player`() = runTest {
        val requestedEpisode = episode("requested").apply {
            downloadStatus = EpisodeDownloadStatus.Downloaded
            downloadedFilePath = "/tmp/requested.mp3"
        }
        whenever(upNextQueue.currentEpisode).thenReturn(requestedEpisode)
        whenever(episodeManager.findByUuid(requestedEpisode.uuid)).thenReturn(requestedEpisode)

        val partiallyConfiguredPlayer = mock<Player>()
        whenever(partiallyConfiguredPlayer.episodeUuid).thenReturn(null)
        whenever(playerFactory.createSimplePlayer(any())).thenReturn(partiallyConfiguredPlayer)
        whenever(partiallyConfiguredPlayer.setEpisode(any(), any()))
            .thenThrow(IllegalStateException("Player configuration failed"))

        val failure = runCatching {
            playbackManager.playQueueSuspend()
        }.exceptionOrNull()

        if (failure !is IllegalStateException) {
            throw requireNotNull(failure)
        }
        verify(partiallyConfiguredPlayer, times(1)).stop()
        assertNull(playbackManager.player)
    }

    @Test
    fun `removing current episode does not deadlock when next cloud file fails`() = runTest {
        val outgoingEpisode = episode("outgoing")
        val cloudEpisode = UserEpisode(
            uuid = "cloud",
            publishedDate = Date(),
            serverStatus = UserEpisodeServerStatus.UPLOADED,
        )
        var currentEpisode: BaseEpisode? = outgoingEpisode
        whenever(upNextQueue.currentEpisode).thenAnswer { currentEpisode }
        whenever(upNextQueue.size).thenReturn(2)
        whenever(upNextQueue.removeEpisode(any(), any())).doSuspendableAnswer { invocation ->
            currentEpisode = when ((invocation.arguments[0] as BaseEpisode).uuid) {
                outgoingEpisode.uuid -> cloudEpisode
                cloudEpisode.uuid -> null
                else -> currentEpisode
            }
            Unit
        }
        whenever(userEpisodeManager.findEpisodeByUuidRxMaybe(cloudEpisode.uuid))
            .thenReturn(Maybe.just(cloudEpisode))
        whenever(userEpisodeManager.getPlaybackUrlRxSingle(cloudEpisode))
            .thenReturn(Single.error(IOException("expired cloud URL")))
        val autoPlaySetting = mock<UserSetting<Boolean>>()
        whenever(autoPlaySetting.value).thenReturn(false)
        whenever(settings.autoPlayNextEpisodeOnEmpty).thenReturn(autoPlaySetting)

        val outgoingPlayer = mock<Player>()
        whenever(outgoingPlayer.episodeUuid).thenReturn(outgoingEpisode.uuid)
        playbackManager.player = outgoingPlayer

        val removalJob = requireNotNull(
            playbackManager.removeEpisodeAsync(outgoingEpisode, SourceView.UNKNOWN, userInitiated = false),
        )

        awaitRealTime(removalJob)

        verify(upNextQueue, times(1)).removeEpisode(outgoingEpisode, false)
        verify(upNextQueue, times(1)).removeEpisode(cloudEpisode, false)
        assertNull(currentEpisode)
    }

    @Test
    fun `completion resumes loading next episode after newer no-op transition`() = runTest {
        assertCompletionResumesAfterNoOp(CompletionTrigger.PlayerEvent)
    }

    @Test
    fun `skip past end resumes loading next episode after newer no-op transition`() = runTest {
        assertCompletionResumesAfterNoOp(CompletionTrigger.SkipPastEnd)
    }

    private suspend fun assertCompletionResumesAfterNoOp(trigger: CompletionTrigger) {
        val completedEpisode = UserEpisode(
            uuid = "completed",
            publishedDate = Date(),
            serverStatus = UserEpisodeServerStatus.UPLOADED,
        )
        val nextEpisode = episode("next").apply {
            downloadStatus = EpisodeDownloadStatus.Downloaded
            downloadedFilePath = "/tmp/next.mp3"
        }
        var currentEpisode: BaseEpisode? = completedEpisode
        whenever(upNextQueue.currentEpisode).thenAnswer { currentEpisode }
        whenever(upNextQueue.removeEpisode(any(), any())).doSuspendableAnswer {
            currentEpisode = nextEpisode
            Unit
        }
        whenever(episodeManager.findByUuid(nextEpisode.uuid)).thenReturn(nextEpisode)
        whenever(sleepTimer.state).thenReturn(SleepTimerState())

        val disabledSetting = mock<UserSetting<Boolean>>()
        whenever(disabledSetting.value).thenReturn(false)
        whenever(settings.upNextShuffle).thenReturn(disabledSetting)
        whenever(settings.audioOnly).thenReturn(disabledSetting)
        val playbackEffectsSetting = mock<UserSetting<PlaybackEffects>>()
        whenever(playbackEffectsSetting.value).thenReturn(PlaybackEffects())
        whenever(settings.globalPlaybackEffects).thenReturn(playbackEffectsSetting)

        var playerEpisodeUuid: String? = completedEpisode.uuid
        val nextEpisodePaused = CompletableDeferred<Unit>()
        val outgoingPlayer = mock<Player>()
        whenever(outgoingPlayer.episodeUuid).thenAnswer { playerEpisodeUuid }
        whenever(outgoingPlayer.isRemote).thenReturn(true)
        whenever(outgoingPlayer.getCurrentPositionMs()).doSuspendableAnswer {
            if (playerEpisodeUuid == nextEpisode.uuid) {
                nextEpisodePaused.complete(Unit)
            }
            0
        }
        playbackManager.player = outgoingPlayer
        playbackManager.pauseSuspend(transientLoss = true)
        playbackManager.playbackStateRelay.accept(
            PlaybackState(
                state = PlaybackState.State.PAUSED,
                episodeUuid = completedEpisode.uuid,
            ),
        )

        val completionPausedAfterRemoval = CompletableDeferred<Unit>()
        val releaseCompletion = CompletableDeferred<Unit>()
        whenever(userEpisodeManager.deletePlayedEpisodeIfReq(any(), any())).doSuspendableAnswer {
            completionPausedAfterRemoval.complete(Unit)
            releaseCompletion.await()
        }
        val nextEpisodeConfigured = CompletableDeferred<Unit>()
        whenever(outgoingPlayer.setEpisode(any(), any())).thenAnswer { invocation ->
            playerEpisodeUuid = (invocation.arguments[0] as BaseEpisode).uuid
            nextEpisodeConfigured.complete(Unit)
            Unit
        }

        when (trigger) {
            CompletionTrigger.PlayerEvent -> {
                dispatchPlayerEvent(outgoingPlayer, PlayerEvent.Completion(completedEpisode.uuid))
            }

            CompletionTrigger.SkipPastEnd -> {
                whenever(outgoingPlayer.durationMs()).thenReturn(1_000)
                playbackManager.skipForwardSuspend(jumpAmountSeconds = 30)
            }
        }
        awaitRealTime(completionPausedAfterRemoval)

        playbackManager.skipForwardSuspend(jumpAmountSeconds = 30)
        verify(outgoingPlayer, never()).stop()

        releaseCompletion.complete(Unit)
        awaitRealTime(nextEpisodeConfigured)
        awaitRealTime(nextEpisodePaused)
        awaitPlaybackTransitionsSettled()

        verify(upNextQueue, times(1)).removeEpisode(completedEpisode, false)
        verify(episodeManager, times(1))
            .updatePlayingStatusBlocking(completedEpisode, au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus.COMPLETED)
        verify(userEpisodeManager, times(1)).deletePlayedEpisodeIfReq(completedEpisode, playbackManager)
        verify(outgoingPlayer, times(1)).setEpisode(nextEpisode, false)
        assertSame(outgoingPlayer, playbackManager.player)
    }

    private enum class CompletionTrigger {
        PlayerEvent,
        SkipPastEnd,
    }

    private fun episode(uuid: String) = PodcastEpisode(
        uuid = uuid,
        publishedDate = Date(),
        downloadUrl = "https://example.com/$uuid.mp3",
    )

    private fun dispatchPlayerEvent(player: Player, event: PlayerEvent) {
        PlaybackManager::class.java
            .getDeclaredMethod("onPlayerEvent", Player::class.java, PlayerEvent::class.java)
            .apply { isAccessible = true }
            .invoke(playbackManager, player, event)
    }

    private suspend fun awaitPlaybackTransitionsSettled() {
        val coordinator = PlaybackManager::class.java
            .getDeclaredField("playerTransitions")
            .apply { isAccessible = true }
            .get(playbackManager) as PlayerTransitionCoordinator
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                coordinator.awaitSettledSnapshot()
            }
        }
    }

    private suspend fun awaitRealTime(
        deferred: CompletableDeferred<Unit>,
        vararg jobs: kotlinx.coroutines.Job,
    ) {
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                deferred.await()
                jobs.asList().joinAll()
            }
        }
    }

    private suspend fun awaitRealTime(vararg jobs: kotlinx.coroutines.Job) {
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                jobs.asList().joinAll()
            }
        }
    }
}
