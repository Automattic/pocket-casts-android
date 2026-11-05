package au.com.shiftyjelly.pocketcasts.settings

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.compose.components.SettingsSection
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal data class AutoAddOption(
    @DrawableRes val iconResId: Int?,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

@Composable
internal fun AutoAddSettingsPage(
    options: List<AutoAddOption>,
    footer: String?,
    podcasts: List<Podcast>,
    bottomInset: Dp,
    onPodcastClick: (Podcast) -> Unit,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
    toolbarModifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primaryUi02),
    ) {
        ThemedTopAppBar(
            title = stringResource(LR.string.settings_auto_up_next_title),
            navigationButton = NavigationButton.Back,
            onNavigationClick = onNavigationClick,
            iconColor = colors.secondaryIcon01,
            textColor = colors.secondaryText01,
            backgroundColor = colors.secondaryUi01,
            modifier = toolbarModifier,
        )
        LazyColumn(
            contentPadding = PaddingValues(top = 16.dp, bottom = bottomInset),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(options) { option -> OptionRow(option) }
            if (footer != null) {
                item {
                    TextP50(
                        text = footer,
                        color = colors.primaryText02,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = SettingsSection.indentedStartPadding, top = 16.dp, end = 24.dp, bottom = 16.dp),
                    )
                }
                item {
                    HorizontalDivider()
                    TextH50(
                        text = stringResource(LR.string.settings_auto_up_next_podcasts),
                        color = colors.support01,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = SettingsSection.indentedStartPadding, top = 16.dp, end = 24.dp, bottom = 16.dp),
                    )
                }
            }
            items(podcasts, key = { it.uuid }) { podcast ->
                PodcastRow(podcast = podcast, onClick = { onPodcastClick(podcast) })
            }
        }
    }
}

@Composable
private fun OptionRow(option: AutoAddOption) {
    SettingRow(
        primaryText = option.title,
        secondaryText = option.subtitle,
        icon = option.iconResId?.let { painterResource(it) },
        iconTint = MaterialTheme.theme.colors.primaryIcon01,
        modifier = Modifier.clickable(onClick = option.onClick),
    )
}

@Composable
private fun PodcastRow(podcast: Podcast, onClick: () -> Unit) {
    val colors = MaterialTheme.theme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        PodcastImage(
            uuid = podcast.uuid,
            imageSize = 56.dp,
            contentDescription = null,
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            TextP40(text = podcast.title, maxLines = 1, modifier = Modifier.fillMaxWidth())
            val subtitle = when (podcast.autoAddToUpNext) {
                Podcast.AutoAddUpNext.PLAY_NEXT -> stringResource(LR.string.settings_auto_up_next_to_top)
                Podcast.AutoAddUpNext.PLAY_LAST -> stringResource(LR.string.settings_auto_up_next_to_bottom)
                Podcast.AutoAddUpNext.OFF -> ""
            }
            TextP50(text = subtitle, color = colors.primaryText02, maxLines = 1, modifier = Modifier.fillMaxWidth())
        }
    }
}
