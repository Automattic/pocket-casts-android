package au.com.shiftyjelly.pocketcasts.transcripts.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.buttons.IconButtonSmall
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBarDefaults
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.transcripts.SearchState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.utils.search.SearchCoordinates
import au.com.shiftyjelly.pocketcasts.utils.search.SearchMatches
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBar as BaseSearchBar
import au.com.shiftyjelly.pocketcasts.localization.R as LR

data class ToolbarColors(
    val button: Color,
    val buttonBackground: Color,
    val searchBarBackground: Color,
    val searchBarText: Color,
    val searchBarTextPlaceholder: Color,
    val searchBarContent: Color,
    val searchBarCursor: Color,
    val searchBarHandle: Color,
) {
    val serachBarSelectionBackground = searchBarHandle.copy(alpha = searchBarHandle.alpha * 0.3f)

    companion object {
        fun default(colors: ThemeColors) = ToolbarColors(
            button = colors.primaryIcon01,
            buttonBackground = colors.primaryIcon01.copy(alpha = 0.15f),
            searchBarBackground = colors.primaryField01,
            searchBarText = colors.primaryText01,
            searchBarTextPlaceholder = colors.primaryText01.copy(alpha = 0.6f),
            searchBarContent = colors.primaryText01.copy(alpha = 0.4f),
            searchBarCursor = colors.primaryText02,
            searchBarHandle = colors.primaryText01,
        )

        fun player(colors: PlayerColors) = ToolbarColors(
            button = colors.contrast02,
            buttonBackground = colors.contrast06,
            searchBarBackground = Color.White,
            searchBarText = Color.Black,
            searchBarTextPlaceholder = Color.Black.copy(alpha = 0.5f),
            searchBarContent = Color.Black.copy(alpha = 0.5f),
            searchBarCursor = Color.Black.copy(alpha = 0.5f),
            searchBarHandle = colors.highlight01,
        )
    }
}

@Composable
internal fun Toolbar(
    searchState: SearchState,
    onClickClose: () -> Unit,
    onUpdateSearchTerm: (String) -> Unit,
    onClearSearchTerm: () -> Unit,
    onSelectPreviousSearch: () -> Unit,
    onSelectNextSearch: () -> Unit,
    onShowSearchBar: () -> Unit,
    onHideSearchBar: () -> Unit,
    modifier: Modifier = Modifier,
    colors: ToolbarColors = ToolbarColors.default(MaterialTheme.theme.colors),
    hideSearchBar: Boolean = false,
    trailingContent: (@Composable (ToolbarColors) -> Unit)? = null,
) {
    val focusRequester = remember { FocusRequester() }
    val showSearchTransition = updateTransition(searchState.isSearchOpen)

    CompositionLocalProvider(
        LocalRippleConfiguration provides RippleConfiguration(color = colors.button),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = modifier,
        ) {
            CloseTranscriptButton(
                colors = colors,
                onClick = onClickClose,
                modifier = Modifier.offset(x = -12.dp),
            )
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            AnimatedVisibility(
                visible = !hideSearchBar,
                enter = SearchEnterTransition,
                exit = SearchExitTransition,
            ) {
                Box {
                    showSearchTransition.AnimatedVisibility(
                        visible = { !it },
                        enter = SearchButtonEnterTransition,
                        exit = SearchButtonExitTransition,
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            trailingContent?.invoke(colors)
                            ShowSearchButton(
                                colors = colors,
                                onClick = onShowSearchBar,
                            )
                        }
                    }
                    showSearchTransition.AnimatedVisibility(
                        visible = { it },
                        enter = SearchBarEnterTransition,
                        exit = SearchBarExitTransition,
                        modifier = Modifier.widthIn(max = 420.dp),
                    ) {
                        SearchBar(
                            searchState = searchState,
                            colors = colors,
                            onUpdateSearchTerm = onUpdateSearchTerm,
                            onClickHide = onHideSearchBar,
                            onClickClear = onClearSearchTerm,
                            onSelectPreviousSearch = onSelectPreviousSearch,
                            onSelectNextSearch = onSelectNextSearch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(focusRequester, searchState.isSearchOpen) {
        if (searchState.isSearchOpen) {
            focusRequester.requestFocus()
        }
    }
}

@Composable
private fun CloseTranscriptButton(
    colors: ToolbarColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(LR.string.transcript_close),
            tint = colors.button,
        )
    }
}

@Composable
private fun ShowSearchButton(
    colors: ToolbarColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier.background(colors.buttonBackground, CircleShape),
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = stringResource(LR.string.transcript_search),
            tint = colors.button,
        )
    }
}

@Composable
private fun HideSearchButton(
    colors: ToolbarColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButtonSmall(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(LR.string.transcript_search_close),
            tint = colors.searchBarContent,
        )
    }
}

@Composable
private fun SearchBar(
    searchState: SearchState,
    colors: ToolbarColors,
    onUpdateSearchTerm: (String) -> Unit,
    onClickHide: () -> Unit,
    onClickClear: () -> Unit,
    onSelectPreviousSearch: () -> Unit,
    onSelectNextSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(
        LocalTextSelectionColors provides TextSelectionColors(
            handleColor = colors.searchBarHandle,
            backgroundColor = colors.serachBarSelectionBackground,
        ),
        LocalRippleConfiguration provides RippleConfiguration(
            color = colors.searchBarContent,
        ),
    ) {
        BaseSearchBar(
            text = searchState.searchTerm,
            leadingContent = {
                HideSearchButton(
                    colors = colors,
                    onClick = onClickHide,
                )
            },
            trailingContent = {
                SearchBarControls(
                    searchState = searchState,
                    colors = colors,
                    onClearSearch = onClickClear,
                    onSelectPreviousSearch = onSelectPreviousSearch,
                    onSelectNextSearch = onSelectNextSearch,
                    modifier = Modifier.padding(end = 8.dp),
                )
            },
            placeholder = stringResource(LR.string.search),
            onTextChange = onUpdateSearchTerm,
            cornerRadius = 16.dp,
            colors = SearchBarDefaults.colors(
                backgroundColor = colors.searchBarBackground,
                textColor = colors.searchBarText,
                placeholderColor = colors.searchBarTextPlaceholder,
                cursorColor = colors.searchBarCursor,
            ),
            contentPadding = PaddingValues(end = 16.dp),
            modifier = modifier,
        )
    }
}

@Composable
private fun SearchBarControls(
    searchState: SearchState,
    colors: ToolbarColors,
    onClearSearch: () -> Unit,
    onSelectPreviousSearch: () -> Unit,
    onSelectNextSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        AnimatedVisibility(
            visible = searchState.searchTerm.isNotEmpty(),
            enter = SearchControlsEnterTransition,
            exit = SearchControlsExitTransition,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val matchesCount = searchState.matches.count
                Text(
                    text = if (matchesCount != 0) {
                        "${searchState.matches.selectedMatchIndex + 1} / ${searchState.matches.count}"
                    } else {
                        "0"
                    },
                    color = colors.searchBarContent,
                    fontSize = 14.nonScaledSp,
                    lineHeight = 14.nonScaledSp,
                )
                ClearSearchButton(
                    colors = colors,
                    onClick = onClearSearch,
                )
            }
        }
        PreviousSearchButton(
            colors = colors,
            onClick = onSelectPreviousSearch,
        )
        NextSearchButton(
            colors = colors,
            onClick = onSelectNextSearch,
        )
    }
}

@Composable
private fun ClearSearchButton(
    colors: ToolbarColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButtonSmall(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.Clear,
            contentDescription = stringResource(LR.string.transcript_search_close),
            tint = colors.searchBarContent,
        )
    }
}

@Composable
private fun PreviousSearchButton(
    colors: ToolbarColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButtonSmall(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowUp,
            contentDescription = stringResource(LR.string.go_to_previous),
            tint = colors.searchBarContent,
        )
    }
}

@Composable
private fun NextSearchButton(
    colors: ToolbarColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButtonSmall(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowDown,
            contentDescription = stringResource(LR.string.go_to_next),
            tint = colors.searchBarContent,
        )
    }
}

private val SearchButtonEnterTransition = fadeIn()
private val SearchButtonExitTransition = fadeOut()

private val SearchBarEnterTransition = fadeIn() + expandHorizontally()
private val SearchBarExitTransition = fadeOut() + shrinkHorizontally()

private val SearchControlsEnterTransition = fadeIn()
private val SearchControlsExitTransition = fadeOut()

private val SearchEnterTransition = fadeIn()
private val SearchExitTransition = fadeOut()

@Preview(widthDp = 600)
@Composable
private fun ToolbarPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp),
        ) {
            Toolbar(
                hideSearchBar = true,
                initialSearchState = SearchState.Empty,
                modifier = Modifier.fillMaxWidth(),
            )
            Toolbar(
                initialSearchState = SearchState.Empty.copy(isSearchOpen = false),
                modifier = Modifier.fillMaxWidth(),
            )
            Toolbar(
                initialSearchState = SearchState.Empty.copy(isSearchOpen = true),
                modifier = Modifier.fillMaxWidth(),
            )
            Toolbar(
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Preview(widthDp = 600)
@Composable
private fun ToolbarPlayerPreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppTheme(ThemeType.ROSE) {
        CompositionLocalProvider(LocalPodcastColors provides podcastColors) {
            val transcriptTheme = rememberTranscriptTheme()
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .background(transcriptTheme.background)
                    .padding(16.dp),
            ) {
                Toolbar(
                    hideSearchBar = true,
                    initialSearchState = SearchState.Empty,
                    colors = transcriptTheme.toolbarColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Toolbar(
                    initialSearchState = SearchState.Empty.copy(isSearchOpen = false),
                    colors = transcriptTheme.toolbarColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Toolbar(
                    initialSearchState = SearchState.Empty.copy(isSearchOpen = true),
                    colors = transcriptTheme.toolbarColors,
                    modifier = Modifier.fillMaxWidth(),
                )
                Toolbar(
                    colors = transcriptTheme.toolbarColors,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun Toolbar(
    modifier: Modifier = Modifier,
    hideSearchBar: Boolean = false,
    initialSearchState: SearchState = SearchState(
        isSearchOpen = true,
        searchTerm = "Lorem ipsum",
        matches = SearchMatches(
            selectedCoordinate = SearchCoordinates(0, 0),
            matchingCoordinates = mapOf(
                0 to listOf(0, 5, 20),
                1 to listOf(6),
                5 to listOf(4, 9),
            ),
        ),
    ),
    colors: ToolbarColors = ToolbarColors.default(MaterialTheme.theme.colors),
) {
    var searchState by remember { mutableStateOf(initialSearchState) }
    var isSearchBarOpen by remember { mutableStateOf(searchState.isSearchOpen) }

    Toolbar(
        searchState = searchState,
        hideSearchBar = hideSearchBar,
        colors = colors,
        onUpdateSearchTerm = { searchTerm ->
            searchState = searchState.copy(searchTerm = searchTerm)
        },
        onClearSearchTerm = {
            searchState = searchState.copy(searchTerm = "")
        },
        onSelectNextSearch = {
            searchState = searchState.copy(matches = searchState.matches.next())
        },
        onSelectPreviousSearch = {
            searchState = searchState.copy(matches = searchState.matches.previous())
        },
        onShowSearchBar = { isSearchBarOpen = true },
        onHideSearchBar = { isSearchBarOpen = false },
        onClickClose = {},
        modifier = modifier,
    )
}
