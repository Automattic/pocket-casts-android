package au.com.shiftyjelly.pocketcasts.profile.blogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.PodcastFragment
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingLauncher
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import com.automattic.eventhorizon.BlogsAddBlogTappedEvent
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow

private object BlogsRoutes {
    const val ADD_BLOG = "add_blog"
    const val PODCASTS = "podcasts"
}

@AndroidEntryPoint
class BlogsFragment : BaseFragment() {

    @Inject
    lateinit var eventHorizon: EventHorizon

    @Inject
    lateinit var userManager: UserManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppTheme(themeType = theme.activeTheme) {
            val isPlusOrPatronFlow = remember {
                userManager.getSignInState().asFlow().map { it.isSignedInAsPlusOrPatron }
            }
            val isPlusOrPatron by isPlusOrPatronFlow.collectAsStateWithLifecycle(initialValue = false)
            val navController = rememberNavController()
            val onAddBlogClick = {
                eventHorizon.track(BlogsAddBlogTappedEvent)
                if (isPlusOrPatron) {
                    navController.navigate(BlogsRoutes.ADD_BLOG)
                } else {
                    OnboardingLauncher.openOnboardingFlow(
                        requireActivity(),
                        OnboardingFlow.Upsell(OnboardingUpgradeSource.BLOGS),
                    )
                }
            }
            NavHost(navController = navController, startDestination = BlogsRoutes.PODCASTS) {
                composable(BlogsRoutes.PODCASTS) {
                    val viewModel = hiltViewModel<BlogsViewModel>()
                    val blogPodcasts by viewModel.blogPodcasts.collectAsStateWithLifecycle()
                    val bottomInsetPx by viewModel.bottomInset.collectAsStateWithLifecycle()
                    val bottomInset = with(LocalDensity.current) { bottomInsetPx.toDp() }
                    val podcasts = blogPodcasts
                    when {
                        podcasts.isNullOrEmpty() -> EmptyBlogsPage(
                            onBackPress = { activity?.onBackPressedDispatcher?.onBackPressed() },
                            onAddBlogClick = onAddBlogClick,
                            showContent = podcasts != null,
                        )

                        else -> BlogsListPage(
                            podcasts = podcasts,
                            bottomInset = bottomInset,
                            onBackPress = { activity?.onBackPressedDispatcher?.onBackPressed() },
                            onAddBlogClick = onAddBlogClick,
                            onPodcastClick = {
                                viewModel.onPodcastTapped(it)
                                navigateToPodcast(it)
                            },
                        )
                    }
                }
                composable(BlogsRoutes.ADD_BLOG) {
                    val viewModel = hiltViewModel<AddBlogViewModel>()
                    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                    val url by viewModel.url.collectAsStateWithLifecycle()
                    val onBlogAdded = { uuid: String ->
                        navController.popBackStack(BlogsRoutes.PODCASTS, inclusive = false)
                        navigateToPodcast(uuid)
                    }
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
                            viewModel.onFindFeedsTapped(url = it, onNavigateToPodcast = onBlogAdded)
                        },
                        onRetry = {
                            viewModel.onRetryTapped(url = it, onNavigateToPodcast = onBlogAdded)
                        },
                        onFeedClick = { webFeed ->
                            viewModel.onFeedSelected(webFeed = webFeed, onNavigateToPodcast = onBlogAdded)
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
