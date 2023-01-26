package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class ShareListCreateFragment : BaseFragment() {

    private val viewModel: ShareListCreateViewModel by viewModels()
    private var navHostController: NavHostController? = null

    private object NavRoutes {
        const val podcasts = "share_podcasts"
        const val title = "share_tile"
        const val building = "building"
        const val failed = "failed"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext()).apply {
        setContent {
            AppThemeWithBackground(theme.activeTheme) {
                navHostController = rememberNavController()
                val navController = navHostController ?: return@AppThemeWithBackground
                NavHost(navController = navController, startDestination = NavRoutes.podcasts) {
                    composable(NavRoutes.podcasts) {
                        ShareListCreatePodcastsPage(
                            onCloseClick = { activity?.finish() },
                            onNextClick = { selectedPodcastsCount ->
                                viewModel.trackShareEvent(
                                    AnalyticsEvent.SHARE_PODCASTS_PODCASTS_SELECTED,
                                    AnalyticsProp.countMap(selectedPodcastsCount)
                                )
                                navController.navigate(NavRoutes.title)
                            },
                            viewModel = viewModel
                        )
                    }
                    composable(NavRoutes.title) {
                        ShareListCreateTitlePage(
                            onBackClick = { navController.popBackStack() },
                            onNextClick = { createShareLink(navController) },
                            viewModel = viewModel
                        )
                    }
                    composable(NavRoutes.building) {
                        ShareListCreateBuildingPage(
                            onCloseClick = { activity?.finish() },
                            viewModel = viewModel
                        )
                    }
                    composable(NavRoutes.failed) {
                        ShareListCreateFailedPage(
                            onCloseClick = { activity?.finish() },
                            onRetryClick = { createShareLink(navController) }
                        )
                    }
                }
            }

            if (!viewModel.isFragmentChangingConfigurations) {
                viewModel.trackShareEvent(AnalyticsEvent.SHARE_PODCASTS_SHOWN)
            }
        }
    }

    private fun createShareLink(navController: NavHostController) {
        viewModel.sharePodcasts(
            context = requireContext(),
            label = getString(LR.string.podcasts_share_via),
            onBefore = { navController.navigate(NavRoutes.building) },
            onSuccess = { activity?.finish() },
            onFailure = { navController.navigate(NavRoutes.failed) }
        )
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }
}

private object AnalyticsProp {
    private const val count = "count"
    fun countMap(count: Int) = mapOf(this.count to count)
}
