package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.support.DatabaseExportHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class HelpViewModel @Inject constructor(
    private val databaseExportHelper: DatabaseExportHelper,
) : ViewModel() {
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    fun onExportDatabaseMenuItemClick(sendIntent: (File) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val exportFile = databaseExportHelper.getExportFile()
            exportFile?.let { sendIntent(it) }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    data class UiState(
        val isLoading: Boolean = false,
    )
}
