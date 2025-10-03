package au.com.shiftyjelly.pocketcasts.utils.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class SyncedAction<InputType, OutputType>(
    private val action: suspend (InputType) -> OutputType,
) {
    private val executeMutex = Mutex()
    private var executeCount = 0
    private var inFlightResult: Deferred<OutputType>? = null

    suspend fun run(input: InputType, scope: CoroutineScope): OutputType {
        val deferred = executeMutex.withLock {
            val inFlight = inFlightResult ?: scope.async(start = CoroutineStart.LAZY) { action(input) }
            inFlightResult = inFlight
            executeCount++
            inFlight
        }

        return try {
            deferred.await()
        } finally {
            executeMutex.withLock {
                executeCount--
                if (executeCount == 0) {
                    inFlightResult = null
                }
            }
        }
    }
}

suspend fun <T> SyncedAction<Unit, T>.run(scope: CoroutineScope) = run(input = Unit, scope)
