package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.PodcastFolder
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.views.helper.UiUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.rx2.asFlow
import java.util.Locale
import java.util.Optional
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class FolderEditViewModel
@Inject constructor(
    private val podcastManager: PodcastManager,
    private val folderManager: FolderManager,
    private val settings: Settings
) : ViewModel(), CoroutineScope {

    data class State(
        val podcastsWithFolders: List<PodcastFolder> = emptyList(),
        val folders: List<Folder> = emptyList(),
        val filteredPodcasts: List<PodcastFolder> = emptyList(),
        val folderUuidToPodcastCount: Map<String?, Int> = emptyMap(),
        val selectedUuids: List<String> = emptyList(),
        val searchText: String = "",
        val folder: Folder? = null,
        val layout: Int = Settings.PodcastGridLayoutType.LARGE_ARTWORK.id
    ) {
        fun isSelected(podcast: Podcast): Boolean {
            return selectedUuids.contains(podcast.uuid)
        }

        val selectedCount: Int
            get() = selectedUuids.size

        val isCreateFolder: Boolean
            get() = folder == null

        val isEditFolder: Boolean
            get() = folder != null
    }

    // folder podcast select page
    private val searchText = MutableStateFlow("")
    private val selectedUuids = MutableStateFlow(emptyList<String>())
    private val folderUuid = MutableStateFlow<Optional<String>>(Optional.empty())
    private val mutableState = MutableStateFlow(State())
    val state: StateFlow<State>
        get() = mutableState

    // folder name page
    val folderName = MutableStateFlow("")

    // folder color page
    val colorId = MutableStateFlow(0)

    init {
        viewModelScope.launch {
            combine(
                settings.podcastSortTypeObservable.toFlowable(BackpressureStrategy.LATEST)
                    .switchMap { podcastSortOrder ->
                        if (podcastSortOrder == PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST) {
                            podcastManager.observePodcastsOrderByLatestEpisode()
                        } else {
                            podcastManager.observeSubscribed()
                        }
                    }
                    .asFlow<List<Podcast>>(),
                searchText,
                selectedUuids,
                settings.selectPodcastSortTypeObservable.asFlow(),
                folderManager.findFoldersFlow().combine(folderUuid) { folders, uuidOptional ->
                    val foldersSorted = folders.sortedBy { it.name.lowercase(Locale.getDefault()) }
                    // find the current open folder
                    val uuid = uuidOptional.orElse(null)
                    val folder = if (uuid == null) null else folders.find { it.uuid == uuid }
                    Pair(foldersSorted, folder)
                },
            ) { podcasts, searchText, selectedUuids, sortOrder, foldersAndFolder ->
                val folders = foldersAndFolder.first
                val folder = foldersAndFolder.second
                val podcastsSelected = podcasts.filter { selectedUuids.contains(it.uuid) }
                val folderUuidToPodcastCount = mutableMapOf<String?, Int>()
                // show the podcast with its current folder
                val podcastsWithFolders = podcasts.map { podcast ->
                    val podcastFolder = podcast.folderUuid?.let { folderUuid ->
                        folders.firstOrNull { folder -> folder.uuid == folderUuid }
                    }
                    folderUuidToPodcastCount[podcastFolder?.uuid] = (folderUuidToPodcastCount[podcastFolder?.uuid] ?: 0) + 1
                    PodcastFolder(podcast = podcast, folder = podcastFolder)
                }

                State(
                    podcastsWithFolders = podcastsWithFolders,
                    folderUuidToPodcastCount = folderUuidToPodcastCount,
                    filteredPodcasts = filterSortPodcasts(
                        searchText = searchText,
                        sortType = sortOrder,
                        podcastsSortedByReleaseDate = podcastsWithFolders,
                        currentFolderUuid = folder?.uuid
                    ),
                    selectedUuids = sortPodcasts(podcastsSortedByReleaseDate = podcastsSelected).map { it.uuid },
                    searchText = searchText,
                    folders = folders,
                    folder = folder,
                    layout = settings.getPodcastsLayout()
                )
            }.collect {
                mutableState.value = it
            }
        }
    }

    private fun filterSortPodcasts(searchText: String, sortType: PodcastsSortType, podcastsSortedByReleaseDate: List<PodcastFolder>, currentFolderUuid: String?): List<PodcastFolder> {
        val filtered = PodcastFolderHelper.filter(searchText = searchText, list = podcastsSortedByReleaseDate)
        return PodcastFolderHelper.sortForSelectingPodcasts(sortType = sortType, podcastsSortedByReleaseDate = filtered, currentFolderUuid = currentFolderUuid)
    }

    private fun sortPodcasts(podcastsSortedByReleaseDate: List<Podcast>): List<Podcast> {
        val podcasts = podcastsSortedByReleaseDate
        return when (settings.getPodcastsSortType()) {
            PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST -> podcastsSortedByReleaseDate
            PodcastsSortType.DATE_ADDED_OLDEST_TO_NEWEST -> podcasts.sortedWith(compareBy { it.addedDate })
            PodcastsSortType.DRAG_DROP -> podcasts.sortedWith(compareBy { it.sortPosition })
            else -> podcasts.sortedWith(compareBy { PodcastsSortType.cleanStringForSort(it.title) })
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    fun addPodcast(uuid: String) {
        val uuids = selectedUuids.value.toMutableSet().apply {
            add(uuid)
        }
        selectedUuids.value = uuids.distinct()
    }

    fun removePodcast(uuid: String) {
        val uuids = selectedUuids.value.toMutableList().apply {
            remove(uuid)
        }
        selectedUuids.value = uuids.distinct()
    }

    fun searchPodcasts(text: String) {
        searchText.value = text
    }

    fun changeFolderName(name: String) {
        folderName.value = name.take(100)
    }

    fun changeColor(index: Int) {
        colorId.value = index
    }

    fun saveColor() {
        val folder = state.value.folder ?: return
        viewModelScope.launch {
            folderManager.updateColor(folderUuid = folder.uuid, color = colorId.value)
        }
    }

    fun saveFolderName(resources: Resources) {
        val folder = state.value.folder ?: return
        var name = folderName.value.trim()
        if (name.isEmpty()) {
            // set an empty folder name to 'New Folder'
            name = resources.getString(LR.string.new_folder_title)
        }
        viewModelScope.launch {
            folderManager.updateName(folderUuid = folder.uuid, name = name)
        }
    }

    fun saveFolder(resources: Resources, onComplete: (folder: Folder) -> Unit) {
        val state = state.value
        val folder = state.folder
        if (folder == null) {
            // create folder
            val podcastUuids = this@FolderEditViewModel.state.value.selectedUuids.toList()
            var name = folderName.value.trim()
            if (name.isEmpty()) {
                // set an empty folder name to 'New Folder'
                name = resources.getString(LR.string.new_folder_title)
            }
            viewModelScope.launch {
                val newFolder = folderManager.create(
                    name = name,
                    color = colorId.value,
                    podcastsSortType = settings.getPodcastsSortType(),
                    podcastUuids = podcastUuids
                )
                onComplete(newFolder)
            }
        } else {
            saveFolderPodcasts(onComplete = onComplete)
        }
    }

    fun saveFolderPodcasts(onComplete: (folder: Folder) -> Unit) {
        viewModelScope.launch {
            val folder = state.value.folder ?: return@launch
            val podcastUuids = this@FolderEditViewModel.state.value.selectedUuids.toList()
            folderManager.updatePodcasts(folderUuid = folder.uuid, podcastUuids = podcastUuids)
            onComplete(folder)
        }
    }

    fun deleteFolder(onComplete: () -> Unit) {
        val folder = state.value.folder ?: return
        viewModelScope.launch {
            folderManager.delete(folder)
            onComplete()
        }
    }

    fun setFolderUuid(uuid: String) {
        selectedUuids.value = emptyList()
        folderUuid.value = Optional.of(uuid)
        viewModelScope.launch {
            val folder = folderManager.findByUuid(uuid) ?: return@launch
            colorId.value = folder.color
            folderName.value = folder.name
            selectedUuids.value = podcastManager.findPodcastsInFolder(folderUuid = uuid).map { it.uuid }
        }
    }

    fun getGridImageWidthDp(layout: Int, context: Context): Int {
        return UiUtil.getGridImageWidthPx(smallArtwork = layout == Settings.PodcastGridLayoutType.SMALL_ARTWORK.id, context = context).pxToDp(context).toInt()
    }

    fun movePodcastToFolder(podcastUuid: String, folder: Folder) {
        viewModelScope.launch {
            podcastManager.updateFolderUuid(folderUuid = folder.uuid, listOf(podcastUuid))
            folderUuid.value = Optional.of(folder.uuid)
        }
    }

    fun removePodcastFromFolder(podcastUuid: String) {
        viewModelScope.launch {
            podcastManager.updateFolderUuid(folderUuid = null, listOf(podcastUuid))
            folderUuid.value = Optional.empty()
        }
    }

    fun loadFolderForPodcast(podcastUuid: String) {
        viewModelScope.launch {
            folderUuid.value = Optional.ofNullable(podcastManager.findPodcastByUuidSuspend(podcastUuid)?.folderUuid)
        }
    }
}
