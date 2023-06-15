package au.com.shiftyjelly.pocketcasts.shared

import android.content.Context
import au.com.shiftyjelly.pocketcasts.repositories.support.Support
import au.com.shiftyjelly.pocketcasts.shared.WatchPhoneCommunication.Companion.Paths.emailLogsToSupport
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WatchPhoneCommunication {

    companion object {
        private object Paths {
            private const val prefix = "/pocket_casts_wear_communication"
            const val emailLogsToSupport = "$prefix/email_support"
        }

        private const val capabilityName = "pocket_casts_wear_listener"
    }

    class Watch @Inject constructor(
        @ApplicationContext private val appContext: Context,
        private val support: Support,
    ) {

        private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())
        private val capabilityInfoFlow = MutableStateFlow<CapabilityInfo?>(null)
        private val availableNodeFlow = capabilityInfoFlow
            .map { it?.nodes?.firstOrNull() } // just using the first available node
            .stateIn(coroutineScope, SharingStarted.Lazily, null)
        val watchPhoneCommunicationStateFlow = availableNodeFlow
            .map {
                when (it) {
                    null -> WatchPhoneCommunicationState.NOT_CONNECTED
                    else -> WatchPhoneCommunicationState.AVAILABLE
                }
            }.stateIn(coroutineScope, SharingStarted.Lazily, WatchPhoneCommunicationState.NOT_CONNECTED)

        private val onCapabilityChangedListener = CapabilityClient.OnCapabilityChangedListener {
            capabilityInfoFlow.value = it
        }

        init {

            coroutineScope.launch {
                val capabilityInfo =
                    Wearable.getCapabilityClient(appContext)
                        .getCapability(capabilityName, CapabilityClient.FILTER_REACHABLE)
                        .await()
                onCapabilityChangedListener.onCapabilityChanged(capabilityInfo)
            }

            Wearable.getCapabilityClient(appContext)
                .addListener({
                    onCapabilityChangedListener.onCapabilityChanged(it)
                }, capabilityName)
        }

        suspend fun emailLogsToSupportMessage() {
            withContext(Dispatchers.IO) {

                val node = availableNodeFlow.value
                if (node == null) {
                    LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "failed to email logs to support because no nodes available")
                    return@withContext
                }

                val path = emailLogsToSupport
                val data = support.getLogs().toByteArray()
                Wearable
                    .getMessageClient(appContext)
                    .sendMessage(node.id, path, data)
            }
        }
    }

    class Phone @Inject constructor(
        @ApplicationContext appContext: Context,
    ) {

        fun handleMessage(messageEvent: MessageEvent) {
            when (messageEvent.path) {

                emailLogsToSupport -> handleEmailLogsToSupportMessage(messageEvent)

                else -> {
                    val message = "${this::class.java.simpleName} received message with unexpected path: ${messageEvent.path}"
                    throw RuntimeException(message)
                }
            }
        }

        private fun handleEmailLogsToSupportMessage(messageEvent: MessageEvent) {
            val logs = String(messageEvent.data)
            TODO("send email to support")
        }
    }
}

enum class WatchPhoneCommunicationState {
    AVAILABLE,
    NOT_CONNECTED,
}
