package au.com.shiftyjelly.pocketcasts.views.helper

import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ComposeNavigationBackHelperTest {

    @Test
    fun `getBackstackCount returns 0 when navController is null`() {
        val count = ComposeNavigationBackHelper.getBackstackCount(null, "start")
        assertEquals(0, count)
    }

    @Test
    fun `getBackstackCount returns fallback when navController is null`() {
        val count = ComposeNavigationBackHelper.getBackstackCount(null, "start", fallbackCount = 5)
        assertEquals(5, count)
    }

    @Test
    fun `getBackstackCount returns 0 when at start destination`() {
        val navController = mock<NavHostController>()
        val destination = mock<NavDestination>()
        whenever(destination.route).thenReturn("start")
        whenever(navController.currentDestination).thenReturn(destination)

        val count = ComposeNavigationBackHelper.getBackstackCount(navController, "start")
        assertEquals(0, count)
    }

    @Test
    fun `getBackstackCount returns 1 when not at start destination`() {
        val navController = mock<NavHostController>()
        val destination = mock<NavDestination>()
        whenever(destination.route).thenReturn("details")
        whenever(navController.currentDestination).thenReturn(destination)

        val count = ComposeNavigationBackHelper.getBackstackCount(navController, "start")
        assertEquals(1, count)
    }

    @Test
    fun `handleBackPressed with null navController calls fallback`() {
        var fallbackCalled = false
        val result = ComposeNavigationBackHelper.handleBackPressed(
            navController = null,
            fallbackHandler = {
                fallbackCalled = true
                true
            },
        )

        assertTrue(fallbackCalled)
        assertTrue(result)
    }

    @Test
    fun `handleBackPressed at start destination calls fallback`() {
        val navController = mock<NavHostController>()
        val destination = mock<NavDestination>()
        whenever(destination.route).thenReturn("start")
        whenever(navController.currentDestination).thenReturn(destination)

        var fallbackCalled = false
        val result = ComposeNavigationBackHelper.handleBackPressed(
            navController = navController,
            startDestinationRoute = "start",
            fallbackHandler = {
                fallbackCalled = true
                true
            },
        )

        assertTrue(fallbackCalled)
        assertTrue(result)
    }

    @Test
    fun `handleBackPressed not at start destination pops backstack`() {
        val navController = mock<NavHostController>()
        val destination = mock<NavDestination>()
        whenever(destination.route).thenReturn("details")
        whenever(navController.currentDestination).thenReturn(destination)
        whenever(navController.popBackStack()).thenReturn(true)

        var fallbackCalled = false
        val result = ComposeNavigationBackHelper.handleBackPressed(
            navController = navController,
            startDestinationRoute = "start",
            fallbackHandler = {
                fallbackCalled = true
                true
            },
        )

        assertFalse(fallbackCalled)
        assertTrue(result)
        verify(navController).popBackStack()
    }

    @Test
    fun `handleBackPressed simplified version pops backstack`() {
        val navController = mock<NavHostController>()
        whenever(navController.popBackStack()).thenReturn(true)

        var fallbackCalled = false
        val result = ComposeNavigationBackHelper.handleBackPressed(
            navController = navController,
            fallbackHandler = {
                fallbackCalled = true
                false
            },
        )

        assertFalse(fallbackCalled)
        assertTrue(result)
        verify(navController).popBackStack()
    }

    @Test
    fun `handleBackPressed simplified version with null navController calls fallback`() {
        var fallbackCalled = false
        val result = ComposeNavigationBackHelper.handleBackPressed(
            navController = null,
            fallbackHandler = {
                fallbackCalled = true
                false
            },
        )

        assertTrue(fallbackCalled)
        assertFalse(result)
    }
}
