package au.com.shiftyjelly.pocketcasts.voicecontrol.foreground

import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.playback.AppLifecycleProvider
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals.GracePeriodSignal
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Singleton
class ForegroundStateMonitor @Inject constructor(
    private val appLifecycleProvider: AppLifecycleProvider,
    @ApplicationScope private val scope: CoroutineScope,
    private val gracePeriodSignal: GracePeriodSignal,
) {
    val isInForeground: StateFlow<Boolean> = appLifecycleProvider.isInForeground
        .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launch {
            var wasInForeground = isInForeground.value
            appLifecycleProvider.isInForeground.collect { inForeground ->
                if (wasInForeground && !inForeground) {
                    gracePeriodSignal.onAppBackgrounded()
                }
                wasInForeground = inForeground
            }
        }
    }
}
