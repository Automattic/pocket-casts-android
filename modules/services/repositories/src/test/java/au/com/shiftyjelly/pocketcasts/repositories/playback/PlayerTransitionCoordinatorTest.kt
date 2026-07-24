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
    fun `delayed player event retains the transition that bound its source`() = runTest {
        val player = Any()
        val playbackVersion = coordinator.beginTransition()
        coordinator.bindEventSource(player, playbackVersion)

        val newerCommandVersion = coordinator.beginTransition()
        val eventVersion = coordinator.tokenForEventSource(player)

        assertTrue(eventVersion === playbackVersion)
        assertFalse(
            coordinator.runIfCurrent(requireNotNull(eventVersion)) {
                error("A stale player event must not commit")
            },
        )
        assertTrue(
            coordinator.runIfCurrent(newerCommandVersion) {
                coordinator.bindEventSource(player, newerCommandVersion)
            },
        )

        coordinator.clearEventSource(player)
        assertNull(coordinator.tokenForEventSource(player))
    }

    @Test
    fun `newer no-op transition adopts the live player event source`() = runTest {
        val player = Any()
        val playbackVersion = coordinator.beginTransition()
        coordinator.bindEventSource(player, playbackVersion)

        val noOpVersion = coordinator.beginTransition()
        assertTrue(
            coordinator.runIfCurrent(noOpVersion) {
                coordinator.bindEventSource(player, noOpVersion)
            },
        )

        val eventVersion = requireNotNull(coordinator.tokenForEventSource(player))
        assertTrue(eventVersion === noOpVersion)
        assertTrue(coordinator.runIfCurrent(eventVersion) {})
    }
}
