package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.support.Support
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val support: Support,
) : ViewModel() {

    data class State(
        val logs: String?,
    )

    private val _state = MutableStateFlow(State(null))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.update {
                val logs = support.getLogs()
                it.copy(logs = logs)
            }
        }
    }

    fun shareLogs(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val intent = support.shareLogs(
                subject = context.getString(LR.string.settings_logs),
                intro = "",
                emailSupport = false,
                context = context,
            )
            context.startActivity(intent)
        }
    }
}
