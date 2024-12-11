package au.com.shiftyjelly.pocketcasts.player.view

import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.models.to.Chapter

interface PlayerClickListener {
    fun onShowNotesClick(episodeUuid: String)
    fun onClosePlayer()
    fun onPictureInPictureClick()
    fun onFullScreenVideoClick()
    fun onSeekPositionChangeStop(progress: Int, seekComplete: () -> Unit)
}

interface ChapterListener {
    fun onChapterClick(chapter: Chapter)
    fun onChapterUrlClick(chapter: Chapter)
}

interface UpNextListener {
    fun onClearUpNext()
    fun onUpNextEpisodeStartDrag(viewHolder: RecyclerView.ViewHolder)
    fun onEpisodeActionsClick(episodeUuid: String, podcastUuid: String?)
    fun onEpisodeActionsLongPress(episodeUuid: String, podcastUuid: String?)
    fun onNowPlayingClick()
}
