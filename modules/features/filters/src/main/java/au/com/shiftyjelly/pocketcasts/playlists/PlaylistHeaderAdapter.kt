package au.com.shiftyjelly.pocketcasts.playlists

import android.os.Build
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// This adapter uses an unconventional configuration: it has only a single view holder, and data is provided via a Flow.
// Updating data through the regular adapter mechanisms causes the UI to flicker, because setContent() is called again,
// which clears and rebuilds the composition. To avoid this, the adapter relies on a single view holder that consumes
// data from an internally stored Flow.
internal class PlaylistHeaderAdapter(
    private val themeType: Theme.ThemeType,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val headerDataFlow = MutableStateFlow<PlaylistHeaderData?>(null)

    fun submitHeader(data: PlaylistHeaderData?) {
        headerDataFlow.value = data
    }

    override fun getItemCount() = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PlaylistHeaderViewHolder(
            composeView = ComposeView(parent.context),
            themeType = themeType,
            headerDataFlow = headerDataFlow,
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as PlaylistHeaderViewHolder).bind()
    }
}

private class PlaylistHeaderViewHolder(
    private val composeView: ComposeView,
    private val themeType: Theme.ThemeType,
    private val headerDataFlow: StateFlow<PlaylistHeaderData?>,
) : RecyclerView.ViewHolder(composeView) {
    init {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
    }

    fun bind() {
        composeView.setContent {
            val headerData by headerDataFlow.collectAsState()
            val statusBarPadding = WindowInsets.statusBars.asPaddingValues()
                .calculateTopPadding()
                .coerceAtLeast(cachedStatusBarPadding)
            cachedStatusBarPadding = statusBarPadding

            AppTheme(themeType) {
                PlaylistHeader(
                    data = headerData,
                    useBlurredArtwork = Build.VERSION.SDK_INT >= 31,
                    contentPadding = PaddingValues(
                        top = statusBarPadding + 56.dp, // Eyeball the position below app bar
                    ),
                    modifier = Modifier.background(MaterialTheme.theme.colors.primaryUi02),
                )
            }
        }
    }
}

// We can't simply apply 'WindowInsets.statusBars' inset.
// When navigating to the playlist page the inset isn't available before first layout
// which can cause an ugly jump effect.
//
// 48.dp is a standard status bar height and should be good enough for the initial pass.
private var cachedStatusBarPadding: Dp = 48.dp
