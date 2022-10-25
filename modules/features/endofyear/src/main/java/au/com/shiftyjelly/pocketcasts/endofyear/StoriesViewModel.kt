package au.com.shiftyjelly.pocketcasts.endofyear

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.endofyear.stories.Story
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class StoriesViewModel @Inject constructor(
    storiesDataSource: StoriesDataSource,
) : ViewModel() {
    private val mutableState = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = mutableState

    private var currentIndex: Int = 0

    init {
        val stories = storiesDataSource.loadStories()
        val state = if (stories.isEmpty()) {
            State.Error
        } else {
            State.Loaded(currentStory = storiesDataSource.storyAt(currentIndex))
        }
        mutableState.value = state
    }

    sealed class State {
        object Loading : State()
        data class Loaded(
            val currentStory: Story?,
        ) : State()

        object Error : State()
    }
}
