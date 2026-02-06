package au.com.shiftyjelly.pocketcasts.views.adapter

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem

class FolderItemDiffCallback : DiffUtil.ItemCallback<FolderItem>() {
    override fun areItemsTheSame(oldItem: FolderItem, newItem: FolderItem): Boolean {
        return oldItem.uuid == newItem.uuid
    }

    override fun areContentsTheSame(oldItem: FolderItem, newItem: FolderItem): Boolean {
        return when (oldItem) {
            is FolderItem.Podcast if newItem is FolderItem.Podcast -> {
                val oldPodcast = oldItem.podcast
                val newPodcast = newItem.podcast
                oldPodcast.uuid == newPodcast.uuid &&
                    oldPodcast.title == newPodcast.title &&
                    oldPodcast.unplayedEpisodeCount == newPodcast.unplayedEpisodeCount
            }

            is FolderItem.Folder if newItem is FolderItem.Folder -> {
                val oldFolder = oldItem.folder
                val newFolder = newItem.folder
                @SuppressLint("DiffUtilEquals") // Lists implement correct equality
                oldFolder.name == newFolder.name &&
                    oldFolder.color == newFolder.color &&
                    oldItem.podcasts.map { it.uuid } == newItem.podcasts.map { it.uuid }
            }

            else -> {
                false
            }
        }
    }
}
