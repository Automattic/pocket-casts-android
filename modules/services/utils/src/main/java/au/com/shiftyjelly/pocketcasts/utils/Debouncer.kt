package au.com.shiftyjelly.pocketcasts.utils

import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class Debouncer(
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val waitDuration: Duration = (1).toDuration(DurationUnit.SECONDS),
) {
    class Work(val work: suspend () -> Unit)
    private val stateFlow = MutableStateFlow<Work?>(null)
    init {
        coroutineScope.launch {
            stateFlow.debounce(waitDuration).collect {
                it?.let { it.work() }
            }
        }
    }

    suspend fun debounce(work: suspend () -> Unit) {
        stateFlow.emit(Work(work))
    }
}
