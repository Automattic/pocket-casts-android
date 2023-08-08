package au.com.shiftyjelly.pocketcasts.views.multiselect

import android.content.res.Resources
import android.widget.Toast
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.map
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.extensions.getStringPlural
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.utils.combineLatest
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.views.R
import au.com.shiftyjelly.pocketcasts.views.dialog.ConfirmationDialog
import au.com.shiftyjelly.pocketcasts.views.dialog.ShareDialog
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.DeleteState
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private const val WARNING_LIMIT = 3
@Singleton
class MultiSelectEpisodesHelper @Inject constructor(
    val episodeManager: EpisodeManager,
    val userEpisodeManager: UserEpisodeManager,
    val podcastManager: PodcastManager,
    val playbackManager: PlaybackManager,
    val downloadManager: DownloadManager,
    val analyticsTracker: AnalyticsTrackerWrapper,
    val settings: Settings,
    private val episodeAnalytics: EpisodeAnalytics
) : MultiSelectHelper<BaseEpisode>() {
    override val maxToolbarIcons = 4

    override val toolbarActions: LiveData<List<MultiSelectAction>> = settings.multiSelectItemsObservable
        .toFlowable(BackpressureStrategy.LATEST)
        .map { MultiSelectEpisodeAction.listFromIds(it) }
        .toLiveData()
        .combineLatest(_selectedListLive)
        .map { (actions, selectedEpisodes) ->
            actions.mapNotNull {
                MultiSelectEpisodeAction.actionForGroup(it.groupId, selectedEpisodes)
            }
        }

    override lateinit var listener: Listener<BaseEpisode>

    override fun isSelected(multiSelectable: BaseEpisode) =
        selectedList.count { it.uuid == multiSelectable.uuid } > 0

    override fun onMenuItemSelected(itemId: Int, resources: Resources, fragmentManager: FragmentManager): Boolean {
        return when (itemId) {
            R.id.menu_archive -> {
                archive(resources = resources, fragmentManager = fragmentManager)
                true
            }
            UR.id.menu_unarchive -> {
                unarchive(resources = resources)
                true
            }
            R.id.menu_delete -> {
                delete(resources, fragmentManager)
                true
            }
            R.id.menu_share -> {
                share(fragmentManager)
                true
            }
            R.id.menu_download -> {
                download(resources, fragmentManager)
                true
            }
            UR.id.menu_undownload -> {
                deleteDownload()
                true
            }
            R.id.menu_mark_played -> {
                markAsPlayed(resources = resources, fragmentManager = fragmentManager)
                true
            }
            UR.id.menu_markasunplayed -> {
                markAsUnplayed(resources = resources)
                true
            }
            R.id.menu_playnext -> {
                playNext(resources = resources)
                true
            }
            R.id.menu_playlast -> {
                playLast(resources = resources)
                true
            }
            R.id.menu_select_all -> {
                selectAll()
                true
            }
            R.id.menu_unselect_all -> {
                unselectAll()
                true
            }
            R.id.menu_removefromupnext -> {
                removeFromUpNext(resources = resources)
                true
            }
            R.id.menu_movetotop -> {
                moveToTop()
                true
            }
            R.id.menu_movetobottom -> {
                moveToBottom()
                true
            }
            R.id.menu_star -> {
                star(resources = resources)
                true
            }
            UR.id.menu_unstar -> {
                unstar(resources = resources)
                true
            }
            else -> false
        }
    }

    override fun deselect(multiSelectable: BaseEpisode) {
        if (isSelected(multiSelectable)) {
            val selectedItem = selectedList.firstOrNull { it.uuid == multiSelectable.uuid }
            selectedItem?.let { selectedList.remove(it) }
        }

        _selectedListLive.value = selectedList

        if (selectedList.isEmpty()) {
            closeMultiSelect()
        }
    }

    fun markAsPlayed(shownWarning: Boolean = false, resources: Resources, fragmentManager: FragmentManager) {
        if (selectedList.isEmpty()) {
            closeMultiSelect()
            return
        }

        launch {
            val list = selectedList.toList()
            if (!shownWarning && list.size > WARNING_LIMIT) {
                playedWarning(list.size, resources = resources, fragmentManager = fragmentManager)
                return@launch
            }

            episodeManager.markAllAsPlayed(list, playbackManager, podcastManager)
            episodeAnalytics.trackBulkEvent(AnalyticsEvent.EPISODE_BULK_MARKED_AS_PLAYED, source, list.size)
            launch(Dispatchers.Main) {
                val snackText = resources.getStringPlural(selectedList.size, LR.string.marked_as_played_singular, LR.string.marked_as_played_plural)
                showSnackBar(snackText)
                closeMultiSelect()
            }
        }
    }

    fun markAsUnplayed(resources: Resources) {
        if (selectedList.isEmpty()) {
            closeMultiSelect()
            return
        }

        launch {
            val list = selectedList.toList()

            episodeManager.markAsUnplayed(list)
            episodeAnalytics.trackBulkEvent(AnalyticsEvent.EPISODE_BULK_MARKED_AS_UNPLAYED, source, list.size)
            launch(Dispatchers.Main) {
                val snackText = resources.getStringPlural(selectedList.size, LR.string.marked_as_unplayed_singular, LR.string.marked_as_unplayed_plural)
                showSnackBar(snackText)
                closeMultiSelect()
            }
        }
    }

    fun archive(shownWarning: Boolean = false, resources: Resources, fragmentManager: FragmentManager) {
        if (selectedList.isEmpty()) {
            closeMultiSelect()
            return
        }

        launch {
            val list = selectedList.filterIsInstance<PodcastEpisode>().toList()
            if (!shownWarning && list.size > WARNING_LIMIT) {
                archiveWarning(list.size, resources = resources, fragmentManager = fragmentManager)
                return@launch
            }

            episodeManager.archiveAllInList(list, playbackManager)
            episodeAnalytics.trackBulkEvent(AnalyticsEvent.EPISODE_BULK_ARCHIVED, source, list.size)
            withContext(Dispatchers.Main) {
                val snackText = resources.getStringPlural(selectedList.size, LR.string.archived_episodes_singular, LR.string.archived_episodes_plural)
                showSnackBar(snackText)
                closeMultiSelect()
            }
        }
    }

    fun unarchive(resources: Resources) {
        if (selectedList.isEmpty()) {
            closeMultiSelect()
            return
        }

        launch {
            val list = selectedList.filterIsInstance<PodcastEpisode>().toList()

            episodeManager.unarchiveAllInList(episodes = list)
            episodeAnalytics.trackBulkEvent(AnalyticsEvent.EPISODE_BULK_UNARCHIVED, source, list.size)
            withContext(Dispatchers.Main) {
                val snackText = resources.getStringPlural(selectedList.size, LR.string.unarchived_episodes_singular, LR.string.unarchived_episodes_plural)
                showSnackBar(snackText)
                closeMultiSelect()
            }
        }
    }

    fun star(resources: Resources) {
        if (selectedList.isEmpty()) {
            closeMultiSelect()
            return
        }

        launch {
            val list = selectedList.filterIsInstance<PodcastEpisode>().toList()
            episodeManager.updateAllStarred(list, starred = true)
            episodeAnalytics.trackBulkEvent(AnalyticsEvent.EPISODE_BULK_STARRED, source, list.size)
            withContext(Dispatchers.Main) {
                val snackText = resources.getStringPlural(selectedList.size, LR.string.starred_episodes_singular, LR.string.starred_episodes_plural)
                showSnackBar(snackText)
                closeMultiSelect()
            }
        }
    }

    fun unstar(resources: Resources) {
        if (selectedList.isEmpty()) {
            closeMultiSelect()
            return
        }

        launch {
            val list = selectedList.filterIsInstance<PodcastEpisode>().toList()
            episodeManager.updateAllStarred(list, starred = false)
            episodeAnalytics.trackBulkEvent(AnalyticsEvent.EPISODE_BULK_UNSTARRED, source, list.size)
            withContext(Dispatchers.Main) {
                val snackText = resources.getStringPlural(selectedList.size, LR.string.unstarred_episodes_singular, LR.string.unstarred_episodes_plural)
                showSnackBar(snackText)
                closeMultiSelect()
            }
        }
    }

    fun playedWarning(count: Int, resources: Resources, fragmentManager: FragmentManager) {
        val buttonString = resources.getStringPlural(count = count, singular = LR.string.mark_as_played_singular, plural = LR.string.mark_as_played_plural)

        ConfirmationDialog()
            .setTitle(resources.getString(LR.string.mark_as_played_title))
            .setIconId(IR.drawable.ic_markasplayed)
            .setButtonType(ConfirmationDialog.ButtonType.Danger(buttonString))
            .setOnConfirm { markAsPlayed(shownWarning = true, resources = resources, fragmentManager = fragmentManager) }
            .show(fragmentManager, "confirm_played_all_")
    }

    fun archiveWarning(count: Int, resources: Resources, fragmentManager: FragmentManager) {
        val buttonString = resources.getStringPlural(count = count, singular = LR.string.archive_episodes_singular, plural = LR.string.archive_episodes_plural)

        ConfirmationDialog()
            .setTitle(resources.getString(LR.string.archive_title))
            .setSummary(resources.getString(LR.string.archive_summary))
            .setIconId(IR.drawable.ic_archive)
            .setButtonType(ConfirmationDialog.ButtonType.Danger(buttonString))
            .setOnConfirm { archive(shownWarning = true, resources = resources, fragmentManager = fragmentManager) }
            .show(fragmentManager, "confirm_archive_all_")
    }

    fun download(resources: Resources, fragmentManager: FragmentManager) {
        if (selectedList.isEmpty()) {
            closeMultiSelect()
            return
        }

        val list = selectedList.toList()
        val trimmedList = list.subList(0, min(Settings.MAX_DOWNLOAD, selectedList.count())).toList()
        ConfirmationDialog.downloadWarningDialog(list.count(), resources) {
            trimmedList.forEach {
                downloadManager.addEpisodeToQueue(it, "podcast download all", false)
            }
            episodeAnalytics.trackBulkEvent(AnalyticsEvent.EPISODE_BULK_DOWNLOAD_QUEUED, source, trimmedList)
            val snackText = resources.getStringPlural(trimmedList.size, LR.string.download_queued_singular, LR.string.download_queued_plural)
            showSnackBar(snackText)
            closeMultiSelect()
        }?.show(fragmentManager, "multiselect_download")
    }

    fun deleteDownload() {
        if (selectedList.isEmpty()) {
            closeMultiSelect()
            return
        }

        val list = selectedList.toList()
        launch {
            val episodes = list.filterIsInstance<PodcastEpisode>()
            episodeManager.deleteEpisodeFiles(episodes, playbackManager)

            val userEpisodes = list.filterIsInstance<UserEpisode>()
            userEpisodeManager.deleteAll(userEpisodes, playbackManager)

            if (episodes.isNotEmpty()) {
                episodeAnalytics.trackBulkEvent(
                    AnalyticsEvent.EPISODE_BULK_DOWNLOAD_DELETED,
                    source = source,
                    count = if (episodes.isNotEmpty()) episodes.size else userEpisodes.size
                )
            }

            withContext(Dispatchers.Main) {
                closeMultiSelect()
            }
        }
    }

    fun playNext(resources: Resources) {
        if (selectedList.isEmpty()) {
            closeMultiSelect()
            return
        }

        val size = min(settings.getMaxUpNextEpisodes(), selectedList.count())
        val trimmedList = selectedList.subList(0, size).toList()
        launch {
            playbackManager.playEpisodesNext(episodes = trimmedList, source = source)
            withContext(Dispatchers.Main) {
                val snackText = resources.getStringPlural(size, LR.string.added_to_up_next_singular, LR.string.added_to_up_next_plural)
                showSnackBar(snackText)
                closeMultiSelect()
            }
        }
    }

    fun playLast(resources: Resources) {
        if (selectedList.isEmpty()) {
            closeMultiSelect()
            return
        }

        val size = min(settings.getMaxUpNextEpisodes(), selectedList.count())
        val trimmedList = selectedList.subList(0, size).toList()
        launch {
            playbackManager.playEpisodesLast(episodes = trimmedList, source = source)
            withContext(Dispatchers.Main) {
                val snackText = resources.getStringPlural(size, LR.string.added_to_up_next_singular, LR.string.added_to_up_next_plural)
                showSnackBar(snackText)
                closeMultiSelect()
            }
        }
    }

    fun delete(resources: Resources, fragmentManager: FragmentManager) {
        val episodes = selectedList.filterIsInstance<UserEpisode>()
        if (episodes.isEmpty()) return

        val onServer = episodes.count { it.isUploaded }
        val onDevice = episodes.count { it.isDownloaded }

        val deleteState = CloudDeleteHelper.getDeleteState(isDownloaded = onDevice > 0, isBoth = onServer > 0 && onDevice > 0)
        val deleteFunction: (List<UserEpisode>, DeleteState) -> Unit = { episodesToDelete, state ->
            episodesToDelete.forEach {
                Timber.d("Deleting $it")
                CloudDeleteHelper.deleteEpisode(it, state, playbackManager, episodeManager, userEpisodeManager)
            }
            episodeAnalytics.trackBulkEvent(AnalyticsEvent.EPISODE_BULK_DOWNLOAD_DELETED, source, episodesToDelete.size)

            val snackText = resources.getStringPlural(episodesToDelete.size, LR.string.episodes_deleted_singular, LR.string.episodes_deleted_plural)
            showSnackBar(snackText)
            closeMultiSelect()
        }
        CloudDeleteHelper.getDeleteDialog(episodes, deleteState, deleteFunction, resources).show(fragmentManager, "delete_warning")
    }

    fun share(fragmentManager: FragmentManager) {

        val episode = selectedList.let { list ->
            if (list.size != 1) {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Can only share one episode, but trying to share ${selectedList.size} episodes when multi selecting")
                return
            } else {
                list.first()
            }
        }

        if (episode !is PodcastEpisode) {
            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Can only share a ${PodcastEpisode::class.java.simpleName}")
            Toast.makeText(context, LR.string.podcasts_share_failed, Toast.LENGTH_SHORT).show()
            return
        }

        launch {
            val podcast = podcastManager.findPodcastByUuidSuspend(episode.podcastUuid) ?: run {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "Share failed because unable to find podcast from uuid")
                return@launch
            }

            ShareDialog(
                episode = episode,
                podcast = podcast,
                fragmentManager = fragmentManager,
                context = context,
                shouldShowPodcast = false,
                analyticsTracker = analyticsTracker,
            ).show(sourceView = SourceView.MULTI_SELECT)
        }

        closeMultiSelect()
    }

    fun removeFromUpNext(resources: Resources) {
        val list = selectedList.toList()
        launch {
            list.forEach {
                playbackManager.upNextQueue.removeEpisode(it)
            }

            withContext(Dispatchers.Main) {
                val snackText = resources.getStringPlural(selectedList.size, LR.string.removed_from_up_next_singular, LR.string.removed_from_up_next_plural)
                showSnackBar(snackText)
            }
        }

        closeMultiSelect()
    }

    fun moveToTop() {
        val list = selectedList.toList()
        playbackManager.playEpisodesNext(episodes = list, source = source)
        closeMultiSelect()
    }

    fun moveToBottom() {
        val list = selectedList.toList()
        playbackManager.playEpisodesLast(episodes = list, source = source)
        closeMultiSelect()
    }
}
