package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun GiveRatingNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = routes.giveRatingListenMore) {
        composable(routes.giveRatingListenMore) {
            GiveRatingListenMore()
        }
    }
}

private val routes = object {
    val giveRatingListenMore = "giveRatingListenMore"
}
