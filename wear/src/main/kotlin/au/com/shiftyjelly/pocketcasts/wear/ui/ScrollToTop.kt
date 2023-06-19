package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import kotlinx.coroutines.launch

object ScrollToTop {
    const val key = "scrollToTop"

    @Composable
    fun handle(
        navController: NavController,
        scrollableState: ScalingLazyListState,
    ) {
        val coroutineScope = rememberCoroutineScope()
        navController.currentBackStackEntry?.savedStateHandle
            ?.getStateFlow(key, false)
            ?.collectAsStateWithLifecycle()?.value?.let { scrollToTop ->
                if (scrollToTop) {
                    coroutineScope.launch {
                        scrollableState.scrollToItem(0)
                    }
                    // Reset once consumed
                    navController.currentBackStackEntry?.savedStateHandle
                        ?.set(key, false)
                }
            }
    }

    // This should only be called after confirming that you have navigated to the
    // screen that you want to scroll to the top of and that desination screen should
    // call handleScrollToTop
    fun initiate(navController: NavController) {
        navController
            .currentBackStackEntry
            ?.savedStateHandle
            ?.set(key, true)
    }
}
