package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.navigation.NavController

object ScrollToTop {
    const val KEY = "scrollToTop"

    // This should only be called after confirming that you have navigated to the
    // screen that you want to scroll to the top of and that desination screen should
    // call handleScrollToTop
    fun initiate(navController: NavController) {
        navController
            .currentBackStackEntry
            ?.savedStateHandle
            ?.set(KEY, true)
    }
}
