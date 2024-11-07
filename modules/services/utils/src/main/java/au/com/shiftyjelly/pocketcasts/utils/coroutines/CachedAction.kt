package au.com.shiftyjelly.pocketcasts.utils.coroutines

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin

class CachedAction<InputT, OutputT>(
    private val action: suspend (InputT) -> OutputT,
) {
    @Volatile
    private var value: Any? = UNSYNCED_TOKEN

    @Volatile
    private var syncJob: Deferred<OutputT>? = null

    fun run(input: InputT, scope: CoroutineScope): Deferred<OutputT> {
        val cachedValue = value
        if (cachedValue !== UNSYNCED_TOKEN) {
            @Suppress("UNCHECKED_CAST")
            return CompletableDeferred(cachedValue as OutputT)
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
        value = UNSYNCED_TOKEN
    }

    private companion object {
        val UNSYNCED_TOKEN = Any()
    }
}

fun <T> CachedAction<Unit, T>.run(scope: CoroutineScope) = run(input = Unit, scope)
