package au.com.shiftyjelly.pocketcasts.compose.podcast

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
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

@Composable
fun PodcastSelectGrid(
    podcasts: List<Podcast>,
    selectedPodcasts: Set<Podcast>,
    onPodcastSelected: (Podcast) -> Unit,
    onPodcastUnselected: (Podcast) -> Unit,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val configuration = LocalConfiguration.current
    val imageMinSize = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) 120.dp else 80.dp
    Column(modifier = modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = imageMinSize),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = modifier.weight(1f),
        ) {
            items(items = podcasts) { podcast ->
                PodcastSelectImage(
                    podcast = podcast,
                    selected = selectedPodcasts.contains(podcast),
                    onPodcastSelected = { onPodcastSelected(podcast) },
                    onPodcastUnselected = { onPodcastUnselected(podcast) },
                )
            }
        }
        SelectGridFooter(
            podcasts = podcasts,
            selectedPodcasts = selectedPodcasts,
            onSelectAll = onSelectAll,
            onSelectNone = onSelectNone,
        )
    }
}

@Composable
private fun SelectGridFooter(
    podcasts: List<Podcast>,
    selectedPodcasts: Set<Podcast>,
    onSelectAll: () -> Unit,
    onSelectNone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.padding(top = 4.dp, start = 16.dp, bottom = 4.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        val amountSelected = when (selectedPodcasts.size) {
            podcasts.size -> stringResource(LR.string.podcasts_share_all_selected)
            else -> stringResource(LR.string.podcasts_share_selected, selectedPodcasts.size)
        }
        TextH40(
            text = amountSelected,
            color = MaterialTheme.theme.colors.primaryText02,
        )
        Spacer(Modifier.weight(1f))
        LinkText(
            text = stringResource(if (podcasts.size == selectedPodcasts.size) LR.string.select_none else LR.string.select_all),
            onClick = {
                if (podcasts.size == selectedPodcasts.size) {
                    onSelectNone()
                } else {
                    onSelectAll()
                }
            },
            modifier = modifier.padding(8.dp),
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
                selectedPodcasts = setOf(Podcast(uuid = "3782b780-0bc5-012e-fb02-00163e1b201c")),
                onPodcastSelected = {},
                onPodcastUnselected = {},
                onSelectAll = {},
                onSelectNone = {},
            )
        }
    }
}
