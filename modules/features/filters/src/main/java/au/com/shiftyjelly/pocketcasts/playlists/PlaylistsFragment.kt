package au.com.shiftyjelly.pocketcasts.playlists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.extensions.contentWithoutConsumedInsets
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.TopScrollable
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PlaylistsFragment :
    BaseFragment(),
    TopScrollable {
    private val scrollToTopSignal = MutableSharedFlow<Unit>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = contentWithoutConsumedInsets {
        val listState = rememberLazyListState()

        AppThemeWithBackground(theme.activeTheme) {
            LazyColumn(
                state = listState,
            ) {
                items(30) { index ->
                    Text(
                        text = "Item $index",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp,
                        modifier = Modifier
                            .background(if (index % 2 == 0) Color.Red else Color.Blue)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    )
                }
            }
        }

        LaunchedEffect(listState) {
            scrollToTopSignal.collectLatest {
                listState.animateScrollToItem(0)
            }
        }
    }

    override fun scrollToTop() {
        lifecycleScope.launch {
            scrollToTopSignal.emit(Unit)
        }
    }
}
