package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.Settings.PodcastGridLayoutType
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import com.jakewharton.rxrelay2.BehaviorRelay
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Flowable.combineLatest
import io.reactivex.rxkotlin.Observables
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Collections
import java.util.Optional
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@HiltViewModel
class PodcastsViewModel
@Inject constructor(
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val folderManager: FolderManager,
    private val settings: Settings,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    userManager: UserManager
) : ViewModel(), CoroutineScope {
    var isFragmentChangingConfigurations: Boolean = false
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val folderUuidObservable = BehaviorRelay.create<Optional<String>>().apply { accept(Optional.empty()) }

    data class FolderState(
        val folder: Folder?,
        val items: List<FolderItem>,
        val isSignedInAsPlus: Boolean
    )

    val signInState: LiveData<SignInState> = userManager.getSignInState().toLiveData()

    val folderState: LiveData<FolderState> =
        combineLatest(
            // monitor all subscribed podcasts, get the podcast in 'Episode release date' as the rest can be done in memory
            podcastManager.observePodcastsOrderByLatestEpisode(),
            // monitor all the folders
            folderManager.observeFolders()
                .switchMap { folders ->
                    if (folders.isEmpty()) {
                        Flowable.just(emptyList())
                    } else {
                        // monitor the folder podcasts
                        val observeFolderPodcasts = folders.map { folder ->
                            podcastManager
                                .observePodcastsInFolderOrderByUserChoice(folder)
                                .map { podcasts ->
                                    FolderItem.Folder(
                                        folder = folder,
                                        podcasts = podcasts
                                    )
                                }
                        }
                        Flowable.zip(observeFolderPodcasts) { results ->
                            results.toList().filterIsInstance<FolderItem.Folder>()
                        }
                    }
                },
            // monitor the folder uuid
            folderUuidObservable.toFlowable(BackpressureStrategy.LATEST),
            // monitor the home folder sort order
            settings.podcastSortTypeObservable.toFlowable(BackpressureStrategy.LATEST),
            // show folders for Plus users
            userManager.getSignInState()
        ) { podcasts, folders, folderUuidOptional, podcastSortOrder, signInState ->
            val folderUuid = folderUuidOptional.orElse(null)
            if (!signInState.isSignedInAsPlus) {
                FolderState(
                    items = buildPodcastItems(podcasts, podcastSortOrder),
                    folder = null,
                    isSignedInAsPlus = false
                )
            } else if (folderUuid == null) {
                FolderState(
                    items = buildHomeFolderItems(podcasts, folders, podcastSortOrder),
                    folder = null,
                    isSignedInAsPlus = true
                )
            } else {
                val openFolder = folders.firstOrNull { it.uuid == folderUuid }
                if (openFolder == null) {
                    FolderState(
                        items = emptyList(),
                        folder = null,
                        isSignedInAsPlus = true
                    )
                } else {
                    FolderState(
                        items = openFolder.podcasts.map { FolderItem.Podcast(it) },
                        folder = openFolder.folder,
                        isSignedInAsPlus = true
                    )
                }
            }
        }
            .doOnNext { adapterState = it.items.toMutableList() }
            .toLiveData()

    val folder: Folder?
        get() = folderState.value?.folder

    private fun buildHomeFolderItems(podcasts: List<Podcast>, folders: List<FolderItem>, podcastSortType: PodcastsSortType): List<FolderItem> {
        if (podcastSortType == PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST) {
            val items = mutableListOf<FolderItem>()
            val uuidToFolder = folders.associateBy({ it.uuid }, { it }).toMutableMap()
            for (podcast in podcasts) {
                if (podcast.folderUuid == null) {
                    items.add(FolderItem.Podcast(podcast))
                } else {
                    // add the folder in the position of the podcast with the latest release date
                    val folder = uuidToFolder.remove(podcast.folderUuid)
                    if (folder != null) {
                        items.add(folder)
                    }
                }
            }
            if (uuidToFolder.isNotEmpty()) {
                items.addAll(uuidToFolder.values)
            }
            return items
        } else {
            val folderUuids = folders.map { it.uuid }.toHashSet()
            val items = podcasts
                // add the podcasts not in a folder or if the folder doesn't exist
                .filter { podcast -> podcast.folderUuid == null || !folderUuids.contains(podcast.folderUuid) }
                .map { FolderItem.Podcast(it) }
                .toMutableList<FolderItem>()
                // add the folders
                .apply { addAll(folders) }

            return items.sortedWith(podcastSortType.folderComparator)
        }
    }

    private fun buildPodcastItems(podcasts: List<Podcast>, podcastSortType: PodcastsSortType): List<FolderItem> {
        val items = podcasts.map { podcast -> FolderItem.Podcast(podcast) }
        return when (podcastSortType) {
            PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST -> items
            else -> items.sortedWith(podcastSortType.folderComparator)
        }
    }

    val podcastUuidToBadge: LiveData<Map<String, Int>> =
        settings.podcastBadgeTypeObservable
            .toFlowable(BackpressureStrategy.LATEST)
            .switchMap { badgeType ->
                return@switchMap when (badgeType) {
                    Settings.BadgeType.ALL_UNFINISHED -> episodeManager.getPodcastUuidToBadgeUnfinished()
                    Settings.BadgeType.LATEST_EPISODE -> episodeManager.getPodcastUuidToBadgeLatest()
                    else -> Flowable.just(emptyMap())
                }
            }
            .toLiveData()

    // We only want the current badge type when loading for this observable or else it will rebind the adapter every time the badge changes. We use take(1) for this.
    val layoutChangedLiveData = Observables.combineLatest(settings.podcastLayoutObservable, settings.podcastBadgeTypeObservable.take(1)).toFlowable(BackpressureStrategy.LATEST).toLiveData()

    val refreshObservable: LiveData<RefreshState> = settings.refreshStateObservable.toFlowable(BackpressureStrategy.LATEST).toLiveData()

    private var adapterState: MutableList<FolderItem> = mutableListOf()

    /**
     * User rearranges the grid with a drag
     */
    fun moveFolderItem(fromPosition: Int, toPosition: Int): List<FolderItem> {
        if (adapterState.isEmpty()) {
            return adapterState
        }

        try {
            if (fromPosition < toPosition) {
                for (index in fromPosition until toPosition) {
                    Collections.swap(adapterState, index, index + 1)
                }
            } else {
                for (index in fromPosition downTo toPosition + 1) {
                    Collections.swap(adapterState, index, index - 1)
                }
            }
        } catch (ex: IndexOutOfBoundsException) {
            Timber.e("Move folder item failed.", ex)
        }

        return adapterState.toList()
    }

    fun commitMoves() {
        launch {
            saveSortOrder()
        }
    }

    fun refreshPodcasts() {
        analyticsTracker.track(
            AnalyticsEvent.PULLED_TO_REFRESH,
            mapOf("source" to "podcasts_list")
        )
        podcastManager.refreshPodcasts("Pull down")
    }

    fun setFolderUuid(folderUuid: String?) {
        folderUuidObservable.accept(Optional.ofNullable(folderUuid))
    }

    fun isFolderOpen(): Boolean {
        return folderUuidObservable.value?.isPresent ?: false
    }

    private suspend fun saveSortOrder() {
        folderManager.updateSortPosition(adapterState)

        val folder = folder
        if (folder == null) {
            settings.setPodcastsSortType(sortType = PodcastsSortType.DRAG_DROP, sync = true)
        } else {
            folderManager.updateSortType(folderUuid = folder.uuid, podcastsSortType = PodcastsSortType.DRAG_DROP)
        }
    }

    fun updateFolderSort(uuid: String, podcastsSortType: PodcastsSortType) {
        launch {
            folderManager.updateSortType(folderUuid = uuid, podcastsSortType = podcastsSortType)
        }
    }

    fun onFragmentPause(isChangingConfigurations: Boolean?) {
        isFragmentChangingConfigurations = isChangingConfigurations ?: false
    }

    fun trackPodcastsListShown() {
        launch {
            val properties = HashMap<String, Any>()
            properties[NUMBER_OF_FOLDERS_KEY] = folderManager.countFolders()
            properties[NUMBER_OF_PODCASTS_KEY] = podcastManager.countSubscribed()
            properties[BADGE_TYPE_KEY] = settings.getPodcastBadgeType().analyticsValue
            properties[LAYOUT_KEY] = PodcastGridLayoutType.fromLayoutId(settings.getPodcastsLayout()).analyticsValue
            properties[SORT_ORDER_KEY] = settings.getPodcastsSortType().analyticsValue
            analyticsTracker.track(AnalyticsEvent.PODCASTS_LIST_SHOWN, properties)
        }
    }

    fun trackFolderShown(folderUuid: String) {
        launch {
            val properties = HashMap<String, Any>()
            properties[SORT_ORDER_KEY] = (folderManager.findByUuid(folderUuid)?.podcastsSortType ?: PodcastsSortType.DATE_ADDED_OLDEST_TO_NEWEST).analyticsValue
            properties[NUMBER_OF_PODCASTS_KEY] = folderManager.findFolderPodcastsSorted(folderUuid).size
            analyticsTracker.track(AnalyticsEvent.FOLDER_SHOWN, properties)
        }
    }

    companion object {
        private const val NUMBER_OF_FOLDERS_KEY = "number_of_folders"
        private const val NUMBER_OF_PODCASTS_KEY = "number_of_podcasts"
        private const val BADGE_TYPE_KEY = "badge_type"
        private const val LAYOUT_KEY = "layout"
        private const val SORT_ORDER_KEY = "sort_order"
    }
}
