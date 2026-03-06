package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.UpNextSortType
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.UpNextSortEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow

@HiltViewModel
class UpNextViewModel @Inject constructor(
    private val userManager: UserManager,
    private val upNextQueue: UpNextQueue,
    private val eventHorizon: EventHorizon,
) : ViewModel() {
    private val _isSignedInAsPaidUser = MutableStateFlow(false)
    val isSignedInAsPaidUser: StateFlow<Boolean> get() = _isSignedInAsPaidUser

    init {
        viewModelScope.launch {
            userManager.getSignInState().asFlow().collect { signInState ->
                _isSignedInAsPaidUser.value = signInState.isSignedInAsPlusOrPatron
            }
        }
    }

    fun sortUpNext(sortType: UpNextSortType) {
        eventHorizon.track(
            UpNextSortEvent(
                sortType = sortType.eventHorizonValue,
            ),
        )
        upNextQueue.sortUpNext(sortType)
    }
}
