package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bookmark.BookmarkRow
import au.com.shiftyjelly.pocketcasts.compose.buttons.TimePlayButtonColors
import au.com.shiftyjelly.pocketcasts.compose.components.EmptyState
import au.com.shiftyjelly.pocketcasts.compose.components.SearchBar
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.components.HeaderRow
import au.com.shiftyjelly.pocketcasts.player.view.bookmark.components.NoMatchingBookmarks
import au.com.shiftyjelly.pocketcasts.player.viewmodel.BookmarksViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.BookmarksViewModel.BookmarkMessage
import au.com.shiftyjelly.pocketcasts.player.viewmodel.BookmarksViewModel.UiState
import au.com.shiftyjelly.pocketcasts.settings.HeadphoneControlsSettingsFragment
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper
import au.com.shiftyjelly.pocketcasts.views.multiselect.MultiSelectBookmarksHelper.NavigationState
import java.util.Date
import java.util.UUID
import kotlinx.coroutines.flow.collectLatest
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun BookmarksPage(
    episodeUuid: String?,
    backgroundColor: Color,
    textColor: Color,
    sourceView: SourceView,
    bookmarksViewModel: BookmarksViewModel,
    multiSelectHelper: MultiSelectBookmarksHelper,
    onRowLongPressed: (Bookmark) -> Unit,
    onShareBookmarkClick: () -> Unit,
    onEditBookmarkClick: () -> Unit,
    onUpgradeClicked: () -> Unit,
    showOptionsDialog: (Int) -> Unit,
    openFragment: (Fragment) -> Unit,
    onSearchBarClearButtonTapped: () -> Unit,
    onHeadphoneControlsButtonTapped: () -> Unit,
    bottomInset: Dp,
    isDarkTheme: Boolean,
) {
    val context = LocalContext.current
    val state by bookmarksViewModel.uiState.collectAsStateWithLifecycle()

    Content(
        state = state,
        sourceView = sourceView,
        backgroundColor = backgroundColor,
        textColor = textColor,
        onRowLongPressed = onRowLongPressed,
        onBookmarksOptionsMenuClicked = { bookmarksViewModel.onOptionsMenuClicked() },
        onPlayClick = { bookmark ->
            bookmarksViewModel.play(bookmark)
        },
        onSearchTextChanged = { bookmarksViewModel.onSearchTextChanged(it) },
        onUpgradeClicked = onUpgradeClicked,
        openFragment = openFragment,
        bottomInset = bottomInset,
        onSearchBarClearButtonTapped = onSearchBarClearButtonTapped,
        onHeadphoneControlsButtonTapped = onHeadphoneControlsButtonTapped,
        isDarkTheme = isDarkTheme,
    )
    LaunchedEffect(episodeUuid) {
        bookmarksViewModel.loadBookmarks(
            episodeUuid = episodeUuid,
            sourceView = sourceView,
        )
        bookmarksViewModel.showOptionsDialog
            .collect { selectedValue ->
                showOptionsDialog(selectedValue)
            }
    }

    LaunchedEffect(context) {
        multiSelectHelper.navigationState
            .collect { navigationState ->
                when (navigationState) {
                    NavigationState.ShareBookmark -> onShareBookmarkClick()
                    NavigationState.EditBookmark -> onEditBookmarkClick()
                }
            }
    }
    LaunchedEffect(context) {
        bookmarksViewModel
            .message
            .collectLatest { message ->
                val string = when (message) {
                    is BookmarkMessage.BookmarkEpisodeNotFound -> context.getString(LR.string.episode_not_found)
                    is BookmarkMessage.PlayingBookmark -> context.getString(LR.string.playing_bookmark, message.bookmarkTitle)
                }
                Toast.makeText(context, string, Toast.LENGTH_SHORT).show()
            }
    }
}

@Composable
private fun Content(
    state: UiState,
    sourceView: SourceView,
    backgroundColor: Color,
    textColor: Color,
    onRowLongPressed: (Bookmark) -> Unit,
    onPlayClick: (Bookmark) -> Unit,
    onBookmarksOptionsMenuClicked: () -> Unit,
    onSearchTextChanged: (String) -> Unit,
    onUpgradeClicked: () -> Unit,
    openFragment: (Fragment) -> Unit,
    onSearchBarClearButtonTapped: () -> Unit,
    onHeadphoneControlsButtonTapped: () -> Unit,
    bottomInset: Dp,
    isDarkTheme: Boolean,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(color = backgroundColor)
            .padding(bottom = if (sourceView == SourceView.PROFILE) 0.dp else 28.dp),
    ) {
        when (state) {
            is UiState.Loading -> LoadingView()
            is UiState.Loaded -> BookmarksView(
                state = state,
                textColor = textColor,
                onRowLongPressed = onRowLongPressed,
                onOptionsMenuClicked = onBookmarksOptionsMenuClicked,
                onPlayClick = onPlayClick,
                onSearchTextChanged = onSearchTextChanged,
                bottomInset = bottomInset,
                onSearchBarClearButtonTapped = onSearchBarClearButtonTapped,
                isDarkTheme = isDarkTheme,
            )

            is UiState.Empty -> Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                EmptyState(
                    title = stringResource(LR.string.bookmarks_empty_state_title),
                    subtitle = stringResource(LR.string.bookmarks_paid_user_empty_state_message),
                    iconResourceId = IR.drawable.ic_bookmark,
                    primaryButtonText = stringResource(LR.string.bookmarks_headphone_settings),
                    onPrimaryButtonClick = {
                        onHeadphoneControlsButtonTapped()
                        openFragment(HeadphoneControlsSettingsFragment())
                    },
                )
            }
            is UiState.Upsell -> Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                EmptyState(
                    title = stringResource(LR.string.bookmarks_empty_state_title),
                    subtitle = stringResource(LR.string.bookmarks_free_user_empty_state_message),
                    iconResourceId = IR.drawable.ic_bookmark,
                    primaryButtonText = stringResource(LR.string.bookmarks_free_user_empty_state_button),
                    onPrimaryButtonClick = onUpgradeClicked,
                )
            }
        }
    }
}

@Composable
private fun BookmarksView(
    state: UiState.Loaded,
    textColor: Color,
    onRowLongPressed: (Bookmark) -> Unit,
    onOptionsMenuClicked: () -> Unit,
    onPlayClick: (Bookmark) -> Unit,
    onSearchTextChanged: (String) -> Unit,
    onSearchBarClearButtonTapped: () -> Unit,
    bottomInset: Dp,
    isDarkTheme: Boolean,
) {
    val focusRequester = remember { FocusRequester() }
    LazyColumn(
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = bottomInset),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.searchEnabled) {
            item {
                SearchBar(
                    text = state.searchText,
                    placeholder = stringResource(LR.string.search),
                    onTextChanged = onSearchTextChanged,
                    onClearButtonTapped = onSearchBarClearButtonTapped,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp)
                        .height(32.dp)
                        .focusRequester(focusRequester),
                )
            }
        }
        if (state.searchEnabled &&
            state.searchText.isNotEmpty() &&
            state.bookmarks.isEmpty()
        ) {
            item {
                NoMatchingBookmarks(
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
        } else {
            item {
                val title = stringResource(
                    id = if (state.bookmarks.size > 1) {
                        LR.string.bookmarks_plural
                    } else {
                        LR.string.bookmarks_singular
                    },
                    state.bookmarks.size,
                )

                HeaderRow(
                    title = title,
                    onOptionsMenuClicked = onOptionsMenuClicked,
                    style = state.headerRowColors,
                )
            }
        }
        items(state.bookmarks, key = { it }) { bookmark ->
            val episode = state.bookmarkIdAndEpisodeMap[bookmark.uuid]
            BookmarkRow(
                bookmark = bookmark.copy(episodeTitle = episode?.title ?: ""),
                episode = episode,
                isMultiSelecting = { state.isMultiSelecting },
                isSelected = state.isSelected,
                onPlayClick = onPlayClick,
                modifier = Modifier
                    .pointerInput(bookmark.adapterId) {
                        detectTapGestures(
                            onLongPress = { onRowLongPressed(bookmark) },
                            onTap = { state.onRowClick(bookmark) },
                        )
                    },
                colors = state.bookmarkRowColors,
                timePlayButtonStyle = state.timePlayButtonStyle,
                timePlayButtonColors = when (state.sourceView) {
                    SourceView.PLAYER -> TimePlayButtonColors.Player(textColor = textColor)
                    else -> TimePlayButtonColors.Default
                },
                showIcon = state.showIcon,
                useEpisodeArtwork = state.useEpisodeArtwork,
                showEpisodeTitle = state.showEpisodeTitle,
                isDarkTheme = isDarkTheme,
            )
        }
    }
}

@Preview
@Composable
private fun BookmarksPreview(
    theme: Theme.ThemeType = Theme.ThemeType.DARK,
) {
    AppTheme(theme) {
        Content(
            state = UiState.Loaded(
                bookmarks = listOf(
                    Bookmark(
                        uuid = UUID.randomUUID().toString(),
                        episodeUuid = UUID.randomUUID().toString(),
                        podcastUuid = UUID.randomUUID().toString(),
                        timeSecs = 10,
                        createdAt = Date(),
                        syncStatus = SyncStatus.SYNCED,
                        title = "Funny bit",
                    ),
                ),
                bookmarkIdAndEpisodeMap = mapOf(
                    UUID.randomUUID().toString() to
                        PodcastEpisode(
                            uuid = "",
                            publishedDate = Date(),
                        ),
                ),
                isMultiSelecting = false,
                useEpisodeArtwork = false,
                isSelected = { false },
                onRowClick = {},
                sourceView = SourceView.PLAYER,
            ),
            sourceView = SourceView.PLAYER,
            backgroundColor = Color.Black,
            textColor = Color.Black,
            onPlayClick = {},
            onRowLongPressed = {},
            onBookmarksOptionsMenuClicked = {},
            onSearchTextChanged = {},
            onUpgradeClicked = {},
            openFragment = {},
            onSearchBarClearButtonTapped = {},
            onHeadphoneControlsButtonTapped = {},
            bottomInset = 0.dp,
            isDarkTheme = false,
        )
    }
}
