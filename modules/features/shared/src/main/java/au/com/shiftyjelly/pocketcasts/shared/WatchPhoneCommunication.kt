package au.com.shiftyjelly.pocketcasts.shared

import android.content.Context
import android.content.Intent
import au.com.shiftyjelly.pocketcasts.repositories.support.Support
import au.com.shiftyjelly.pocketcasts.shared.WatchPhoneCommunication.Companion.Paths.emailLogsToSupport
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.CapabilityInfo
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
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
import timber.log.Timber
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

        suspend fun emailLogsToSupportMessage(): WatchMessageSendState {
            return withContext(Dispatchers.IO) {
                withAvailableNode { node ->
                    val path = emailLogsToSupport
                    val data = support.getLogs().toByteArray()
                    try {
                        Wearable
                            .getMessageClient(appContext)
                            .sendMessage(node.id, path, data)
                            .await()
                        WatchMessageSendState.QUEUED
                    } catch (e: Exception) {
                        Timber.e(e)
                        WatchMessageSendState.FAILED_TO_QUEUE
                    }
                } ?: WatchMessageSendState.FAILED_TO_QUEUE
            }
        }

        private suspend fun <T> withAvailableNode(continuation: suspend (node: Node) -> T): T? {
            val node = availableNodeFlow.value
            return if (node == null) {
                // This should not happen because we should be preventing the user from selecting
                // an option requiring a node when no nodes are available
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, "cannot communicate with phone because no nodes are available")
                null
            } else {
                continuation(node)
            }
        }
    }

    class Phone @Inject constructor(
        @ApplicationContext private val appContext: Context,
        private val support: Support,
    ) {

        private val coroutineScope = CoroutineScope(Dispatchers.IO + Job())

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
            coroutineScope.launch {
                val intent = support.emailWearLogsToSupportIntent(messageEvent.data, appContext).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                appContext.startActivity(intent)
            }
        }
    }
}

enum class WatchMessageSendState {
    // a message being queued for delivery does not guarantee delivery
    QUEUED,
    // this occurs when attempting to queue a message for a node that is not available
    FAILED_TO_QUEUE,
}

enum class WatchPhoneCommunicationState {
    AVAILABLE,
    NOT_CONNECTED,
}
