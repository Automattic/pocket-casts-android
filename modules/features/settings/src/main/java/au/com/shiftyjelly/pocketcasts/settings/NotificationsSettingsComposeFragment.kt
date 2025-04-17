package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.NotificationsSettingsViewModel
import au.com.shiftyjelly.pocketcasts.ui.helper.FragmentHostListener
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragmentSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class NotificationsSettingsComposeFragment : BaseFragment(), PodcastSelectFragment.Listener {

    companion object {
        const val NAV_TO_NOTIFICATIONS_SETTINGS = "notifications_settings"
        const val NAV_TO_PODCAST_SELECT = "podcast_select"
    }

    @Inject
    lateinit var settings: Settings

    private val viewModel: NotificationsSettingsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        AppThemeWithBackground(theme.activeTheme) {
            val navController = rememberNavController()

            val bottomInset = settings.bottomInset.collectAsStateWithLifecycle(initialValue = 0)
            val state by viewModel.state.collectAsState()

            NavHost(
                navController = navController,
                startDestination = NAV_TO_NOTIFICATIONS_SETTINGS,
            ) {
                composable(NAV_TO_NOTIFICATIONS_SETTINGS) {
                    NotificationsSettingsPage(
                        state = state,
                        bottomInset = bottomInset.value.pxToDp(LocalContext.current).dp,
                        onBackPressed = {
                            (activity as? FragmentHostListener)?.closeModal(this@NotificationsSettingsComposeFragment)
                        },
                        onSelectPodcasts = {
                            navController.navigate(NAV_TO_PODCAST_SELECT)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                composable(NAV_TO_PODCAST_SELECT) {
                    FragmentContainer(
                        fragment = {
                            PodcastSelectFragment.newInstance(
                                source = PodcastSelectFragmentSource.NOTIFICATIONS,
                            )
                        },
                        onBackPressed = {
                            (activity as? FragmentHostListener)?.closeModal(this@NotificationsSettingsComposeFragment)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }

    override fun podcastSelectFragmentSelectionChanged(newSelection: List<String>) {
        viewModel.onPodcastsSelectionChanged(newSelection)
    }

    override fun podcastSelectFragmentGetCurrentSelection(): List<String> {
        return viewModel.getCurrentPodcastsSelection()
    }

    @Composable
    private fun FragmentContainer(
        fragment: () -> Fragment,
        onBackPressed: () -> Unit,
        modifier: Modifier = Modifier,
    ) {
        Column {
            ThemedTopAppBar(
                title = stringResource(LR.string.settings_select_podcasts),
                bottomShadow = true,
                onNavigationClick = { onBackPressed() },
            )

            AndroidView(
                factory = { ctx ->
                    FragmentContainerView(ctx).apply {
                        id = au.com.shiftyjelly.pocketcasts.settings.R.id.fragment_container_view
                    }
                },
                modifier = modifier,
                update = { view ->
                    val fragmentManager = childFragmentManager
                    if (fragmentManager.findFragmentById(view.id) == null) {
                        fragmentManager.beginTransaction()
                            .replace(view.id, fragment())
                            .setReorderingAllowed(true)
                            .commit()
                    }
                },
            )
        }
    }
}
