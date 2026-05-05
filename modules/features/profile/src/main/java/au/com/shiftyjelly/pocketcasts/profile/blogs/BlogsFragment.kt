package au.com.shiftyjelly.pocketcasts.profile.blogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint

private object BlogsRoutes {
    const val EMPTY = "empty"
}

@AndroidEntryPoint
class BlogsFragment : BaseFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppTheme(themeType = theme.activeTheme) {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = BlogsRoutes.EMPTY) {
                composable(BlogsRoutes.EMPTY) {
                    EmptyBlogsPage(
                        onBackPress = { activity?.onBackPressedDispatcher?.onBackPressed() },
                        onAddBlogClick = { },
                    )
                }
            }
        }
    }
}
