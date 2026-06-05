package au.com.shiftyjelly.pocketcasts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

private data class SampleRow(
    val title: String,
    val itemCount: Int,
)

@Composable
fun TvTabPlaceholder(
    tab: TvTab,
    modifier: Modifier = Modifier,
) {
    val rows = remember {
        listOf(
            SampleRow("Made for TV", (4..10).random()),
            SampleRow("Recommendations", (4..10).random()),
            SampleRow("Because you liked Podcast", (4..10).random()),
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }
        items(rows) { row ->
            TvRow(
                title = row.title,
                items = (1..row.itemCount).toList(),
            ) { index ->
                TvTile(onClick = {}) {
                    Box(
                        modifier = Modifier
                            .size(180.dp, 120.dp)
                            .padding(12.dp),
                        contentAlignment = Alignment.BottomStart,
                    ) {
                        Text(
                            text = "Item $index",
                            color = Color.White,
                        )
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvTabPlaceholderPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvTabPlaceholder(tab = TvTab.Home)
            }
        }
    }
}
