package au.com.shiftyjelly.pocketcasts.views.fragments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Checkbox
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPluralPodcastsSelected
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PodcastSelectPage(
    showToolbar: Boolean,
    selectTextTintColor: Int? = null,
    selectedCount: Int,
    selectText: String,
    podcastItems: List<SelectablePodcast>,
    onSelectClick: () -> Unit,
    onBackPressed: () -> Unit,
    onPodcastToggled: (SelectablePodcast, Boolean) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.theme.colors.primaryUi01),
    ) {
        if (showToolbar) {
            ThemedTopAppBar(
                title = stringResource(LR.string.settings_choose_podcasts),
                bottomShadow = true,
                onNavigationClick = { onBackPressed() },
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val selectedText =
                LocalContext.current.resources.getStringPluralPodcastsSelected(selectedCount)

            Text(
                text = selectedText,
                color = MaterialTheme.theme.colors.primaryText02,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )

            TextButton(
                onClick = onSelectClick,
            ) {
                Text(
                    text = selectText,
                    color = selectTextTintColor?.let { Color(it) }
                        ?: LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            items(podcastItems.size) { index ->
                PodcastItemRow(
                    podcast = podcastItems[index],
                    onSelected = { enabled -> onPodcastToggled(podcastItems[index], enabled) },
                )
            }
        }
    }
}

@Composable
private fun PodcastItemRow(
    podcast: SelectablePodcast,
    onSelected: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onSelected(podcast.selected) }
            .padding(horizontal = 16.dp)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PodcastImage(
            uuid = podcast.podcast.uuid,
            modifier = modifier
                .size(56.dp),
        )

        Spacer(Modifier.width(8.dp))

        Column(
            modifier = Modifier
                .weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = podcast.podcast.title,
                color = MaterialTheme.theme.colors.primaryText01,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = podcast.podcast.author,
                color = MaterialTheme.theme.colors.primaryText02,
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.width(8.dp))

        Checkbox(
            checked = podcast.selected,
            onCheckedChange = { newState ->
                onSelected(newState)
            },
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}
