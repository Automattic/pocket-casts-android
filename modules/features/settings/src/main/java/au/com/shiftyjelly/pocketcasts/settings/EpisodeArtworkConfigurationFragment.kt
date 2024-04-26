package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class EpisodeArtworkConfigurationFragment : BaseFragment() {
    @Inject lateinit var settings: Settings

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        val sortedElements = ArtworkConfiguration.Element.entries.sortedBy { getString(it.titleId) }
        setContent {
            AppThemeWithBackground(theme.activeTheme) {
                val artworkConfiguration by settings.artworkConfiguration.flow.collectAsState()

                EpisodeArtworkSettings(
                    artworkConfiguration = artworkConfiguration,
                    elements = sortedElements,
                    onUpdateConfiguration = { configuration ->
                        settings.artworkConfiguration.set(configuration, updateModifiedAt = true)
                    },
                    onBackPressed = {
                        @Suppress("DEPRECATION")
                        activity?.onBackPressed()
                    },
                )
            }
        }
    }

    @Composable
    private fun EpisodeArtworkSettings(
        artworkConfiguration: ArtworkConfiguration,
        elements: List<ArtworkConfiguration.Element>,
        onUpdateConfiguration: (ArtworkConfiguration) -> Unit,
        onBackPressed: () -> Unit,
    ) {
        Column(
            modifier = Modifier
                .background(MaterialTheme.theme.colors.primaryUi02)
                .verticalScroll(rememberScrollState()),
        ) {
            ThemedTopAppBar(
                title = stringResource(LR.string.settings_use_episode_artwork_title),
                onNavigationClick = onBackPressed,
                bottomShadow = true,
            )
            SettingSection {
                UseEpisodeArtwork(artworkConfiguration, onUpdateConfiguration)
            }
            SettingSection(
                heading = stringResource(LR.string.settings_use_episode_artwork_customization_section),
                subHeading = stringResource(LR.string.settings_use_episode_artwork_customization_description),
                showDivider = false,
            ) {
                elements.forEach { element ->
                    ArtworkElement(artworkConfiguration, element, onUpdateConfiguration)
                }
            }
        }
    }

    @Composable
    private fun UseEpisodeArtwork(
        configuration: ArtworkConfiguration,
        onUpdateConfiguration: (ArtworkConfiguration) -> Unit,
    ) {
        SettingRow(
            primaryText = stringResource(LR.string.settings_use_episode_artwork),
            secondaryText = stringResource(LR.string.settings_use_episode_artwork_details),
            toggle = SettingRowToggle.Switch(checked = configuration.useEpisodeArtwork),
            modifier = Modifier.toggleable(value = configuration.useEpisodeArtwork, role = Role.Switch) { newValue ->
                onUpdateConfiguration(configuration.copy(useEpisodeArtwork = newValue))
            },
        )
    }

    @Composable
    private fun ArtworkElement(
        configuration: ArtworkConfiguration,
        element: ArtworkConfiguration.Element,
        onUpdateConfiguration: (ArtworkConfiguration) -> Unit,
    ) {
        SettingRow(
            primaryText = stringResource(element.titleId),
            toggle = SettingRowToggle.Checkbox(checked = configuration.useEpisodeArtwork(element), enabled = configuration.useEpisodeArtwork),
            modifier = Modifier.toggleable(value = configuration.useEpisodeArtwork(element), role = Role.Checkbox) { newValue ->
                onUpdateConfiguration(if (newValue) configuration.enable(element) else configuration.disable(element))
            },
        )
    }

    private val ArtworkConfiguration.Element.titleId get() = when (this) {
        ArtworkConfiguration.Element.Filters -> LR.string.filters
        ArtworkConfiguration.Element.UpNext -> LR.string.up_next
        ArtworkConfiguration.Element.Downloads -> LR.string.profile_navigation_downloads
        ArtworkConfiguration.Element.Files -> LR.string.profile_navigation_files
        ArtworkConfiguration.Element.Starred -> LR.string.profile_navigation_starred
        ArtworkConfiguration.Element.Bookmarks -> LR.string.bookmarks
        ArtworkConfiguration.Element.ListeningHistory -> LR.string.profile_navigation_listening_history
    }
}
