package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.utils.FileUtilWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class StorageSettingsViewModel
@Inject constructor(
    private val fileStorage: FileStorage,
    private val fileUtil: FileUtilWrapper,
) : ViewModel() {
    private val mutableSnackbarMessage = MutableSharedFlow<Int>()
    val snackbarMessage = mutableSnackbarMessage.asSharedFlow()

    fun onClearDownloadCacheClick() {
        viewModelScope.launch {
            val tempPath = fileStorage.tempPodcastDirectory
            fileUtil.deleteDirectoryContents(tempPath.absolutePath)
            mutableSnackbarMessage.emit(LR.string.settings_storage_clear_cache)
        }
    }
}
