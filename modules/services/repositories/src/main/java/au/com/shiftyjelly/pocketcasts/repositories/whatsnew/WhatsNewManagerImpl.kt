package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewCatalog
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessage
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewServiceManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion
import java.net.HttpURLConnection.HTTP_GATEWAY_TIMEOUT
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import retrofit2.HttpException
import timber.log.Timber

@Singleton
class WhatsNewManagerImpl @Inject constructor(
    private val serviceManager: WhatsNewServiceManager,
    private val readStateStore: WhatsNewReadStateStore,
    private val settings: Settings,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : WhatsNewManager {
    private val _catalog = MutableStateFlow<WhatsNewCatalog?>(null)
    override val catalog = _catalog.asStateFlow()

    override val readState: StateFlow<WhatsNewReadState> = readStateStore.state

    private val evaluatedAt = MutableStateFlow(Instant.now())

    private val appVersion by lazy { ReleaseVersion.fromString(settings.getVersion()) }

    override val feedMessages = combine(
        catalog,
        settings.cachedSubscription.flow,
        evaluatedAt,
    ) { catalog, subscription, now ->
        WhatsNewMessageFilter.of(subscription?.tier, appVersion).feedMessages(catalog?.messages.orEmpty(), now)
    }.distinctUntilChanged()

    override val hasUnlistedMessages = combine(feedMessages, readState) { messages, readState ->
        messages.any { message -> !readState.isListed(message.id) }
    }.distinctUntilChanged()

    override val hasUnseenMessages = combine(feedMessages, readState) { messages, readState ->
        messages.any { message -> readState.isUnseen(message.id) }
    }.distinctUntilChanged()

    private val refreshLock = Mutex()

    override suspend fun refreshIfNeeded() = refreshCatalog(cacheControl = null)

    override suspend fun refresh() = refreshCatalog(CacheControl.FORCE_NETWORK)

    override fun markAsRead(messageIds: Collection<String>) = readStateStore.markAsRead(messageIds)

    override fun markAsSeen(messageIds: Collection<String>) = readStateStore.markAsSeen(messageIds)

    override fun markAsListed(messageIds: Collection<String>) = readStateStore.markAsListed(messageIds)

    override suspend fun markFeedAsSeen() = markAsSeen(feedMessages.first().map(WhatsNewMessage::id))

    override fun markAsResponded(pollId: String) = readStateStore.markAsResponded(pollId)

    override fun resetReadState() = readStateStore.reset()

    private suspend fun refreshCatalog(cacheControl: CacheControl?) {
        FeatureFlag.awaitProvidersInitialised()
        if (!FeatureFlag.isEnabled(Feature.WHATS_NEW_FEED)) return

        refreshLock.withLock {
            withContext(ioDispatcher) {
                if (_catalog.value == null) {
                    fetchCatalog(CacheControl.FORCE_CACHE)
                }
                fetchCatalog(cacheControl)
            }
        }
        evaluatedAt.value = Instant.now()
    }

    private suspend fun fetchCatalog(cacheControl: CacheControl?) {
        try {
            _catalog.value = serviceManager.getCatalog(cacheControl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val isCacheMiss = cacheControl?.onlyIfCached == true && (e as? HttpException)?.code() == HTTP_GATEWAY_TIMEOUT
            if (!isCacheMiss) {
                Timber.w(e, "Could not refresh the What's New catalog")
            }
        }
    }
}
