package au.com.shiftyjelly.pocketcasts.views.adapter

import androidx.recyclerview.widget.DiffUtil
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem

class FolderItemDiffCallback : DiffUtil.ItemCallback<FolderItem>() {
    override fun areItemsTheSame(oldItem: FolderItem, newItem: FolderItem): Boolean {
        return oldItem.uuid == newItem.uuid
    }

    override fun areContentsTheSame(oldItem: FolderItem, newItem: FolderItem): Boolean {
        if (oldItem is FolderItem.Podcast && newItem is FolderItem.Podcast) {
            val oldPodcast = oldItem.podcast
            val newPodcast = newItem.podcast
            return oldPodcast.uuid == newPodcast.uuid &&
                oldPodcast.title == newPodcast.title &&
                oldPodcast.unplayedEpisodeCount == newPodcast.unplayedEpisodeCount
        } else if (oldItem is FolderItem.Folder && newItem is FolderItem.Folder) {
            val oldFolder = oldItem.folder
            val newFolder = newItem.folder
            return oldFolder.name == newFolder.name &&
                oldFolder.color == newFolder.color &&
                oldItem.podcasts.map { it.uuid } == newItem.podcasts.map { it.uuid }
        } else {
            return false
        }
    }
}
