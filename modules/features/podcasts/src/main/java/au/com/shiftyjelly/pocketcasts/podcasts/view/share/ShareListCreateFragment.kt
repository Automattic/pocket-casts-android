package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.Modifier
import androidx.fragment.app.viewModels
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class ShareListCreateFragment : BaseFragment() {

    private val viewModel: ShareListCreateViewModel by viewModels()
    private var navHostController: NavHostController? = null

    private object NavRoutes {
        const val PODCASTS = "share_podcasts"
        const val TITLE = "share_tile"
        const val BUILDING = "building"
        const val FAILED = "failed"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            navHostController = rememberNavController()
            val navController = navHostController ?: return@AppThemeWithBackground
            NavHost(
                navController = navController,
                startDestination = NavRoutes.PODCASTS,
                modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)),
            ) {
                composable(NavRoutes.PODCASTS) {
                    ShareListCreatePodcastsPage(
                        onCloseClick = { activity?.finish() },
                        onNextClick = { selectedPodcastsCount ->
                            viewModel.trackShareEvent(
                                AnalyticsEvent.SHARE_PODCASTS_PODCASTS_SELECTED,
                                AnalyticsProp.countMap(selectedPodcastsCount),
                            )
                            navController.navigate(NavRoutes.TITLE)
                        },
                        viewModel = viewModel,
                    )
                }
                composable(NavRoutes.TITLE) {
                    ShareListCreateTitlePage(
                        onBackPress = { navController.popBackStack() },
                        onNextClick = { createShareLink(navController) },
                        viewModel = viewModel,
                    )
                }
                composable(NavRoutes.BUILDING) {
                    ShareListCreateBuildingPage(
                        onCloseClick = { activity?.finish() },
                        viewModel = viewModel,
                    )
                }
                composable(NavRoutes.FAILED) {
                    ShareListCreateFailedPage(
                        onCloseClick = { activity?.finish() },
                        onRetryClick = { createShareLink(navController) },
                    )
                }
            }
        }

        if (!viewModel.isFragmentChangingConfigurations) {
            viewModel.trackShareEvent(AnalyticsEvent.SHARE_PODCASTS_SHOWN)
        }
    }

    private fun createShareLink(navController: NavHostController) {
        viewModel.sharePodcasts(
            context = requireContext(),
            label = getString(LR.string.podcasts_share_via),
            onBefore = { navController.navigate(NavRoutes.BUILDING) },
            onSuccess = { activity?.finish() },
            onFailure = { navController.navigate(NavRoutes.FAILED) },
        )
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }
}

private object AnalyticsProp {
    private const val COUNT = "count"
    fun countMap(count: Int) = mapOf(this.COUNT to count)
}
