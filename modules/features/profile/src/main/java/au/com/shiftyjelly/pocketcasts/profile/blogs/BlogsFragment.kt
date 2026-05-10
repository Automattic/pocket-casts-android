package au.com.shiftyjelly.pocketcasts.profile.blogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import com.automattic.eventhorizon.BlogsAddBlogTappedEvent
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private object BlogsRoutes {
    const val EMPTY = "empty"
    const val ADD_BLOG = "add_blog"
}

@AndroidEntryPoint
class BlogsFragment : BaseFragment() {

    @Inject
    lateinit var eventHorizon: EventHorizon

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
                        onAddBlogClick = {
                            eventHorizon.track(BlogsAddBlogTappedEvent)
                            navController.navigate(BlogsRoutes.ADD_BLOG)
                        },
                    )
                }
                composable(BlogsRoutes.ADD_BLOG) {
                    val viewModel = hiltViewModel<AddBlogViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val url by viewModel.url.collectAsStateWithLifecycle()
                    AddBlogPage(
                        state = uiState,
                        url = url,
                        onUrlChange = viewModel::onUrlChange,
                        onBackPress = {
                            if (!viewModel.onBackPressed()) {
                                navController.popBackStack()
                            }
                        },
                        onFindFeeds = {
                            viewModel.onFindFeedsTapped(
                                url = it,
                                onNavigateToPodcast = { uuid ->
                                    navigateToPodcast(uuid)
                                    viewModel.resetToStart()
                                },
                            )
                        },
                        onRetry = {
                            viewModel.onRetryTapped(
                                url = it,
                                onNavigateToPodcast = { uuid ->
                                    navigateToPodcast(uuid)
                                    viewModel.resetToStart()
                                },
                            )
                        },
                        onFeedClick = { webFeed ->
                            viewModel.onFeedSelected(
                                webFeed = webFeed,
                                onNavigateToPodcast = { uuid ->
                                    navigateToPodcast(uuid)
                                    viewModel.resetToStart()
                                },
                            )
                        },
                        onEditUrl = { viewModel.editUrl() },
                    )
                }
            }
        }
    }

    private fun navigateToPodcast(uuid: String) {
        if (!isAdded) return
        val host = activity as? FragmentHostListener ?: return
        val fragment = PodcastFragment.newInstance(
            podcastUuid = uuid,
            sourceView = SourceView.BLOGS,
        )
        host.addFragment(fragment)
    }
}
