package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.asFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.player.viewmodel.BookmarksViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.BookmarksViewModel.UiState
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
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
                BookmarksPage(
                    playerViewModel = playerViewModel
                )
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
        val backgroundColor = Color(it.podcastHeader.backgroundColor)
        when (state) {
            is UiState.Empty,
            is UiState.Loading,
            -> Unit

            is UiState.Loaded -> Content(
                bookmarks = (state as UiState.Loaded).bookmarks,
                backgroundColor = backgroundColor,
            )
        }
        LaunchedEffect(Unit) {
            bookmarksViewModel.loadBookmarks(
                episodeUuid = it.podcastHeader.episodeUuid
            )
        }
    }
}

@Composable
private fun Content(
    bookmarks: List<Bookmark>,
    backgroundColor: Color,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(color = backgroundColor)
    ) {
        item {
            val title = stringResource(
                id = if (bookmarks.size > 1) {
                    LR.string.bookmarks_plural
                } else {
                    LR.string.bookmarks_singular
                },
                bookmarks.size
            )
            HeaderItem(title)
        }
        items(bookmarks) { bookmark ->
            BookmarkItem(
                bookmark = bookmark,
                textColor = backgroundColor
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
) {
    Column(
        modifier = Modifier
            .clickable { /* TODO */ }
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
                .padding(16.dp)
        ) {
            val createdAtText by remember {
                mutableStateOf(
                    bookmark.createdAt.toLocalizedFormatPattern(
                        pattern = "MMM d, YYYY 'at' h:mm a"
                    )
                )
            }
            Column(
                Modifier.weight(1f, fill = true)
            ) {
                TextH40(
                    text = bookmark.title,
                    color = MaterialTheme.theme.colors.playerContrast01,
                    maxLines = 2,
                )

                TextH60(
                    text = createdAtText,
                    color = MaterialTheme.theme.colors.playerContrast02,
                    modifier = Modifier.padding(top = 8.dp),
                    maxLines = 1,
                )
            }
            PlayButton(
                bookmark = bookmark,
                textColor = textColor,
            )
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
                .size(10.dp, 16.dp)
        )
    }
}

@Preview
@Composable
private fun BookmarksPreview(
    theme: Theme.ThemeType = Theme.ThemeType.DARK,
) {
    AppTheme(theme) {
        Content(
            bookmarks = listOf(
                Bookmark(
                    uuid = UUID.randomUUID().toString(),
                    episodeUuid = UUID.randomUUID().toString(),
                    podcastUuid = UUID.randomUUID().toString(),
                    timeSecs = 10,
                    createdAt = Date(),
                    syncStatus = 1,
                    title = "Funny bit",
                )
            ),
            backgroundColor = Color.Black,
        )
    }
}
