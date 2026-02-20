package au.com.shiftyjelly.pocketcasts.views.helper

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@Composable
fun ObserveBackstack(
    navController: NavHostController,
    onBackstackChange: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(navController, onBackstackChange) {
        val job = lifecycleOwner.lifecycleScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navController.currentBackStackEntryFlow.collect {
                    onBackstackChange()
                }
            }
        }

        onDispose {
            job.cancel()
        }
    }
}

fun Fragment.observeNavControllerBackstack(
    navController: NavHostController,
    onBackstackChanged: () -> Unit,
) {
    viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            navController.currentBackStackEntryFlow.collect {
                onBackstackChanged()
            }
        }
    }
}

fun NavHostController?.getBackstackCount(
    startDestinationRoute: String,
    fallbackCount: Int = 0,
): Int {
    return ComposeNavigationBackHelper.getBackstackCount(this, startDestinationRoute, fallbackCount)
}

fun NavHostController?.handleBackPressed(
    startDestinationRoute: String,
    fallbackHandler: () -> Boolean,
): Boolean {
    return ComposeNavigationBackHelper.handleBackPressed(this, startDestinationRoute, fallbackHandler)
}

fun NavHostController?.handleBackPressed(fallbackHandler: () -> Boolean): Boolean {
    return ComposeNavigationBackHelper.handleBackPressed(this, fallbackHandler)
}

fun NavHostController?.isAtStartDestination(startDestinationRoute: String): Boolean {
    if (this == null) return true
    return currentDestination?.route == startDestinationRoute
}
