package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.compose.components.DialogButtonState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.file.FolderLocation
import au.com.shiftyjelly.pocketcasts.repositories.file.StorageException
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtilWrapper
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.collect
import java.io.File
import java.util.*
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class StorageSettingsViewModel
@Inject constructor(
    private val podcastManager: PodcastManager,
    private val episodeManager: EpisodeManager,
    private val fileStorage: FileStorage,
    private val fileUtil: FileUtilWrapper,
    private val settings: Settings,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val mutableState = MutableStateFlow(initState())
    val state: StateFlow<State> = mutableState

    private val mutableSnackbarMessage = MutableSharedFlow<Int>()
    val snackbarMessage = mutableSnackbarMessage.asSharedFlow()

    private val mutableAlertDialog = MutableSharedFlow<AlertDialogState>()
    val alertDialog = mutableAlertDialog.asSharedFlow()

    private val mutableProgressDialog = MutableSharedFlow<Boolean>()
    val progressDialog = mutableProgressDialog.asSharedFlow()

    private val mutablePermissionRequest = MutableSharedFlow<String>()
    val permissionRequest = mutablePermissionRequest.asSharedFlow()

    private val backgroundRefreshSummary: Int
        get() = if (settings.refreshPodcastsAutomatically()) {
            LR.string.settings_storage_background_refresh_on
        } else {
            LR.string.settings_storage_background_refresh_off
        }

    private val storageChoiceSummary: String?
        get() = if (settings.usingCustomFolderStorage()) {
            context.getString(LR.string.settings_storage_custom_folder)
        } else {
            settings.getStorageChoiceName()
        }

    private val storageFolderSummary: String
        get() = if (settings.usingCustomFolderStorage()) {
            settings.getStorageCustomFolder()
        } else {
            context.getString(LR.string.settings_storage_using, settings.getStorageChoiceName())
        }

    private lateinit var foldersAvailable: List<FolderLocation>
    private lateinit var folderLocations: () -> List<FolderLocation>
    private lateinit var permissionGranted: () -> Boolean
    private var permissionRequestedForPath: String? = null
    private var sdkVersion: Int = 0

    fun start(
        folderLocations: () -> List<FolderLocation>,
        permissionGranted: () -> Boolean,
        sdkVersion: Int = Build.VERSION.SDK_INT,
    ) {
        this.folderLocations = folderLocations
        this.permissionGranted = permissionGranted
        this.sdkVersion = sdkVersion
        viewModelScope.launch {
            episodeManager.observeDownloadedEpisodes()
                .collect { downloadedEpisodes ->
                    val downloadSize = downloadedEpisodes.sumOf { it.sizeInBytes }
                    mutableState.value = mutableState.value.copy(
                        downloadedFilesState = mutableState.value.downloadedFilesState.copy(size = downloadSize)
                    )
                }
        }
    }

    private fun initState() = State(
        downloadedFilesState = State.DownloadedFilesState(),
        storageChoiceState = State.StorageChoiceState(
            title = settings.getStorageChoice(),
            summary = storageChoiceSummary,
            onStateChange = { onStorageChoiceChange(it) }
        ),
        storageFolderState = State.StorageFolderState(
            isVisible = settings.usingCustomFolderStorage(),
            summary = storageFolderSummary,
            onStateChange = { onStorageFolderChange(it) }
        ),
        backgroundRefreshState = State.BackgroundRefreshState(
            summary = backgroundRefreshSummary,
            isChecked = settings.refreshPodcastsAutomatically(),
            onCheckedChange = { onBackgroundRefreshCheckedChange(it) }
        ),
        storageDataWarningState = State.StorageDataWarningState(
            isChecked = settings.warnOnMeteredNetwork(),
            onCheckedChange = { onStorageDataWarningCheckedChange(it) }
        ),
    )

    fun onFragmentResume() {
        setupStorage()
    }

    fun onClearDownloadCacheClick() {
        viewModelScope.launch {
            val tempPath = fileStorage.tempPodcastDirectory
            fileUtil.deleteDirectoryContents(tempPath.absolutePath)
            mutableSnackbarMessage.emit(LR.string.settings_storage_clear_cache)
        }
    }

    private fun onStorageDataWarningCheckedChange(isChecked: Boolean) {
        settings.setWarnOnMeteredNetwork(isChecked)
        updateMobileDataWarningState()
    }

    private fun updateMobileDataWarningState() {
        mutableState.value = mutableState.value.copy(
            storageDataWarningState = mutableState.value.storageDataWarningState.copy(
                isChecked = settings.warnOnMeteredNetwork(),
            )
        )
    }

    private fun onBackgroundRefreshCheckedChange(isChecked: Boolean) {
        settings.setRefreshPodcastsAutomatically(isChecked)
        updateBackgroundRefreshState()
    }

    private fun updateBackgroundRefreshState() {
        mutableState.value = mutableState.value.copy(
            backgroundRefreshState = mutableState.value.backgroundRefreshState.copy(
                isChecked = settings.refreshPodcastsAutomatically(),
                summary = backgroundRefreshSummary
            )
        )
    }

    private fun setupStorage() {
        /* Find all the places the user might want to store their podcasts, but still give them a custom folder option on sdk version < 29 */
        foldersAvailable = folderLocations()
        var optionsCount = foldersAvailable.size
        if (sdkVersion < 29) {
            optionsCount++
        }

        val entries = arrayOfNulls<String>(optionsCount)
        val entryValues = arrayOfNulls<String>(optionsCount)
        foldersAvailable.mapIndexed { index, folderLocation ->
            entries[index] = folderLocation.label
            entryValues[index] = folderLocation.filePath
        }

        if (sdkVersion < 29) {
            entries[foldersAvailable.size] = context.getString(LR.string.settings_storage_custom_folder)
            entryValues[foldersAvailable.size] = Settings.STORAGE_ON_CUSTOM_FOLDER
        }

        mutableState.value = mutableState.value.copy(
            storageChoiceState = mutableState.value.storageChoiceState.copy(
                choices = Pair(entries, entryValues)
            )
        )

        updateStorageLabels()
    }

    private fun onStorageChoiceChange(folderPathChosen: String?) {
        if (folderPathChosen == Settings.STORAGE_ON_CUSTOM_FOLDER) {
            try {
                val baseDirectory = fileStorage.baseStorageDirectory
                baseDirectory?.absolutePath?.let { basePath ->
                    settings.setStorageCustomFolder(basePath)
                    updateStorageLabels()
                }
            } catch (e: StorageException) {
                viewModelScope.launch {
                    mutableAlertDialog.emit(
                        createAlertDialogState(
                            title = context.getString(LR.string.settings_storage_folder_change_failed) + " " + e.message,
                            message = LR.string.settings_storage_android_10_custom,
                        )
                    )
                    return@launch
                }
            }
        } else {
            // store the old folder value, this is still available until we set it below
            val oldFolderValue =
                if (settings.usingCustomFolderStorage()) settings.getStorageCustomFolder() else settings.getStorageChoice()

            // set the name for this folder
            for (folder in foldersAvailable) {
                if (folder.filePath == folderPathChosen) {
                    settings.setStorageChoice(folderPathChosen, folder.label)
                    updateStorageLabels()
                    break
                }
            }

            // if it's a new folder, ask the user if they want to move their files there
            movePodcastStorage(oldFolderValue, folderPathChosen)
        }

        if (sdkVersion >= 29 && settings.usingCustomFolderStorage()) {
            val (_, folderPaths) = mutableState.value.storageChoiceState.choices
            mutableState.value = mutableState.value.copy(
                storageChoiceState = mutableState.value.storageChoiceState.copy(
                    summary = folderPaths.firstOrNull() ?: ""
                )
            )
            viewModelScope.launch {
                mutableAlertDialog.emit(
                    createAlertDialogState(
                        title = context.getString(LR.string.settings_storage_folder_write_failed),
                        message = LR.string.settings_storage_android_10_custom,
                    )
                )
            }
        }
    }

    private fun onStorageFolderChange(
        newPath: String?,
    ) {
        viewModelScope.launch {
            if (newPath == null) {
                return@launch
            }

            // validate the path
            if (newPath.trim().isEmpty()) {
                mutableAlertDialog.emit(createAlertDialogState(context.getString(LR.string.settings_storage_folder_blank)))
                return@launch
            }

            var oldDirectory: File? = null
            try {
                oldDirectory = fileStorage.baseStorageDirectory
            } catch (e: StorageException) {
                // ignore error
            }

            if (!permissionGranted()) {
                mutablePermissionRequest.emit(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                permissionRequestedForPath = newPath
                return@launch
            }

            val newDirectory = File(newPath)
            if (!newDirectory.exists()) {
                val success = newDirectory.mkdirs()
                if (!success && !newDirectory.exists()) {
                    mutableAlertDialog.emit(createAlertDialogState(context.getString(LR.string.settings_storage_folder_not_found)))
                    return@launch
                }
            }

            if (!newDirectory.canWrite()) {
                mutableAlertDialog.emit(createAlertDialogState(context.getString(LR.string.settings_storage_folder_write_failed)))
                return@launch
            }

            // move the podcasts if the user wants
            if (oldDirectory != null) {
                movePodcastStorage(oldDirectory.absolutePath, newDirectory.absolutePath)
            }

            settings.setStorageCustomFolder(newPath)
            updateStorageLabels()
        }
    }

    private fun movePodcastStorage(oldDirectory: String?, newDirectory: String?) {
        if (oldDirectory == null || newDirectory != oldDirectory) {
            viewModelScope.launch {
                mutableAlertDialog.emit(
                    createStorageMoveLocationAlertDialogState(oldDirectory, newDirectory)
                )
            }
        }
    }

    private fun movePodcasts(oldDirectory: String?, newDirectory: String?) {
        if (oldDirectory == null || newDirectory == null) {
            return
        }
        LogBuffer.i(
            LogBuffer.TAG_BACKGROUND_TASKS,
            "Moving storage from $oldDirectory to $newDirectory"
        )
        viewModelScope.launch(Dispatchers.IO) {
            mutableProgressDialog.emit(true)
            fileStorage.moveStorage(
                File(oldDirectory),
                File(newDirectory),
                podcastManager,
                episodeManager
            )
            mutableProgressDialog.emit(false)
        }
        setupStorage()
    }

    private fun updateStorageLabels() {
        mutableState.value = mutableState.value.copy(
            storageChoiceState = mutableState.value.storageChoiceState.copy(
                summary = storageChoiceSummary
            ),
            storageFolderState = mutableState.value.storageFolderState.copy(
                isVisible = settings.usingCustomFolderStorage(),
                summary = storageFolderSummary
            )
        )
    }

    fun onPermissionGrantedStorage() {
        val path = permissionRequestedForPath
        if (path?.isNotBlank() == true) {
            onStorageFolderChange(path)
        }
    }

    private fun createStorageMoveLocationAlertDialogState(
        oldDirectory: String?,
        newDirectory: String?,
    ) = AlertDialogState(
        title = context.getString(LR.string.settings_storage_move_are_you_sure),
        message = context.getString(LR.string.settings_storage_move_message),
        buttons = listOf(
            DialogButtonState(
                text = context.getString(LR.string.settings_storage_move_cancel).uppercase(
                    Locale.getDefault()
                ),
                onClick = {}

            ),
            DialogButtonState(
                text = context.getString(LR.string.settings_storage_move),
                onClick = { movePodcasts(oldDirectory, newDirectory) },
            )
        )
    )

    private fun createAlertDialogState(
        title: String,
        @StringRes message: Int? = null,
    ) = AlertDialogState(
        title = title,
        message = message?.let { context.getString(message) },
        buttons = listOf(
            DialogButtonState(
                text = context.getString(LR.string.cancel).uppercase(
                    Locale.getDefault()
                ),
                onClick = {}
            ),
            DialogButtonState(
                text = context.getString(LR.string.ok),
                onClick = {}
            )
        )
    )

    data class State(
        val downloadedFilesState: DownloadedFilesState,
        val storageChoiceState: StorageChoiceState,
        val storageFolderState: StorageFolderState,
        val backgroundRefreshState: BackgroundRefreshState,
        val storageDataWarningState: StorageDataWarningState,
    ) {
        data class DownloadedFilesState(
            val size: Long = 0L,
        )

        data class StorageChoiceState(
            val title: String? = null,
            val summary: String? = null,
            val choices: Pair<Array<String?>, Array<String?>> = Pair(emptyArray(), emptyArray()),
            val onStateChange: (String?) -> Unit
        )

        data class StorageFolderState(
            val isVisible: Boolean = false,
            val summary: String? = null,
            val onStateChange: (newPath: String?) -> Unit
        )

        data class BackgroundRefreshState(
            @StringRes val summary: Int,
            val isChecked: Boolean = true,
            val onCheckedChange: (Boolean) -> Unit,
        )

        data class StorageDataWarningState(
            val isChecked: Boolean = false,
            val onCheckedChange: (Boolean) -> Unit,
        )
    }

    data class AlertDialogState(
        val title: String,
        val message: String? = null,
        val buttons: List<DialogButtonState>,
    )
}
