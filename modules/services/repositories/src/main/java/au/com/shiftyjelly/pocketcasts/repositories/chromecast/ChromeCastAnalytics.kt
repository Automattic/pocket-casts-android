package au.com.shiftyjelly.pocketcasts.repositories.chromecast

import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import com.automattic.eventhorizon.ChromecastViewShownEvent
import com.automattic.eventhorizon.EventHorizon
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class ChromeCastAnalytics @Inject constructor(
    private val eventHorizon: EventHorizon,
    private val castManager: CastManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    fun trackChromeCastViewShown() {
        applicationScope.launch {
            val isConnected = castManager.isConnected()
            eventHorizon.track(
                ChromecastViewShownEvent(
                    isConnected = isConnected,
                ),
            )
        }
    }
}
