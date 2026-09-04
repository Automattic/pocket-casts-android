package au.com.shiftyjelly.pocketcasts.component

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.theme.GoogleSansFontFamily
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors

private const val TITLE_UNFOCUSED_SIZE = 19f
private const val TITLE_FOCUSED_SIZE = 23f

@Composable
fun TvSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TITLE_UNFOCUSED_SIZE.sp,
) {
    Text(
        text = title,
        color = MaterialTheme.tvColors.textPrimary,
        style = TextStyle(
            fontFamily = GoogleSansFontFamily,
            fontSize = fontSize,
            fontWeight = FontWeight(500),
            platformStyle = PlatformTextStyle(includeFontPadding = false),
        ),
        modifier = modifier,
    )
}

@Composable
fun <T> TvRow(
    title: String,
    items: List<T>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 42.dp),
    itemSpacing: Dp = 12.dp,
    key: ((T) -> Any)? = null,
    focusRequester: FocusRequester? = null,
    centerFocusedItem: Boolean = false,
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
        TvSectionTitle(
            title = title,
            fontSize = titleSize.sp,
            modifier = Modifier
                .padding(contentPadding)
                .padding(bottom = 16.dp),
        )

        var lastFocusedIndex by rememberSaveable(items) { mutableIntStateOf(0) }
        val focusRequesters = remember(items) { List(items.size) { FocusRequester() } }
        val listState = rememberLazyListState()

        if (centerFocusedItem) {
            LaunchedEffect(lastFocusedIndex) {
                val item = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == lastFocusedIndex }
                val viewport = listState.layoutInfo.viewportSize.width
                val centerOffset = item?.let { -(viewport - it.size) / 2 } ?: 0
                listState.animateScrollToItem(lastFocusedIndex, centerOffset)
            }
        }

        LazyRow(
            state = listState,
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(itemSpacing),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .focusGroup()
                .focusProperties {
                    onEnter = {
                        runCatching { focusRequesters.getOrNull(lastFocusedIndex)?.requestFocus() }
                    }
                },
        ) {
            itemsIndexed(
                items = items,
                key = key?.let { k -> { _, item: T -> k(item) } },
            ) { index, item ->
                Box(
                    modifier = Modifier
                        .focusRequester(focusRequesters[index])
                        .onFocusChanged { focusState ->
                            if (focusState.hasFocus) {
                                lastFocusedIndex = index
                            }
                        },
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
    TvTheme {
        Box(modifier = Modifier.background(MaterialTheme.tvColors.backgroundSunken)) {
            TvRow(
                title = "Recently Played",
                items = (1..8).toList(),
            ) { index ->
                TvTile(onClick = {}) {
                    Box(
                        modifier = Modifier
                            .size(120.dp, 75.dp)
                            .padding(9.dp),
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
