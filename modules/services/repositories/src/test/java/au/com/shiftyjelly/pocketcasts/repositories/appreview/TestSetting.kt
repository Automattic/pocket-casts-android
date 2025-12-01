package au.com.shiftyjelly.pocketcasts.repositories.appreview

import au.com.shiftyjelly.pocketcasts.preferences.ReadWriteSetting
import java.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TestSetting<T>(
    initialValue: T,
) : ReadWriteSetting<T> {
    private val stateFlow = MutableStateFlow(initialValue)

    override val value: T
        get() = stateFlow.value

    override val flow: StateFlow<T>
        get() = stateFlow

    override fun set(value: T, updateModifiedAt: Boolean, commit: Boolean, clock: Clock) {
        stateFlow.value = value
    }

    fun set(value: T) = set(value, updateModifiedAt = false)
}
