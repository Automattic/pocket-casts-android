package au.com.shiftyjelly.pocketcasts.coroutines.flow

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

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
