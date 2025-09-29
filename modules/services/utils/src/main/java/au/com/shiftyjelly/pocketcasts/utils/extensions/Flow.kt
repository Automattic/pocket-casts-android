package au.com.shiftyjelly.pocketcasts.utils.extensions

import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.combine as kotlinCombine

inline fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    crossinline transform: suspend (T1, T2, T3, T4, T5, T6) -> R,
): Flow<R> = kotlinCombine(flow1, flow2, flow3, flow4, flow5, flow6) { args: Array<*> ->
    @Suppress("UNCHECKED_CAST")
    transform(
        args[0] as T1,
        args[1] as T2,
        args[2] as T3,
        args[3] as T4,
        args[4] as T5,
        args[5] as T6,
    )
}

inline fun <T1, T2, T3, T4, T5, T6, T7, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    crossinline transform: suspend (T1, T2, T3, T4, T5, T6, T7) -> R,
): Flow<R> = kotlinCombine(flow1, flow2, flow3, flow4, flow5, flow6, flow7) { array ->
    @Suppress("UNCHECKED_CAST")
    transform(
        array[0] as T1,
        array[1] as T2,
        array[2] as T3,
        array[3] as T4,
        array[4] as T5,
        array[5] as T6,
        array[6] as T7,
    )
}

inline fun <T1, T2, T3, T4, T5, T6, T7, T8, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    flow7: Flow<T7>,
    flow8: Flow<T8>,
    crossinline transform: suspend (T1, T2, T3, T4, T5, T6, T7, T8) -> R,
): Flow<R> = kotlinCombine(flow1, flow2, flow3, flow4, flow5, flow6, flow7, flow8) { array ->
    @Suppress("UNCHECKED_CAST")
    transform(
        array[0] as T1,
        array[1] as T2,
        array[2] as T3,
        array[3] as T4,
        array[4] as T5,
        array[5] as T6,
        array[6] as T7,
        array[7] as T8,
    )
}

fun <T> Flow<T>.windowed(size: Int) = flow {
    check(size > 0) { "Window size must be positive: $size" }
    val queue = ArrayDeque<T>(size)
    collect { item ->
        if (queue.size < size) {
            queue.addLast(item)
            if (queue.size == size) {
                emit(queue.toList())
            }
        } else {
            queue.removeFirst()
            queue.addLast(item)
            emit(queue.toList())
        }
    }
}

// Source: https://github.com/Kotlin/kotlinx.coroutines/issues/2631#issuecomment-2812699291
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
fun <T, R> StateFlow<T>.mapState(transform: (T) -> R): StateFlow<R> = object : StateFlow<R> {
    override val replayCache: List<R> get() = listOf(value)

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        var lastEmittedValue: Any? = nullSurrogate
        this@mapState.collect { newValue ->
            val transformedValue = transform(newValue)
            if (transformedValue != lastEmittedValue) {
                lastEmittedValue = transformedValue
                collector.emit(transformedValue)
            }
        }
    }

    private var lastUpstreamValue = this@mapState.value

    override var value: R = transform(lastUpstreamValue)
        private set
        get() {
            val currentUpstreamValue: T = this@mapState.value
            if (currentUpstreamValue == lastUpstreamValue) {
                return field
            }

            val newValue = transform(currentUpstreamValue)
            field = newValue
            lastUpstreamValue = currentUpstreamValue
            return newValue
        }
}

private val nullSurrogate = Any()
