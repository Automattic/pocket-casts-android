package au.com.shiftyjelly.pocketcasts.repositories

import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.TrackedEvent
import au.com.shiftyjelly.pocketcasts.models.type.Membership
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.servers.di.Cached
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.time.Clock
import java.time.Instant
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import timber.log.Timber

@Singleton
class MetricsAndChillListener @Inject constructor(
    private val settings: Settings,
    @Cached private val httpClient: OkHttpClient,
    private val moshi: Moshi,
    private val clock: Clock,
    @ApplicationScope private val coroutineScope: CoroutineScope,
) : AnalyticsTracker.Listener {
    private val eventAdapter = run {
        val newMoshi = moshi.newBuilder()
            .add(EventProperties::class.java, EventPropertiesJsonAdapter())
            .build()
        newMoshi.adapter<List<InputEvent>>(Types.newParameterizedType(List::class.java, InputEvent::class.java))
    }

    private val pendingEvents = LinkedBlockingDeque<InputEvent>(2_000)

    @Volatile
    private var shouldBeConnected = false

    private val webSocketRef: AtomicReference<WebSocket?> = AtomicReference(null)

    private val isReconnecting = AtomicBoolean()

    private val reconnectCounter = AtomicInteger()

    private val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            reconnectCounter.set(0)

            val events = ArrayList<InputEvent>(pendingEvents.size)
            pendingEvents.drainTo(events)
            if (events.isNotEmpty()) {
                runCatching { eventAdapter.toJson(events) }
                    .onSuccess(webSocket::send)
                    .onFailure { error -> Timber.d(error, "Failed to send analytics event") }
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            webSocketRef.compareAndSet(webSocket, null)
            establishConnection()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            webSocketRef.compareAndSet(webSocket, null)
            establishConnection()
        }
    }

    init {
        coroutineScope.launch {
            settings.cachedMembership.flow
                .map { membership -> membership != Membership.Empty }
                .distinctUntilChanged()
                .collect { isSignedIn ->
                    shouldBeConnected = isSignedIn
                    val webSocket = webSocketRef.get()
                    if (isSignedIn && webSocket == null) {
                        establishConnection()
                    } else if (!isSignedIn && webSocket != null) {
                        webSocket.close(1000, null)
                        webSocketRef.compareAndSet(webSocket, null)
                    }
                }
        }
    }

    private fun establishConnection() {
        if (!shouldBeConnected || webSocketRef.get() != null || !isReconnecting.compareAndSet(false, true)) {
            return
        }
        coroutineScope.launch {
            try {
                val counter = reconnectCounter.getAndIncrement()
                delay((counter * 15).coerceAtMost(60).seconds)

                val request = Request.Builder()
                    .url("ws://192.168.3.109:8080/input?token=admin:michal.sikora@automattic.com")
                    .build()
                val newSocket = httpClient.newWebSocket(request, webSocketListener)
                webSocketRef.getAndSet(newSocket)?.close(1000, "")
            } finally {
                isReconnecting.set(false)
            }
        }
    }

    override fun onEvent(
        event: AnalyticsEvent,
        properties: Map<String, Any>,
        trackedEvents: Map<String, TrackedEvent?>,
    ) {
        coroutineScope.launch {
            val inputEvent = InputEvent(
                name = event.key,
                timestamp = clock.instant(),
                properties = EventProperties(properties),
                platform = "Android",
            )
            val webSocket = webSocketRef.get()
            if (webSocket != null) {
                runCatching { eventAdapter.toJson(listOf(inputEvent)) }
                    .onSuccess(webSocket::send)
                    .onFailure { error -> Timber.d(error, "Failed to send analytics event") }
            } else {
                while (!pendingEvents.offerLast(inputEvent)) {
                    pendingEvents.pollFirst()
                }
            }
        }
    }
}

@JsonClass(generateAdapter = true)
internal data class InputEvent(
    val name: String,
    val timestamp: Instant,
    val properties: EventProperties,
    val platform: String,
)

internal data class EventProperties(
    val value: Map<String, Any>,
)

private class EventPropertiesJsonAdapter : JsonAdapter<EventProperties>() {
    override fun fromJson(reader: JsonReader): EventProperties {
        error("Deserialization of EventProperties is not supported")
    }

    override fun toJson(writer: JsonWriter, properties: EventProperties?) {
        requireNotNull(properties) {
            "properties was null! Wrap in .nullSafe() to write nullable values."
        }
        writer.beginObject()
        for ((key, value) in properties.value) {
            writer.name(key)
            when (value) {
                is String -> writer.value(value)
                is Long -> writer.value(value)
                is Double -> writer.value(value)
                is Number -> writer.value(value)
                is Boolean -> writer.value(value)
                else -> writer.value(value.toString())
            }
        }
        writer.endObject()
    }
}
