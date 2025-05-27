package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.ContextCompat.getSystemService
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
import kotlinx.coroutines.withContext

@HiltViewModel
class LogsViewModel @Inject constructor(
    private val support: Support,
) : ViewModel() {

    data class State(
        val logs: String?,
        val logLines: List<String>,
    )

    private val _state = MutableStateFlow(State(null, emptyList()))
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val logs = support.getLogs()
            val logLines = withContext(Dispatchers.Default) { logs.split('\n') }
            _state.update { it.copy(logs = logs, logLines = logLines) }
        }
    }

    fun shareLogs(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val intent = support.shareLogs(
                subject = "Android logs.",
                intro = "Please find my logs attached.",
                emailSupport = true,
                context = context,
            )
            context.startActivity(intent)
        }
    }

    fun copyToClipboard(context: Context, logs: String?) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Logs", logs.orEmpty()))
    }
}
