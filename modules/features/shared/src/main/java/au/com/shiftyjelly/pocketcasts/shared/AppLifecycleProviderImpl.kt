package au.com.shiftyjelly.pocketcasts.shared

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import au.com.shiftyjelly.pocketcasts.repositories.playback.AppLifecycleProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Singleton
class AppLifecycleProviderImpl @Inject constructor() :
    AppLifecycleProvider,
    DefaultLifecycleObserver {

    private val _isInForeground = MutableStateFlow(false)
    override val isInForeground: StateFlow<Boolean> = _isInForeground

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        _isInForeground.value = true
    }

    override fun onPause(owner: LifecycleOwner) {
        super.onPause(owner)
        _isInForeground.value = false
    }
}
