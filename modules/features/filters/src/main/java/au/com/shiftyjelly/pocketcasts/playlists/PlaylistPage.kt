package au.com.shiftyjelly.pocketcasts.playlists

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentData
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeader
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderButtonData
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderData
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistToolbar
import au.com.shiftyjelly.pocketcasts.playlists.component.ToolbarConfig
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

internal enum class ContentState {
    Uninitialized,
    HasNoEpisodes,
    HasEpisode,
}

internal class PlaylistPageListener(
    val onClickBack: () -> Unit,
    val onClickOptions: () -> Unit,
    val onShowArchivedToggle: () -> Unit,
    val onClickShowArchivedCta: () -> Unit,
    val onHideKeyboard: () -> Unit,
)

@Composable
internal fun PlaylistPage(
    headerData: PlaylistHeaderData?,
    episodes: List<PlaylistEpisode>,
    contentState: ContentState,
    noContentData: NoContentData,
    leftButton: PlaylistHeaderButtonData,
    rightButton: PlaylistHeaderButtonData,
    searchState: TextFieldState,
    bottomInsetPx: Int,
    listener: PlaylistPageListener,
    multiSelectTopBar: (@Composable () -> Unit)?,
    episodeRow: @Composable (PlaylistEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val listState = rememberLazyListState()
    val keyboardInset = WindowInsets.ime.getBottom(density)
    val toolbarAlpha = rememberToolbarAlpha(listState)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.theme.colors.primaryUi02),
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = with(density) { (bottomInsetPx + keyboardInset).toDp() }),
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(rememberHideKeyboardOnDrag(listState, listener.onHideKeyboard)),
        ) {
            item(key = "header") {
                PlaylistListHeader(headerData, leftButton, rightButton, searchState, listState, listener)
            }
            items(episodes, key = { it.uuid }) { episode ->
                episodeRow(episode)
            }
        }
        NoContentOverlay(contentState, noContentData)
        PlaylistToolbar(
            title = headerData?.title.orEmpty(),
            config = when (contentState) {
                ContentState.Uninitialized -> ToolbarConfig.WithoutTitle
                ContentState.HasNoEpisodes -> ToolbarConfig.WithTitle
                ContentState.HasEpisode -> ToolbarConfig.ForAlpha(if (keyboardInset != 0) 1f else toolbarAlpha)
            },
            onClickBack = listener.onClickBack,
            onClickOptions = listener.onClickOptions,
        )
        multiSelectTopBar?.invoke()
    }
}

@Composable
private fun PlaylistListHeader(
    headerData: PlaylistHeaderData?,
    leftButton: PlaylistHeaderButtonData,
    rightButton: PlaylistHeaderButtonData,
    searchState: TextFieldState,
    listState: LazyListState,
    listener: PlaylistPageListener,
) {
    val scope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf<Boolean?>(null) }
    var searchTopOffset by remember { mutableFloatStateOf(0f) }
    PlaylistHeader(
        data = headerData,
        leftButton = leftButton,
        rightButton = rightButton,
        searchState = searchState,
        useBlurredArtwork = Build.VERSION.SDK_INT >= 31,
        onShowArchivedToggle = listener.onShowArchivedToggle,
        onClickShowArchivedCta = listener.onClickShowArchivedCta,
        onMeasureSearchTopOffset = { topOffset -> searchTopOffset = topOffset },
        onChangeSearchFocus = { focusState ->
            val hasFocus = focusState.hasFocus
            if (isFocused != hasFocus) {
                isFocused = hasFocus
                if (hasFocus) {
                    scope.launch { listState.animateScrollToItem(0, searchTopOffset.roundToInt()) }
                }
                keyboard?.show()
            }
        },
        modifier = Modifier.background(MaterialTheme.theme.colors.primaryUi02),
    )
}

@Composable
private fun NoContentOverlay(
    contentState: ContentState,
    noContentData: NoContentData,
) {
    val transition = updateTransition(contentState)
    Box(
        modifier = when (transition.currentState) {
            ContentState.Uninitialized, ContentState.HasNoEpisodes -> Modifier.clickable(
                onClick = {},
                interactionSource = null,
                indication = null,
            )

            ContentState.HasEpisode -> Modifier
        },
    ) {
        if (transition.currentState == ContentState.Uninitialized) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.theme.colors.primaryUi02),
            )
        }
        transition.AnimatedVisibility(
            visible = { it == ContentState.HasNoEpisodes },
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.theme.colors.primaryUi02),
            ) {
                NoContentBanner(data = noContentData)
            }
        }
    }
}

@Composable
private fun rememberToolbarAlpha(listState: LazyListState): Float {
    val density = LocalDensity.current
    val transparencyThreshold = with(density) { 40.dp.toPx().toInt() }
    val maxProgressDistance = with(density) { 100.dp.toPx().toInt() }
    val toolbarAlpha by remember(transparencyThreshold, maxProgressDistance) {
        derivedStateOf {
            val headerOffset = if (listState.firstVisibleItemIndex == 0) {
                listState.firstVisibleItemScrollOffset.absoluteValue
            } else {
                Int.MAX_VALUE
            }
            if (headerOffset > transparencyThreshold) {
                val scrollFraction = (headerOffset - transparencyThreshold).toFloat() / maxProgressDistance
                lerp(0f, 1f, scrollFraction).coerceIn(0f, 1f)
            } else {
                0f
            }
        }
    }
    return toolbarAlpha
}

@Composable
private fun rememberHideKeyboardOnDrag(listState: LazyListState, onHideKeyboard: () -> Unit): NestedScrollConnection {
    val touchSlop = LocalViewConfiguration.current.touchSlop
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    val currentIsDragged by rememberUpdatedState(isDragged)
    return remember(touchSlop, onHideKeyboard) {
        object : NestedScrollConnection {
            private var totalDy = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && currentIsDragged) {
                    totalDy += consumed.y.absoluteValue
                    if (totalDy >= touchSlop) {
                        totalDy = 0f
                        onHideKeyboard()
                    }
                }
                return Offset.Zero
            }
        }
    }
}
