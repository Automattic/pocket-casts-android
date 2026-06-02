package au.com.shiftyjelly.pocketcasts

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class TvScaffoldViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(TvScaffoldUiState())
    val uiState: StateFlow<TvScaffoldUiState> = _uiState.asStateFlow()

    fun selectTab(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
    }
}

data class TvScaffoldUiState(
    val tabs: List<TvTab> = TvTab.entries,
    val selectedTabIndex: Int = 0,
)
