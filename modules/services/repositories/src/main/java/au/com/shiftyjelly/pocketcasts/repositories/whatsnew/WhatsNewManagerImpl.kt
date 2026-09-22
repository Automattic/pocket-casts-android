package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewCatalog
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewServiceManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion
import java.time.Duration
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class WhatsNewManagerImpl @Inject constructor(
    private val serviceManager: WhatsNewServiceManager,
    private val catalogStore: WhatsNewCatalogStore,
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

    override suspend fun refreshIfNeeded() = refreshCatalog(force = false)

    override suspend fun refresh() = refreshCatalog(force = true)

    override fun markAsRead(messageIds: Collection<String>) = readStateStore.markAsRead(messageIds)

    override fun markAsSeen(messageIds: Collection<String>) = readStateStore.markAsSeen(messageIds)

    override fun markAsListed(messageIds: Collection<String>) = readStateStore.markAsListed(messageIds)

    override fun markAsResponded(pollId: String) = readStateStore.markAsResponded(pollId)

    override fun resetReadState() = readStateStore.reset()

    private suspend fun refreshCatalog(force: Boolean) {
        FeatureFlag.awaitProvidersInitialised()
        if (!FeatureFlag.isEnabled(Feature.WHATS_NEW_FEED)) return

        refreshLock.withLock {
            withContext(ioDispatcher) {
                val locale = serviceManager.catalogLocale()
                if (_catalog.value == null) {
                    _catalog.value = catalogStore.read(locale)
                }
                if (force || _catalog.value == null || isStale(locale)) {
                    fetchCatalog(locale)
                }
            }
        }
        evaluatedAt.value = Instant.now()
    }

    private suspend fun fetchCatalog(locale: String) {
        try {
            val body = serviceManager.getCatalog()
            val catalog = catalogStore.decode(body) ?: return
            catalogStore.write(locale, body)
            _catalog.value = catalog
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "Could not refresh the What's New catalog")
        }
    }

    private fun isStale(locale: String): Boolean {
        val writtenAt = catalogStore.writtenAt(locale) ?: return true
        val age = Duration.between(writtenAt, Instant.now())
        return age.isNegative || age >= REFRESH_INTERVAL
    }

    companion object {
        val REFRESH_INTERVAL: Duration = Duration.ofHours(6)
    }
}
