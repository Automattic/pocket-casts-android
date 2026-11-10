package au.com.shiftyjelly.pocketcasts.views.fragments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Checkbox
import androidx.compose.material.CheckboxDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PodcastSelectPage(
    podcasts: List<Podcast>,
    selectedUuids: Set<String>,
    selectedText: String,
    tintColor: Color?,
    showToolbar: Boolean,
    bottomInset: Dp,
    onToggle: (Podcast, Boolean) -> Unit,
    onSelectButtonClick: () -> Unit,
    onNavigationClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.primaryUi01)
            .windowInsetsPadding(WindowInsets.statusBars),
    ) {
        if (showToolbar) {
            ThemedTopAppBar(
                title = stringResource(LR.string.settings_choose_podcasts),
                navigationButton = NavigationButton.Back,
                onNavigationClick = onNavigationClick,
                iconColor = colors.primaryInteractive01,
                textColor = colors.primaryText01,
                backgroundColor = colors.secondaryUi01,
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
            ) {
                TextP50(
                    text = selectedText,
                    color = colors.primaryText02,
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            TextButton(onClick = onSelectButtonClick) {
                val selectText = stringResource(if (selectedUuids.size == podcasts.size) LR.string.select_none else LR.string.select_all)
                TextH50(
                    text = selectText,
                    color = tintColor ?: colors.primaryInteractive01,
                    maxLines = 1,
                )
            }
        }
        LazyColumn(
            contentPadding = PaddingValues(bottom = bottomInset + 8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(podcasts, key = { it.uuid }) { podcast ->
                val isSelected = podcast.uuid in selectedUuids
                PodcastSelectRow(
                    podcast = podcast,
                    isSelected = isSelected,
                    tintColor = tintColor,
                    onToggle = { onToggle(podcast, it) },
                )
            }
        }
    }
}

@Composable
private fun PodcastSelectRow(
    podcast: Podcast,
    isSelected: Boolean,
    tintColor: Color?,
    onToggle: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.theme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.primaryUi01)
            .clickable { onToggle(!isSelected) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        PodcastImage(
            uuid = podcast.uuid,
            imageSize = 56.dp,
            contentDescription = null,
        )
        Column(
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f),
        ) {
            TextP50(text = podcast.title, maxLines = 1, modifier = Modifier.fillMaxWidth())
            TextP50(text = podcast.author, color = colors.primaryText02, maxLines = 1, modifier = Modifier.fillMaxWidth())
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.height(56.dp),
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onToggle,
                colors = if (tintColor != null) CheckboxDefaults.colors(checkedColor = tintColor) else CheckboxDefaults.colors(),
            )
        }
    }
}
