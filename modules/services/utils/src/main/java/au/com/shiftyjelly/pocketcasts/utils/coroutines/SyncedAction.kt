package au.com.shiftyjelly.pocketcasts.utils.coroutines

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin

class SyncedAction<InputT, OutputT : Any>(
    private val action: suspend (InputT) -> OutputT,
) {
    @Volatile
    private var value: OutputT? = null

    @Volatile
    private var syncJob: Deferred<OutputT>? = null

    fun run(input: InputT, scope: CoroutineScope): Deferred<OutputT> {
        val cachedValue = value
        if (cachedValue != null) {
            return CompletableDeferred(cachedValue)
        }

        return syncJob ?: synchronized(this) {
            syncJob ?: scope.async {
                val newValue = action(input)
                value = newValue
                newValue
            }.also { newJob ->
                syncJob = newJob
                newJob.invokeOnCompletion { syncJob = null }
            }
        }
    }

    suspend fun reset() {
        syncJob?.cancelAndJoin()
        value = null
    }
}

fun <T : Any> SyncedAction<Unit, T>.run(scope: CoroutineScope) = run(input = Unit, scope)
