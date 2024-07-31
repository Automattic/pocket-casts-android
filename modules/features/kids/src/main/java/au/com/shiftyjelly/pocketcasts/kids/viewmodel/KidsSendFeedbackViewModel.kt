package au.com.shiftyjelly.pocketcasts.kids.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class KidsSendFeedbackViewModel @Inject constructor() : ViewModel() {
    private val _showFeedbackDialog = MutableStateFlow(false)
    val showFeedbackDialog: StateFlow<Boolean> = _showFeedbackDialog

    fun onSendFeedbackClick() {
        viewModelScope.launch {
            _showFeedbackDialog.value = true
        }
    }

    fun onNoThankYouClick() {
        viewModelScope.launch {
            _showFeedbackDialog.value = false
        }
    }
}
