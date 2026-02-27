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

class PredictiveBackExtensionsTest {

    @Test
    fun `getBackstackCount extension returns 0 for null controller`() {
        val navController: NavHostController? = null
        assertEquals(0, navController.getBackstackCount("start"))
    }

    @Test
    fun `getBackstackCount extension returns fallback for null controller`() {
        val navController: NavHostController? = null
        assertEquals(10, navController.getBackstackCount("start", fallbackCount = 10))
    }

    @Test
    fun `getBackstackCount extension returns 0 at start destination`() {
        val navController = mock<NavHostController>()
        val destination = mock<NavDestination>()
        whenever(destination.route).thenReturn("start")
        whenever(navController.currentDestination).thenReturn(destination)

        assertEquals(0, navController.getBackstackCount("start"))
    }

    @Test
    fun `getBackstackCount extension returns 1 not at start destination`() {
        val navController = mock<NavHostController>()
        val destination = mock<NavDestination>()
        whenever(destination.route).thenReturn("details")
        whenever(navController.currentDestination).thenReturn(destination)

        assertEquals(1, navController.getBackstackCount("start"))
    }

    @Test
    fun `handleBackPressed extension with start route calls fallback at start`() {
        val navController = mock<NavHostController>()
        val destination = mock<NavDestination>()
        whenever(destination.route).thenReturn("start")
        whenever(navController.currentDestination).thenReturn(destination)

        var fallbackCalled = false
        val result = navController.handleBackPressed("start") {
            fallbackCalled = true
            false
        }

        assertTrue(fallbackCalled)
        assertFalse(result)
    }

    @Test
    fun `handleBackPressed extension pops when not at start`() {
        val navController = mock<NavHostController>()
        val destination = mock<NavDestination>()
        whenever(destination.route).thenReturn("details")
        whenever(navController.currentDestination).thenReturn(destination)
        whenever(navController.popBackStack()).thenReturn(true)

        var fallbackCalled = false
        val result = navController.handleBackPressed("start") {
            fallbackCalled = true
            false
        }

        assertFalse(fallbackCalled)
        assertTrue(result)
        verify(navController).popBackStack()
    }

    @Test
    fun `handleBackPressed extension simplified version pops backstack`() {
        val navController = mock<NavHostController>()
        whenever(navController.popBackStack()).thenReturn(true)

        var fallbackCalled = false
        val result = navController.handleBackPressed {
            fallbackCalled = true
            false
        }

        assertFalse(fallbackCalled)
        assertTrue(result)
        verify(navController).popBackStack()
    }

    @Test
    fun `isAtStartDestination returns true for null controller`() {
        val navController: NavHostController? = null
        assertTrue(navController.isAtStartDestination("start"))
    }

    @Test
    fun `isAtStartDestination returns true when at start`() {
        val navController = mock<NavHostController>()
        val destination = mock<NavDestination>()
        whenever(destination.route).thenReturn("start")
        whenever(navController.currentDestination).thenReturn(destination)

        assertTrue(navController.isAtStartDestination("start"))
    }

    @Test
    fun `isAtStartDestination returns false when not at start`() {
        val navController = mock<NavHostController>()
        val destination = mock<NavDestination>()
        whenever(destination.route).thenReturn("details")
        whenever(navController.currentDestination).thenReturn(destination)

        assertFalse(navController.isAtStartDestination("start"))
    }
}
