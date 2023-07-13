package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.progressSemantics
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
import au.com.shiftyjelly.pocketcasts.player.viewmodel.BookmarksViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.BookmarksViewModel.UiState
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar
import java.util.Date
import java.util.UUID
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@AndroidEntryPoint
class BookmarksFragment : BaseFragment() {
    private val playerViewModel: PlayerViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            AppTheme(theme.activeTheme) {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                // Hack to allow nested scrolling inside bottom sheet viewpager
                // https://stackoverflow.com/a/70195667/193545
                Surface(modifier = Modifier.nestedScroll(rememberNestedScrollInteropConnection())) {
                    BookmarksPage(
                        playerViewModel = playerViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun BookmarksPage(
    playerViewModel: PlayerViewModel,
    bookmarksViewModel: BookmarksViewModel = hiltViewModel(),
) {
    val state by bookmarksViewModel.uiState.collectAsStateWithLifecycle()
    val listData = playerViewModel.listDataLive.asFlow().collectAsState(initial = null)
    listData.value?.let {
        Content(
            state = state,
            backgroundColor = Color(it.podcastHeader.backgroundColor),
        )
        LaunchedEffect(Unit) {
            bookmarksViewModel.loadBookmarks(
                episodeUuid = it.podcastHeader.episodeUuid
            )
        }
    }
}

@Composable
private fun Content(
    state: UiState,
    backgroundColor: Color,
) {
    Box(
        modifier = Modifier
            .background(color = backgroundColor)
    ) {
        when (state) {
            is UiState.Loading -> LoadingView()
            is UiState.Loaded -> BookmarksView(
                state = state,
                backgroundColor = backgroundColor,
            )

            is UiState.Empty -> NoBookmarksView()
            is UiState.PlusUpsell -> PlusUpsellView()
        }
    }
}

@Composable
private fun BookmarksView(
    state: UiState.Loaded,
    backgroundColor: Color,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
    ) {
        item {
            val title = stringResource(
                id = if (state.bookmarks.size > 1) {
                    LR.string.bookmarks_plural
                } else {
                    LR.string.bookmarks_singular
                },
                state.bookmarks.size
            )
            HeaderItem(title)
        }
        items(state.bookmarks, key = { it }) { bookmark ->
            BookmarkItem(
                bookmark = bookmark,
                textColor = backgroundColor,
                isMultiSelecting = state.isMultiSelecting,
                isSelected = state.isSelected,
                modifier = Modifier
                    .pointerInput(state.isSelected(bookmark)) {
                        detectTapGestures(
                            onLongPress = { state.onRowLongPress(bookmark) },
                            onTap = { state.onRowClick(bookmark) }
                        )
                    }
            )
        }
    }
}

@Composable
private fun HeaderItem(title: String) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp)
    ) {
        TextH40(
            text = title,
            color = MaterialTheme.theme.colors.playerContrast02,
        )
        IconButton(
            onClick = { /* TODO */ },
        ) {
            Icon(
                painter = painterResource(IR.drawable.ic_more_vert_black_24dp),
                contentDescription = stringResource(LR.string.more_options),
                tint = MaterialTheme.theme.colors.playerContrast01,
            )
        }
    }
}

@Composable
private fun BookmarkItem(
    bookmark: Bookmark,
    textColor: Color,
    isMultiSelecting: Boolean,
    isSelected: (Bookmark) -> Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        Spacer(
            modifier = Modifier
                .height(1.dp)
                .fillMaxWidth()
                .background(color = MaterialTheme.theme.colors.playerContrast05)
        )
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = if (isMultiSelecting && isSelected(bookmark)) {
                        MaterialTheme.theme.colors.primaryUi02Selected
                    } else {
                        Color.Transparent
                    }
                )

        ) {
            val createdAtText by remember {
                mutableStateOf(
                    bookmark.createdAt.toLocalizedFormatPattern(
                        bookmark.createdAtDatePattern()
                    )
                )
            }

            if (isMultiSelecting) {
                Checkbox(
                    checked = isSelected(bookmark),
                    onCheckedChange = null,
                    modifier = Modifier
                        .padding(start = 16.dp)
                )
            }
            Column(
                Modifier.weight(1f)
            ) {
                TextH40(
                    text = bookmark.title,
                    color = MaterialTheme.theme.colors.playerContrast01,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 16.dp, start = 16.dp),
                )

                TextH60(
                    text = createdAtText,
                    color = MaterialTheme.theme.colors.playerContrast02,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp, start = 16.dp),
                    maxLines = 1,
                )
            }
            Box(modifier = Modifier.padding(end = 16.dp)) {
                PlayButton(
                    bookmark = bookmark,
                    textColor = textColor,
                )
            }
        }
    }
}

@Composable
private fun PlayButton(
    bookmark: Bookmark,
    textColor: Color,
) {
    val timeText by remember {
        mutableStateOf(TimeHelper.formattedSeconds(bookmark.timeSecs.toDouble()))
    }
    val description = stringResource(LR.string.bookmark_play, timeText)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = MaterialTheme.theme.colors.playerContrast01,
                shape = RoundedCornerShape(size = 42.dp)
            )
            .clickable { /* TODO */ }
            .semantics {
                contentDescription = description
            },
    ) {
        TextH40(
            text = timeText,
            color = textColor,
            modifier = Modifier
                .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
                .clearAndSetSemantics { },
        )
        Icon(
            painter = painterResource(IR.drawable.ic_play),
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier
                .padding(start = 10.dp, end = 16.dp)
                .size(10.dp, 13.dp)
        )
    }
}

@Composable
private fun NoBookmarksView() {
    MessageView(
        titleView = {
            TextH20(
                text = stringResource(LR.string.bookmarks_not_found),
                color = MaterialTheme.theme.colors.playerContrast01,
            )
        },
        buttonTitleRes = LR.string.bookmarks_headphone_settings,
        buttonAction = { /* TODO */ },
    )
}

@Composable
private fun PlusUpsellView() {
    MessageView(
        titleView = {
            Image(
                painter = painterResource(IR.drawable.pocket_casts_plus_logo),
                contentDescription = stringResource(LR.string.pocket_casts),
            )
        },
        buttonTitleRes = LR.string.subscribe, // TODO: Bookmarks update upsell button title based on subscription status
        buttonAction = { /* TODO */ },
    )
}

@Composable
private fun MessageView(
    titleView: @Composable () -> Unit = {},
    @StringRes buttonTitleRes: Int,
    buttonAction: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(
                    color = MaterialTheme.theme.colors.playerContrast06,
                    shape = RoundedCornerShape(size = 4.dp)
                )
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp)
            ) {
                titleView()
                TextP40(
                    text = stringResource(LR.string.bookmarks_create_instructions),
                    color = MaterialTheme.theme.colors.playerContrast02,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
                TextButton(
                    buttonTitleRes = buttonTitleRes,
                    buttonAction = buttonAction,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun TextButton(
    @StringRes buttonTitleRes: Int,
    buttonAction: () -> Unit = {},
    modifier: Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable { buttonAction() }
    ) {
        TextH40(
            text = stringResource(buttonTitleRes),
            color = MaterialTheme.theme.colors.playerContrast01,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )
    }
}

@Composable
private fun LoadingView() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .progressSemantics()
                .size(24.dp),
            strokeWidth = 2.dp,
        )
    }
}

private fun Bookmark.createdAtDatePattern(): String {
    val calendar = Calendar.getInstance()
    val currentYear = calendar.get(Calendar.YEAR)
    calendar.time = createdAt
    val createdAtYear = calendar.get(Calendar.YEAR)

    return if (createdAtYear == currentYear) {
        "MMM d 'at' h:mm a"
    } else {
        "MMM d, YYYY 'at' h:mm a"
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
                    )
                ),
                isMultiSelecting = false,
                isSelected = { false },
                onRowLongPress = {},
                onRowClick = {},
            ),
            backgroundColor = Color.Black,
        )
    }
}

@Preview
@Composable
private fun NoBookmarksPreview(
    theme: Theme.ThemeType = Theme.ThemeType.DARK,
) {
    AppTheme(theme) {
        Content(
            state = UiState.Empty,
            backgroundColor = Color.Black,
        )
    }
}

@Preview
@Composable
private fun PlusUpsellPreview(
    theme: Theme.ThemeType = Theme.ThemeType.DARK,
) {
    AppTheme(theme) {
        Content(
            state = UiState.PlusUpsell,
            backgroundColor = Color.Black,
        )
    }
}
