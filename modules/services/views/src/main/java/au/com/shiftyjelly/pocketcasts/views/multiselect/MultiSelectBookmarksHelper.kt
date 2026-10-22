package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.content.res.Resources
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.asFlow
import androidx.lifecycle.liveData
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPlural
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.views.R
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

class MultiSelectBookmarksHelper @Inject constructor(
    private val bookmarkManager: BookmarkManager,
    private val bookmarkDeleter: BookmarkDeleter,
    var episodeManager: EpisodeManager,
) : MultiSelectHelper<Bookmark>() {
    override val maxToolbarIcons = 3

    private val _navigationState = MutableSharedFlow<NavigationState>()
    val navigationState = _navigationState.asSharedFlow()

    override var source by bookmarkManager::sourceView

    override val toolbarActions: LiveData<List<MultiSelectAction>> = liveData {
        _selectedListLive.asFlow().collect { selectedList ->
            Timber.d("MultiSelectBookmarksHelper toolbarActions updated, ${selectedList.size} bookmarks from $source")

            val actions = listOf(
                MultiSelectBookmarkAction.ShareBookmark(
                    isVisible = source != SourceView.FILES && selectedList.count() == 1 && isEligibleToShare(),
                ),
                MultiSelectBookmarkAction.EditBookmark(isVisible = selectedList.count() == 1),
                MultiSelectBookmarkAction.DeleteBookmark,
                MultiSelectAction.SelectAll,
            )

            emit(actions)
        }
    }

    override fun isSelected(multiSelectable: Bookmark) = selectedSet.any { it.uuid == multiSelectable.uuid }

    override fun onMenuItemSelected(
        itemId: Int,
        resources: Resources,
        activity: FragmentActivity,
    ): Boolean {
        return when (itemId) {
            R.id.menu_share -> {
                share()
                true
            }

            UR.id.menu_edit -> {
                edit()
                true
            }

            R.id.menu_delete -> {
                delete(resources, activity.supportFragmentManager)
                true
            }

            R.id.menu_select_all -> {
                selectAll()
                true
            }

            else -> false
        }
    }

    override fun deselect(multiSelectable: Bookmark) {
        if (isSelected(multiSelectable)) {
            selectedSet.removeIf { it.uuid == multiSelectable.uuid }
        }

        _selectedListLive.value = selectedSet.toList()

        if (selectedSet.isEmpty()) {
            closeMultiSelect()
        }
    }

    private fun share() {
        launch { _navigationState.emit(NavigationState.ShareBookmark) }
    }

    private fun edit() {
        launch { _navigationState.emit(NavigationState.EditBookmark) }
    }

    fun delete(resources: Resources, fragmentManager: FragmentManager) {
        val bookmarks = selectedSet.toList()
        if (bookmarks.isEmpty()) {
            closeMultiSelect()
            return
        }
        bookmarkDeleter.confirmDelete(
            bookmarks = bookmarks,
            source = source,
            resources = resources,
            fragmentManager = fragmentManager,
            scope = this,
            onConfirmed = { closeMultiSelect() },
            onDeleted = { count ->
                showSnackBar(
                    resources.getStringPlural(
                        count,
                        LR.string.bookmarks_deleted_singular,
                        LR.string.bookmarks_deleted_plural,
                    ),
                )
            },
        )
    }

    private suspend fun isEligibleToShare(): Boolean {
        val bookmark = selectedSet.first()

        val episode = episodeManager.findEpisodeByUuid(bookmark.episodeUuid)
        return episode is PodcastEpisode
    }

    sealed class NavigationState {
        data object EditBookmark : NavigationState()
        data object ShareBookmark : NavigationState()
    }
}
