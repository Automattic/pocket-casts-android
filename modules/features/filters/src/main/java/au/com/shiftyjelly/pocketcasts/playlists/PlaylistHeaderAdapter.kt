package au.com.shiftyjelly.pocketcasts.playlists

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

internal class PlaylistHeaderAdapter(
    private val themeType: Theme.ThemeType,
) : ListAdapter<String, RecyclerView.ViewHolder>(PlaylistHeaderDiffCallback) {
    fun submitHeader(data: String) = submitList(listOf(data))

    override fun submitList(list: List<String>?) {
        require(list == null || list.size <= 1) { "Header cannot have more than 1 element" }
        super.submitList(list ?: listOf(""))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return PlaylistHeaderViewHolder(
            composeView = ComposeView(parent.context),
            themeType = themeType,
        )
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as PlaylistHeaderViewHolder).bind(currentList[position])
    }
}

private class PlaylistHeaderViewHolder(
    private val composeView: ComposeView,
    private val themeType: Theme.ThemeType,
) : RecyclerView.ViewHolder(composeView) {
    init {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
    }

    fun bind(podcastTitle: String) {
        composeView.setContent {
            AppTheme(themeType) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding(),
                ) {
                    TextH20(
                        text = podcastTitle,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .padding(vertical = 32.dp)
                            .background(MaterialTheme.theme.colors.primaryUi02Active, RoundedCornerShape(8.dp))
                            .padding(16.dp),
                    )
                }
            }
        }
    }
}

private object PlaylistHeaderDiffCallback : DiffUtil.ItemCallback<String>() {
    override fun areItemsTheSame(oldItem: String, newItem: String) = oldItem == newItem

    override fun areContentsTheSame(oldItem: String, newItem: String) = oldItem == newItem

    override fun getChangePayload(oldItem: String, newItem: String) = String
}
