package au.com.shiftyjelly.pocketcasts.utils

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine as kotlinCombine

public fun <T1, T2, T3, T4, T5, T6, R> combine(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    flow4: Flow<T4>,
    flow5: Flow<T5>,
    flow6: Flow<T6>,
    transform: suspend (T1, T2, T3, T4, T5, T6) -> R,
): Flow<R> = kotlinCombine(flow1, flow2, flow3, flow4, flow5, flow6) { array ->
    @Suppress("UNCHECKED_CAST")
    transform(
        array[0] as T1,
        array[1] as T2,
        array[2] as T3,
        array[3] as T4,
        array[4] as T5,
        array[5] as T6,
    )
}
