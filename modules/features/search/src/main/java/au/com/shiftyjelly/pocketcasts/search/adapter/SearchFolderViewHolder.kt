package au.com.shiftyjelly.pocketcasts.search.adapter

import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.search.component.SearchFolderRow
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

class SearchFolderViewHolder(
    val composeView: ComposeView,
    val theme: Theme,
    val onFolderClick: (Folder, List<Podcast>) -> Unit
) : RecyclerView.ViewHolder(composeView) {

    init {
        composeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
    }

    fun bind(folder: Folder, podcasts: List<Podcast>) {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                SearchFolderRow(folder, podcasts, onClick = { onFolderClick(folder, podcasts) })
            }
        }
    }
}
