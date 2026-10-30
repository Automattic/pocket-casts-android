package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.core.view.updatePadding
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralPodcastsSelected
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoAddUpNextLimitBehaviour
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AutoAddSettingsState
import au.com.shiftyjelly.pocketcasts.settings.viewmodel.AutoAddSettingsViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.views.dialog.OptionsDialog
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.PodcastSelectFragmentSource
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class AutoAddSettingsFragment :
    BaseFragment(),
    PodcastSelectFragment.Listener {
    @Inject lateinit var settings: Settings

    val viewModel by activityViewModels<AutoAddSettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel.onShown()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val content = contentWithoutConsumedInsets {
            val state by viewModel.autoAddPodcasts.observeAsState()
            val bottomInsetPx by settings.bottomInset.collectAsStateWithLifecycle(0)
            AppTheme(theme.activeTheme) {
                val currentState = state ?: return@AppTheme
                AutoAddSettingsPage(
                    options = options(currentState),
                    footer = when (currentState.behaviour) {
                        AutoAddUpNextLimitBehaviour.ONLY_ADD_TO_TOP -> stringResource(LR.string.settings_auto_up_next_limit_reached_top_summary, currentState.limit)
                        AutoAddUpNextLimitBehaviour.STOP_ADDING -> stringResource(LR.string.settings_auto_up_next_limit_reached_stop_summary, currentState.limit)
                    },
                    podcasts = currentState.autoAddPodcasts,
                    bottomInset = with(LocalDensity.current) { bottomInsetPx.toDp() },
                    onPodcastClick = ::showPodcastOptions,
                    onNavigationClick = { requireActivity().onBackPressedDispatcher.onBackPressed() },
                    toolbarModifier = Modifier.pointerInput(Unit) {
                        detectTapGestures(onLongPress = { theme.toggleDarkLightThemeActivity(requireActivity() as AppCompatActivity) })
                    },
                )
            }
        }
        return FrameLayout(requireContext()).apply {
            id = R.id.fragment_container
            addView(content)
        }
    }

    @Composable
    private fun options(state: AutoAddSettingsState) = listOf(
        AutoAddOption(
            iconResId = IR.drawable.ic_upnext_playlast,
            title = stringResource(LR.string.settings_auto_up_next_limit),
            subtitle = stringResource(LR.string.episodes_plural, settings.autoAddUpNextLimit.value),
            onClick = ::showLimitOptions,
        ),
        AutoAddOption(
            iconResId = null,
            title = stringResource(LR.string.settings_auto_up_next_limit_reached),
            subtitle = when (state.behaviour) {
                AutoAddUpNextLimitBehaviour.ONLY_ADD_TO_TOP -> stringResource(LR.string.settings_auto_up_next_limit_reached_top)
                AutoAddUpNextLimitBehaviour.STOP_ADDING -> stringResource(LR.string.settings_auto_up_next_limit_reached_stop)
            },
            onClick = { showLimitReachedOptions(state.behaviour) },
        ),
        AutoAddOption(
            iconResId = null,
            title = stringResource(LR.string.settings_choose_podcasts),
            subtitle = LocalResources.current.getStringPluralPodcastsSelected(state.autoAddPodcasts.size),
            onClick = ::openPodcastsList,
        ),
    )

    private fun showPodcastOptions(podcast: Podcast) {
        OptionsDialog()
            .setTitle(getString(LR.string.settings_auto_up_next_add_to))
            .addCheckedOption(
                titleString = getString(LR.string.settings_auto_up_next_top),
                checked = podcast.autoAddToUpNext == Podcast.AutoAddUpNext.PLAY_NEXT,
                click = { viewModel.updatePodcast(podcast, Podcast.AutoAddUpNext.PLAY_NEXT) },
            )
            .addCheckedOption(
                titleString = getString(LR.string.settings_auto_up_next_bottom),
                checked = podcast.autoAddToUpNext == Podcast.AutoAddUpNext.PLAY_LAST,
                click = { viewModel.updatePodcast(podcast, Podcast.AutoAddUpNext.PLAY_LAST) },
            )
            .show(childFragmentManager, "autoadd_options")
    }

    private fun showLimitOptions() {
        val currentLimit = settings.autoAddUpNextLimit.value
        val dialog = OptionsDialog().setTitle(getString(LR.string.settings_auto_up_next_limit))
        listOf(10, 20, 50, 100, 200, 500, 1000).forEach { limit ->
            dialog.addCheckedOption(titleString = getString(LR.string.episodes_plural, limit), checked = currentLimit == limit) { viewModel.autoAddUpNextLimitChanged(limit) }
        }
        dialog.show(childFragmentManager, "autoadd_options")
    }

    private fun showLimitReachedOptions(behaviour: AutoAddUpNextLimitBehaviour) {
        OptionsDialog()
            .setTitle(getString(LR.string.settings_auto_up_next_add_to))
            .addCheckedOption(titleId = LR.string.settings_auto_up_next_limit_reached_stop, checked = behaviour == AutoAddUpNextLimitBehaviour.STOP_ADDING) {
                viewModel.autoAddUpNextLimitBehaviorChanged(AutoAddUpNextLimitBehaviour.STOP_ADDING)
            }
            .addCheckedOption(titleId = LR.string.settings_auto_up_next_limit_reached_top, checked = behaviour == AutoAddUpNextLimitBehaviour.ONLY_ADD_TO_TOP) {
                viewModel.autoAddUpNextLimitBehaviorChanged(AutoAddUpNextLimitBehaviour.ONLY_ADD_TO_TOP)
            }
            .show(childFragmentManager, "autoadd_options")
    }

    override fun onPause() {
        super.onPause()
        viewModel.onFragmentPause(activity?.isChangingConfigurations)
    }

    private fun openPodcastsList() {
        val fragment = PodcastSelectFragment.newInstance(
            tintColor = ThemeColor.primaryInteractive01(theme.activeTheme),
            showToolbar = true,
            source = PodcastSelectFragmentSource.AUTO_ADD,
        )
        fragment.listener = this
        childFragmentManager.beginTransaction()
            .replace(
                R.id.fragment_container,
                fragment,
            )
            .addToBackStack("podcast_selector")
            .commit()
    }

    override fun onBackPressed(): Boolean {
        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()

            view?.updatePadding(top = 0, bottom = 0)

            return true
        }

        return super.onBackPressed()
    }

    override fun podcastSelectFragmentSelectionChanged(newSelection: List<String>) {
        viewModel.selectionUpdated(newSelection)
    }

    override fun podcastSelectFragmentGetCurrentSelection(): List<String> {
        return viewModel.autoAddPodcasts.value?.autoAddPodcasts?.map { it.uuid } ?: emptyList()
    }
}
