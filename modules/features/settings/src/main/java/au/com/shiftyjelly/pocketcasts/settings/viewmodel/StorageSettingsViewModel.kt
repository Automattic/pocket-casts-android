package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import android.os.StatFs
import androidx.annotation.IntegerRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.compose.components.DialogButtonState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.file.FolderLocation
import au.com.shiftyjelly.pocketcasts.utils.FileUtilWrapper
import au.com.shiftyjelly.pocketcasts.utils.Util
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class StorageSettingsViewModel
@Inject constructor(
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

    private val storageChoiceSummary: String?
        get() = if (settings.usingCustomFolderStorage()) {
            context.getString(LR.string.settings_storage_custom_folder)
        } else {
            settings.getStorageChoiceName()
        }

    private lateinit var foldersAvailable: List<FolderLocation>
    private lateinit var folderLocations: () -> List<FolderLocation>

    fun start(folderLocations: () -> List<FolderLocation>) {
        this.folderLocations = folderLocations
    }

    private fun initState() = State(
        storageDataWarningState = State.StorageDataWarningState(
            isChecked = settings.warnOnMeteredNetwork(),
            onCheckedChange = { onStorageDataWarningCheckedChange(it) }
        ),
        storageChoiceState = State.StorageChoiceState(
            title = settings.getStorageChoice(),
            summary = storageChoiceSummary
        )
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

    private fun setupStorage() {
        // find all the places the user might want to store their podcasts, but still give them a custom folder option
        foldersAvailable = folderLocations()
        var optionsCount = foldersAvailable.size
        if (android.os.Build.VERSION.SDK_INT < 29) {
            optionsCount++
        }

        val entries = arrayOfNulls<String>(optionsCount)
        val entryValues = arrayOfNulls<String>(optionsCount)
        var i = 0
        for (folderLocation in foldersAvailable) {
            entries[i] =
                folderLocation.label + ", " + getStorageSpaceString(folderLocation.filePath)
            entryValues[i] = folderLocation.filePath

            i++
        }
        if (android.os.Build.VERSION.SDK_INT < 29) {
            entries[i] = context.getString(LR.string.settings_storage_custom_folder) + "…"
            entryValues[i] = Settings.STORAGE_ON_CUSTOM_FOLDER
        }

        mutableState.value = mutableState.value.copy(
            storageChoiceState = mutableState.value.storageChoiceState.copy(
                choices = Pair(entries, entryValues)
            )
        )

        changeStorageLabels()
    }

    private fun changeStorageLabels() {
        mutableState.value = mutableState.value.copy(
            storageChoiceState = mutableState.value.storageChoiceState.copy(
                summary = storageChoiceSummary
            )
        )
    }

    private fun getStorageSpaceString(path: String): String {
        return try {
            val stat = StatFs(path)
            val free = stat.availableBlocksLong * stat.blockSizeLong
            context.getString(
                LR.string.settings_storage_size_free,
                Util.formattedBytes(free, context = context)
            )
        } catch (e: Exception) {
            ""
        }
    }

    private fun createAlertDialogState(
        @IntegerRes title: String,
        @IntegerRes message: Int? = null,
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
                onClick = {},
            )
        ),
        onDismissRequest = {}
    )

    data class State(
        val storageDataWarningState: StorageDataWarningState,
        val storageChoiceState: StorageChoiceState,
    ) {
        data class StorageDataWarningState(
            val isChecked: Boolean = false,
            val onCheckedChange: (Boolean) -> Unit,
        )

        data class StorageChoiceState(
            val title: String? = null,
            val summary: String? = null,
            val choices: Pair<Array<String?>, Array<String?>>? = null,
        )
    }

    data class AlertDialogState(
        val title: String,
        val message: String? = null,
        val buttons: List<DialogButtonState>,
        val onDismissRequest: (() -> Unit)? = null,
    )
}
