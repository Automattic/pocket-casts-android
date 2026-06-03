package au.com.shiftyjelly.pocketcasts

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

private const val TITLE_UNFOCUSED_SIZE = 17f
private const val TITLE_FOCUSED_SIZE = 21f

@Composable
fun <T> TvRow(
    title: String,
    items: List<T>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 32.dp),
    itemSpacing: Dp = 16.dp,
    key: ((T) -> Any)? = null,
    content: @Composable (T) -> Unit,
) {
    var hasFocus by remember { mutableStateOf(false) }
    val titleSize by animateFloatAsState(
        targetValue = if (hasFocus) TITLE_FOCUSED_SIZE else TITLE_UNFOCUSED_SIZE,
        label = "TvRowTitleSize",
    )

    Column(
        modifier = modifier.onFocusChanged { focusState ->
            hasFocus = focusState.hasFocus
        },
    ) {
        Text(
            text = title,
            color = Color.White,
            style = TextStyle(
                fontSize = titleSize.sp,
                fontWeight = FontWeight(510),
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
            modifier = Modifier
                .padding(contentPadding)
                .padding(bottom = 17.dp),
        )

        val firstItemFocusRequester = remember { FocusRequester() }

        LazyRow(
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            modifier = Modifier
                .focusGroup()
                .focusRestorer(firstItemFocusRequester),
        ) {
            itemsIndexed(
                items = items,
                key = key?.let { k -> { _, item: T -> k(item) } },
            ) { index, item ->
                Box(
                    modifier = if (index == 0) Modifier.focusRequester(firstItemFocusRequester) else Modifier,
                ) {
                    content(item)
                }
            }
        }
    }
}

@Preview(device = Devices.TV_1080p, showBackground = true)
@Composable
private fun TvRowPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvRow(
                    title = "Recently Played",
                    items = (1..8).toList(),
                ) { index ->
                    TvTile(onClick = {}) {
                        Box(
                            modifier = Modifier
                                .size(160.dp, 100.dp)
                                .padding(12.dp),
                            contentAlignment = Alignment.BottomStart,
                        ) {
                            Text(
                                text = "Tile $index",
                                color = Color.White,
                            )
                        }
                    }
                }
            }
        }
    }
}
