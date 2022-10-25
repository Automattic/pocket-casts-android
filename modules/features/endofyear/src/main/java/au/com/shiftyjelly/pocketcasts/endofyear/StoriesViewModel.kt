package au.com.shiftyjelly.pocketcasts.endofyear

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class StoriesViewModel
@Inject constructor() : ViewModel() {
    private val mutableState = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = mutableState

    sealed class State {
        object Loading : State()
        object Loaded : State()
        object Error : State()
    }
}
