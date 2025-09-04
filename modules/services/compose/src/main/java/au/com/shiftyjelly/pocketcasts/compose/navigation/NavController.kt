package au.com.shiftyjelly.pocketcasts.compose.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptionsBuilder

fun NavController.navigateOnce(route: String, builder: NavOptionsBuilder.() -> Unit = {}) {
    navigate(route) {
        builder()
        launchSingleTop = true
    }
}
