package au.com.shiftyjelly.pocketcasts.compose.podcast

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.GridCells
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.text.LinkText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PodcastSelectGrid(
    podcasts: List<Podcast>,
    selectedUuids: Set<String>,
    onPodcastSelected: (Podcast) -> Unit,
    onPodcastUnselected: (Podcast) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            cells = GridCells.Adaptive(minSize = 80.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.weight(1f)
        ) {
            items(items = podcasts) { podcast ->
                PodcastSelectImage(
                    podcast = podcast,
                    selected = selectedUuids.contains(podcast.uuid),
                    onPodcastSelected = { onPodcastSelected(podcast) },
                    onPodcastUnselected = { onPodcastUnselected(podcast) }
                )
            }
        }
        SelectGridFooter(
            podcasts = podcasts,
            selectedUuids = selectedUuids,
            onSelectAll = onSelectAll,
            onSelectNone = onSelectNone
        )
    }
}

@Composable
private fun SelectGridFooter(
    podcasts: List<Podcast>,
    selectedUuids: Set<String>,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.padding(top = 4.dp, start = 16.dp, bottom = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        val amountSelected = when (selectedUuids.size) {
            podcasts.size -> stringResource(LR.string.podcasts_share_all_selected)
            else -> stringResource(LR.string.podcasts_share_selected, selectedUuids.size)
        }
        TextH40(
            text = amountSelected,
            color = MaterialTheme.theme.colors.primaryText02,
        )
        Spacer(Modifier.weight(1f))
        LinkText(
            text = stringResource(if (podcasts.size == selectedUuids.size) LR.string.select_none else LR.string.select_all),
            onClick = {
                if (podcasts.size == selectedUuids.size) {
                    onSelectNone()
                } else {
                    onSelectAll()
                }
            }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PodcastSelectListPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        Column {
            PodcastSelectGrid(
                podcasts = listOf(Podcast(uuid = "e7a6f7d0-02f2-0133-1c51-059c869cc4eb"), Podcast(uuid = "3782b780-0bc5-012e-fb02-00163e1b201c")),
                selectedUuids = setOf("3782b780-0bc5-012e-fb02-00163e1b201c"),
                onPodcastSelected = {},
                onPodcastUnselected = {},
                onSelectAll = {},
                onSelectNone = {}
            )
        }
    }
}
