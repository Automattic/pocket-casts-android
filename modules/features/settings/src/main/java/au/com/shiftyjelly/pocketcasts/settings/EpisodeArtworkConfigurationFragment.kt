package au.com.shiftyjelly.pocketcasts.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRowToggle
import au.com.shiftyjelly.pocketcasts.compose.components.SettingSection
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class EpisodeArtworkConfigurationFragment : BaseFragment() {
    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var analyticsTracker: AnalyticsTracker

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val sortedElements = remember { ArtworkConfiguration.Element.entries.sortedBy { getString(it.titleId) } }
        val bottomInset by settings.bottomInset.collectAsStateWithLifecycle(initialValue = 0)

        AppThemeWithBackground(theme.activeTheme) {
            val artworkConfiguration by settings.artworkConfiguration.flow.collectAsState()

            CallOnce {
                analyticsTracker.track(AnalyticsEvent.SETTINGS_ADVANCED_EPISODE_ARTWORK_SHOWN)
            }

            EpisodeArtworkSettings(
                artworkConfiguration = artworkConfiguration,
                elements = sortedElements,
                bottomInset = LocalDensity.current.run { bottomInset.toDp() },
                onUpdateConfiguration = { configuration ->
                    if (artworkConfiguration.useEpisodeArtwork != configuration.useEpisodeArtwork) {
                        analyticsTracker.track(AnalyticsEvent.SETTINGS_ADVANCED_EPISODE_ARTWORK_USE_EPISODE_ARTWORK_TOGGLED, mapOf("enabled" to configuration.useEpisodeArtwork))
                    }
                    settings.artworkConfiguration.set(configuration, updateModifiedAt = true)
                },
                onBackPress = {
                    @Suppress("DEPRECATION")
                    activity?.onBackPressed()
                },
            )
        }
    }

    @Composable
    private fun EpisodeArtworkSettings(
        artworkConfiguration: ArtworkConfiguration,
        elements: List<ArtworkConfiguration.Element>,
        bottomInset: Dp,
        onUpdateConfiguration: (ArtworkConfiguration) -> Unit,
        onBackPress: () -> Unit,
    ) {
        Column(
            modifier = Modifier.background(MaterialTheme.theme.colors.primaryUi02),
        ) {
            ThemedTopAppBar(
                title = stringResource(LR.string.settings_use_episode_artwork_title),
                onNavigationClick = onBackPress,
                bottomShadow = true,
            )
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
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
                Spacer(
                    modifier = Modifier.height(bottomInset),
                )
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
                if (configuration.useEpisodeArtwork) {
                    analyticsTracker.track(AnalyticsEvent.SETTINGS_ADVANCED_EPISODE_ARTWORK_CUSTOMIZATION_ELEMENT_TOGGLED, mapOf("enabled" to newValue, "element" to element.analyticsValue))
                }
                onUpdateConfiguration(if (newValue) configuration.enable(element) else configuration.disable(element))
            },
        )
    }

    private val ArtworkConfiguration.Element.titleId
        get() = when (this) {
            ArtworkConfiguration.Element.Filters -> LR.string.filters
            ArtworkConfiguration.Element.UpNext -> LR.string.up_next
            ArtworkConfiguration.Element.Downloads -> LR.string.profile_navigation_downloads
            ArtworkConfiguration.Element.Files -> LR.string.profile_navigation_files
            ArtworkConfiguration.Element.Starred -> LR.string.profile_navigation_starred
            ArtworkConfiguration.Element.Bookmarks -> LR.string.bookmarks
            ArtworkConfiguration.Element.ListeningHistory -> LR.string.profile_navigation_listening_history
            ArtworkConfiguration.Element.Podcasts -> LR.string.podcasts
        }
}
