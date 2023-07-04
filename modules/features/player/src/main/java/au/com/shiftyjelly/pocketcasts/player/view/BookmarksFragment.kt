package au.com.shiftyjelly.pocketcasts.player.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.asFlow
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import dagger.hilt.android.AndroidEntryPoint
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
            AppThemeWithBackground(theme.activeTheme) {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                BookmarksPage(
                    playerViewModel = playerViewModel
                )
            }
        }
    }
}

@Composable
fun BookmarksPage(
    playerViewModel: PlayerViewModel,
) {
    val listData = playerViewModel.listDataLive.asFlow().collectAsState(initial = null)
    listData.value?.let {
        val backgroundColor = Color(it.podcastHeader.backgroundColor)
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(color = backgroundColor)
        ) {
            item {
                HeaderItem()
            }
            item {
                BookmarkItem(backgroundColor)
            }
        }
    }
}

@Composable
private fun HeaderItem() {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        TextH40(
            text = stringResource(id = LR.string.bookmarks_singular),
            color = MaterialTheme.theme.colors.playerContrast02,
        )
        Icon(
            painter = painterResource(id = IR.drawable.ic_more_vert_black_24dp),
            contentDescription = stringResource(id = LR.string.more_options),
            tint = MaterialTheme.theme.colors.playerContrast01,
        )
    }
}

@Composable
private fun BookmarkItem(
    backgroundColor: Color,
) {
    Column {
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
            Column {
                TextH40(
                    text = "Funny bit",
                    color = MaterialTheme.theme.colors.playerContrast01,
                )

                TextH60(
                    text = "Feb 15, 2023 at 9:41 AM",
                    color = MaterialTheme.theme.colors.playerContrast02,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.theme.colors.playerContrast01,
                        shape = RoundedCornerShape(size = 42.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                TextH40(
                    text = "23:10",
                    color = backgroundColor,
                )
                Icon(
                    painter = painterResource(id = IR.drawable.ic_play),
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .size(10.dp, 16.dp)
                )
            }
        }
    }
}
