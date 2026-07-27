@file:OptIn(ExperimentalCoroutinesApi::class)

package au.com.shiftyjelly.pocketcasts.repositories.playback

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerTransitionCoordinatorTest {
    private val coordinator = PlayerTransitionCoordinator()

    @Test
    fun `newer transition wins when older preparation finishes last`() = runTest {
        val olderPrepared = CompletableDeferred<Unit>()
        val releaseOlder = CompletableDeferred<Unit>()
        val commits = mutableListOf<String>()

        val olderVersion = coordinator.beginTransition()
        val older = async {
            olderPrepared.complete(Unit)
            releaseOlder.await()
            coordinator.runIfCurrent(olderVersion) {
                commits += "older"
            }
        }
        olderPrepared.await()

        val newerVersion = coordinator.beginTransition()
        val newerCommitted = coordinator.runIfCurrent(newerVersion) {
            commits += "newer"
        }

        releaseOlder.complete(Unit)

        assertTrue(newerCommitted)
        assertFalse(older.await())
        assertEquals(listOf("newer"), commits)
    }

    @Test
    fun `commits for one transition are serialized`() = runTest {
        val transitionVersion = coordinator.beginTransition()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val commits = mutableListOf<String>()
        var activeCommits = 0
        var maxActiveCommits = 0

        val first = launch {
            coordinator.runIfCurrent(transitionVersion) {
                activeCommits++
                maxActiveCommits = maxOf(maxActiveCommits, activeCommits)
                firstEntered.complete(Unit)
                releaseFirst.await()
                commits += "first"
                activeCommits--
            }
        }
        firstEntered.await()

        val second = launch {
            coordinator.runIfCurrent(transitionVersion) {
                activeCommits++
                maxActiveCommits = maxOf(maxActiveCommits, activeCommits)
                commits += "second"
                activeCommits--
            }
        }
        runCurrent()

        assertFalse(second.isCompleted)

        releaseFirst.complete(Unit)
        joinAll(first, second)

        assertEquals(1, maxActiveCommits)
        assertEquals(listOf("first", "second"), commits)
    }

    @Test
    fun `queued commit rechecks version after acquiring mutex`() = runTest {
        val firstVersion = coordinator.beginTransition()
        val firstEntered = CompletableDeferred<Unit>()
        val releaseFirst = CompletableDeferred<Unit>()
        val commits = mutableListOf<String>()

        val first = async {
            coordinator.runIfCurrent(firstVersion) {
                firstEntered.complete(Unit)
                releaseFirst.await()
                commits += "first"
            }
        }
        firstEntered.await()

        val queuedVersion = coordinator.beginTransition()
        val queued = async {
            coordinator.runIfCurrent(queuedVersion) {
                commits += "queued"
            }
        }
        runCurrent()

        val latestVersion = coordinator.beginTransition()
        releaseFirst.complete(Unit)

        assertTrue(first.await())
        assertFalse(queued.await())
        assertTrue(
            coordinator.runIfCurrent(latestVersion) {
                commits += "latest"
            },
        )
        assertEquals(listOf("first", "latest"), commits)
    }

    @Test
    fun `unchanged snapshot can begin a transition`() {
        val snapshot = coordinator.snapshot()

        val transition = requireNotNull(coordinator.tryBeginTransition(snapshot))

        assertTrue(coordinator.isCurrent(transition))
    }

    @Test
    fun `snapshot cannot supersede a newer transition`() {
        val snapshot = coordinator.snapshot()
        val newerTransition = coordinator.beginTransition()

        assertNull(coordinator.tryBeginTransition(snapshot))
        assertTrue(coordinator.isCurrent(newerTransition))
    }

    @Test
    fun `snapshot can begin only one transition`() {
        val snapshot = coordinator.snapshot()

        val firstTransition = requireNotNull(coordinator.tryBeginTransition(snapshot))

        assertNull(coordinator.tryBeginTransition(snapshot))
        assertTrue(coordinator.isCurrent(firstTransition))
    }

    @Test
    fun `in-flight command does not invalidate callbacks from live player configuration`() = runTest {
        val player = Any()
        bindSettledSource(player)
        val eventSourceToken = requireNotNull(coordinator.tokenForEventSource(player))

        coordinator.beginTransition()

        assertTrue(coordinator.isCurrentEventSource(eventSourceToken))
    }

    @Test
    fun `terminal event waits for in-flight command and resumes after same-source completion`() = runTest {
        val player = Any()
        bindSettledSource(player)
        val eventSourceToken = requireNotNull(coordinator.tokenForEventSource(player))

        val inFlightTransition = coordinator.beginTransition()
        val terminalTransition = async {
            coordinator.beginTransitionForEventSource(eventSourceToken)
        }
        runCurrent()

        assertFalse(terminalTransition.isCompleted)
        assertTrue(coordinator.isCurrent(inFlightTransition))

        assertTrue(coordinator.completeTransition(inFlightTransition) { player })

        assertTrue(coordinator.isCurrent(requireNotNull(terminalTransition.await()).token))
    }

    @Test
    fun `source reconfiguration invalidates terminal event waiting behind a command`() = runTest {
        val player = Any()
        bindSettledSource(player)
        val oldEventSourceToken = requireNotNull(coordinator.tokenForEventSource(player))

        val reconfiguration = coordinator.beginTransition()
        val terminalTransition = async {
            coordinator.beginTransitionForEventSource(oldEventSourceToken)
        }
        runCurrent()

        assertTrue(
            coordinator.runIfCurrent(reconfiguration) {
                coordinator.bindEventSource(player, reconfiguration)
            },
        )
        assertTrue(coordinator.completeTransition(reconfiguration) { player })

        assertNull(terminalTransition.await())
    }

    @Test
    fun `same source completion preserves passive events and allows waiting terminal event`() = runTest {
        val player = Any()
        bindSettledSource(player)
        val oldEventSourceToken = requireNotNull(coordinator.tokenForEventSource(player))

        val completedTransition = coordinator.beginTransition()
        assertTrue(coordinator.completeTransition(completedTransition) { player })

        assertTrue(coordinator.isCurrentEventSource(oldEventSourceToken))
        assertTrue(
            coordinator.isCurrent(
                requireNotNull(coordinator.beginTransitionForEventSource(oldEventSourceToken)).token,
            ),
        )
    }

    @Test
    fun `completion waits for in-progress player commit before adopting source`() = runTest {
        val player = Any()
        val playerCommitEntered = CompletableDeferred<Unit>()
        val releasePlayerCommit = CompletableDeferred<Unit>()
        val playerTransition = coordinator.beginTransition()
        val playerCommit = async {
            coordinator.runIfCurrent(playerTransition) {
                playerCommitEntered.complete(Unit)
                releasePlayerCommit.await()
                coordinator.bindEventSource(player, playerTransition)
            }
        }
        playerCommitEntered.await()

        val completedTransition = coordinator.beginTransition()
        val completion = async {
            coordinator.completeTransition(completedTransition) { player }
        }
        runCurrent()

        assertFalse(completion.isCompleted)

        releasePlayerCommit.complete(Unit)

        assertTrue(playerCommit.await())
        assertTrue(completion.await())
        assertEquals(
            completedTransition.version,
            requireNotNull(coordinator.tokenForEventSource(player)).transitionVersion,
        )
    }

    @Test
    fun `stale completion cannot overwrite newer transition state`() = runTest {
        val player = Any()
        val committedTransition = coordinator.beginTransition()
        coordinator.bindEventSource(player, committedTransition)
        val committedEventSourceToken = requireNotNull(coordinator.tokenForEventSource(player))

        val staleTransition = coordinator.beginTransition()
        val currentTransition = coordinator.beginTransition()
        assertFalse(coordinator.completeTransition(staleTransition) { player })

        assertSame(committedEventSourceToken, coordinator.tokenForEventSource(player))
        assertTrue(coordinator.isCurrent(currentTransition))
    }

    @Test
    fun `completion does not adopt an unbound replacement source`() = runTest {
        val previousPlayer = Any()
        val replacementPlayer = Any()
        val previousTransition = coordinator.beginTransition()
        coordinator.bindEventSource(previousPlayer, previousTransition)

        val replacementTransition = coordinator.beginTransition()

        assertTrue(coordinator.completeTransition(replacementTransition) { replacementPlayer })
        assertNull(coordinator.tokenForEventSource(previousPlayer))
        assertNull(coordinator.tokenForEventSource(replacementPlayer))
    }

    @Test
    fun `duplicate terminal event can begin only one transition`() = runTest {
        val player = Any()
        bindSettledSource(player)
        val eventSourceToken = requireNotNull(coordinator.tokenForEventSource(player))

        val firstTerminalTransition = requireNotNull(coordinator.beginTransitionForEventSource(eventSourceToken))

        assertNull(coordinator.beginTransitionForEventSource(eventSourceToken))
        assertNull(coordinator.tokenForEventSource(player))
        assertTrue(coordinator.isCurrent(firstTerminalTransition.token))
    }

    @Test
    fun `terminal source accepts callbacks again only after transition completion`() = runTest {
        val player = Any()
        bindSettledSource(player)
        val eventSourceToken = requireNotNull(coordinator.tokenForEventSource(player))

        val terminalTransition = requireNotNull(coordinator.beginTransitionForEventSource(eventSourceToken))

        assertNull(coordinator.tokenForEventSource(player))
        assertFalse(
            coordinator.runIfEventSourceCurrent(eventSourceToken) {
                error("Outgoing source callback should not run during a terminal transition")
            },
        )

        assertTrue(coordinator.completeTransition(terminalTransition.token) { player })
        assertTrue(
            coordinator.isCurrentEventSource(
                requireNotNull(coordinator.tokenForEventSource(player)),
            ),
        )
    }

    @Test
    fun `terminal claim survives a newer same-source no-op transition`() = runTest {
        val player = Any()
        bindSettledSource(player)
        val eventSourceToken = requireNotNull(coordinator.tokenForEventSource(player))
        val terminalTransition = requireNotNull(coordinator.beginTransitionForEventSource(eventSourceToken))

        val noOpTransition = coordinator.beginTransition()
        assertTrue(coordinator.hasInactiveEventSourceFromEarlierTransition(player, noOpTransition))
        assertFalse(coordinator.hasInactiveEventSourceFromEarlierTransition(player, terminalTransition.token))
        assertTrue(coordinator.completeTransition(noOpTransition) { player })

        assertNull(coordinator.tokenForEventSource(player))
        val retriedTransition = requireNotNull(
            coordinator.retryTransitionForEventSource(terminalTransition.eventSourceToken),
        )
        assertTrue(coordinator.isCurrent(retriedTransition.token))

        assertTrue(coordinator.completeTransition(retriedTransition.token) { player })
        requireNotNull(coordinator.tokenForEventSource(player))
    }

    @Test
    fun `passive event finishes before source can be rebound`() = runTest {
        val player = Any()
        bindSettledSource(player)
        val eventSourceToken = requireNotNull(coordinator.tokenForEventSource(player))
        val passiveEventEntered = CompletableDeferred<Unit>()
        val releasePassiveEvent = CompletableDeferred<Unit>()
        val passiveEvent = async {
            coordinator.runIfEventSourceCurrent(eventSourceToken) {
                passiveEventEntered.complete(Unit)
                releasePassiveEvent.await()
            }
        }
        passiveEventEntered.await()

        val reconfiguration = coordinator.beginTransition()
        val rebind = async {
            coordinator.runIfCurrent(reconfiguration) {
                coordinator.bindEventSource(player, reconfiguration)
            }
        }
        runCurrent()

        assertFalse(rebind.isCompleted)

        releasePassiveEvent.complete(Unit)

        assertTrue(passiveEvent.await())
        assertTrue(rebind.await())
        assertFalse(coordinator.isCurrentEventSource(eventSourceToken))
    }

    @Test
    fun `reconfiguring same player invalidates callbacks from previous generation`() {
        val player = Any()
        val previousTransition = coordinator.beginTransition()
        coordinator.bindEventSource(player, previousTransition)
        val previousGeneration = requireNotNull(coordinator.tokenForEventSource(player))

        val currentTransition = coordinator.beginTransition()
        coordinator.bindEventSource(player, currentTransition)
        val currentGeneration = requireNotNull(coordinator.tokenForEventSource(player))

        assertFalse(coordinator.isCurrentEventSource(previousGeneration))
        assertTrue(coordinator.isCurrentEventSource(currentGeneration))
    }

    @Test
    fun `replacing player invalidates callbacks from previous source`() {
        val previousPlayer = Any()
        val currentPlayer = Any()
        val previousTransition = coordinator.beginTransition()
        coordinator.bindEventSource(previousPlayer, previousTransition)
        val previousSource = requireNotNull(coordinator.tokenForEventSource(previousPlayer))

        val currentTransition = coordinator.beginTransition()
        coordinator.bindEventSource(currentPlayer, currentTransition)

        assertFalse(coordinator.isCurrentEventSource(previousSource))
        assertNull(coordinator.tokenForEventSource(previousPlayer))
        assertTrue(coordinator.isCurrentEventSource(requireNotNull(coordinator.tokenForEventSource(currentPlayer))))
    }

    private suspend fun bindSettledSource(source: Any) {
        val transition = coordinator.beginTransition()
        coordinator.bindEventSource(source, transition)
        assertTrue(coordinator.completeTransition(transition) { source })
    }
}
