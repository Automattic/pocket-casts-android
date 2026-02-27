package au.com.shiftyjelly.pocketcasts.views.helper

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

object ComposeNavigationBackHelper {

    fun observeBackstack(
        navController: NavHostController,
        lifecycleOwner: LifecycleOwner,
        onBackstackChanged: () -> Unit,
    ) {
        lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navController.currentBackStackEntryFlow.collect {
                    onBackstackChanged()
                }
            }
        }
    }

    fun getBackstackCount(
        navController: NavHostController?,
        startDestinationRoute: String,
        fallbackCount: Int = 0,
    ): Int {
        if (navController == null) return fallbackCount
        val isAtRoot = navController.currentDestination?.route == startDestinationRoute
        return if (isAtRoot) 0 else 1
    }

    fun handleBackPressed(
        navController: NavHostController?,
        startDestinationRoute: String,
        fallbackHandler: () -> Boolean,
    ): Boolean {
        if (navController == null) {
            return fallbackHandler()
        }

        val isAtRoot = navController.currentDestination?.route == startDestinationRoute
        return if (isAtRoot) {
            fallbackHandler()
        } else {
            navController.popBackStack()
        }
    }

    fun handleBackPressed(
        navController: NavHostController?,
        fallbackHandler: () -> Boolean,
    ): Boolean {
        if (navController == null) {
            return fallbackHandler()
        }
        return navController.popBackStack()
    }
}
