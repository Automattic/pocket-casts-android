package au.com.shiftyjelly.pocketcasts.settings.history.upnext

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextHistoryEntry
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.history.upnext.UpNextHistoryManager
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class UpNextHistoryViewModel @Inject constructor(
    private val upNextHistoryManager: UpNextHistoryManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _navigationState: MutableSharedFlow<NavigationState> = MutableSharedFlow()
    val navigationState = _navigationState.asSharedFlow()

    init {
        loadHistoryEntries()
    }

    private fun loadHistoryEntries() {
        viewModelScope.launch(ioDispatcher) {
            try {
                val entries = upNextHistoryManager.findAllHistoryEntries()
                _uiState.update { state ->
                    UiState.Loaded(
                        entries = entries,
                    )
                }
            } catch (e: Exception) {
                val message = "Failed to load UpNext history entries"
                Timber.e(e, message)
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, e, message)
                _uiState.update { state -> UiState.Error }
            }
        }
    }

    fun onHistoryEntryClick(entry: UpNextHistoryEntry) {
        viewModelScope.launch {
            _navigationState.emit(NavigationState.ShowHistoryDetails(entry.date))
        }
    }

    sealed class UiState {
        data object Loading : UiState()
        data class Loaded(
            val entries: List<UpNextHistoryEntry>,
        ) : UiState()

        data object Error : UiState()
    }

    sealed class NavigationState {
        data class ShowHistoryDetails(val date: Date) : NavigationState()
    }
}
