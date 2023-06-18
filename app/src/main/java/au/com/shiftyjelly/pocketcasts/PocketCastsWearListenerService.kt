package au.com.shiftyjelly.pocketcasts

import au.com.shiftyjelly.pocketcasts.shared.WatchPhoneCommunication
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PocketCastsWearListenerService : WearableListenerService() {

    @Inject lateinit var watchPhoneCommunication: WatchPhoneCommunication.Phone

    override fun onMessageReceived(event: MessageEvent) {
        watchPhoneCommunication.handleMessage(event)
        super.onMessageReceived(event)
    }
}
