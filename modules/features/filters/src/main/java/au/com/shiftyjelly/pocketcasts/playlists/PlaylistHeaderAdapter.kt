package au.com.shiftyjelly.pocketcasts.playlists

import android.os.Build
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeader
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderButtonData
import au.com.shiftyjelly.pocketcasts.playlists.component.PlaylistHeaderData
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlinx.coroutines.flow.MutableStateFlow
import au.com.shiftyjelly.pocketcasts.ui.R as UR

// This adapter uses an unconventional configuration: it has only a single view holder, and data is provided via a Flow.
// Updating data through the regular adapter mechanisms causes the UI to flicker, because setContent() is called again,
// which clears and rebuilds the composition. To avoid this, the adapter relies on a single view holder that consumes
// data from an internally stored Flow.
internal class PlaylistHeaderAdapter(
    private val themeType: Theme.ThemeType,
    private val leftButton: PlaylistHeaderButtonData,
    private val rightButton: PlaylistHeaderButtonData,
    private val searchState: TextFieldState,
    private val onChangeSearchFocus: (Boolean, Float) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val headerDataFlow = MutableStateFlow<PlaylistHeaderData?>(null)
    private var searchTopOffset = 0f

    fun submitHeader(data: PlaylistHeaderData?) {
        headerDataFlow.value = data
    }

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PlaylistHeaderViewHolder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as PlaylistHeaderViewHolder).bind()
    }

    private inner class PlaylistHeaderViewHolder(
        private val composeView: ComposeView,
    ) : RecyclerView.ViewHolder(composeView) {
        init {
            composeView.setTag(UR.id.playlist_view_header_tag, true)
            composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
        }

        fun bind() {
            composeView.setContent {
                val headerData by headerDataFlow.collectAsState()
                var isFocused by remember { mutableStateOf<Boolean?>(null) }
                val keyboard = LocalSoftwareKeyboardController.current

                AppTheme(themeType) {
                    PlaylistHeader(
                        data = headerData,
                        leftButton = leftButton,
                        rightButton = rightButton,
                        searchState = searchState,
                        useBlurredArtwork = Build.VERSION.SDK_INT >= 31,
                        onMeasureSearchTopOffset = { topOffset -> searchTopOffset = topOffset },
                        onChangeSearchFocus = { focusState ->
                            val hasFocus = focusState.hasFocus
                            if (isFocused != hasFocus) {
                                isFocused = hasFocus
                                onChangeSearchFocus(hasFocus, searchTopOffset)
                                keyboard?.show()
                            }
                        },
                        modifier = Modifier.background(MaterialTheme.theme.colors.primaryUi02),
                    )
                }
            }
        }
    }
}
