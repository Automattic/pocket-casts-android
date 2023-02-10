package au.com.shiftyjelly.pocketcasts.search.adapter

import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.adapter.FolderItemDiffCallback

class PodcastSearchAdapter(
    val theme: Theme,
    val onPodcastClick: (Podcast) -> Unit,
    val onFolderClick: (Folder, List<Podcast>) -> Unit
) : ListAdapter<FolderItem, RecyclerView.ViewHolder>(FolderItemDiffCallback()) {

    override fun getItemId(position: Int): Long {
        return getItem(position).adapterId
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FolderItem.Podcast -> FolderItem.Podcast.viewTypeId
            is FolderItem.Folder -> FolderItem.Folder.viewTypeId
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            FolderItem.Podcast.viewTypeId -> {
                SearchPodcastViewHolder(
                    composeView = ComposeView(parent.context),
                    theme = theme,
                    onPodcastClick = onPodcastClick
                )
            }
            FolderItem.Folder.viewTypeId -> {
                SearchFolderViewHolder(
                    composeView = ComposeView(parent.context),
                    theme = theme,
                    onFolderClick = onFolderClick
                )
            }
            else -> throw Exception("Unknown view type $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is FolderItem.Podcast -> (holder as SearchPodcastViewHolder).bind(item.podcast)
            is FolderItem.Folder -> (holder as SearchFolderViewHolder).bind(item.folder, item.podcasts)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        when (holder) {
            is SearchPodcastViewHolder -> {
                holder.composeView.disposeComposition()
            }
            is SearchFolderViewHolder -> {
                holder.composeView.disposeComposition()
            }
        }
    }
}
