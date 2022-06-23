package au.com.shiftyjelly.pocketcasts.utils

import java.util.Objects

class Optional<T> {

    companion object {
        fun <T> empty(): Optional<T> {
            return Optional()
        }

        fun <T> of(value: T): Optional<T> {
            return Optional(value)
        }
    }

    private var value: T? = null

    private constructor() {
        this.value = null
    }

    private constructor(value: T) {
        this.value = Objects.requireNonNull(value)
    }

    fun ifPresent(action: (T) -> Unit) {
        value?.let { action(it) }
    }

    fun isPresent(): Boolean {
        return value != null
    }

    fun get(): T? {
        return value
    }
}
