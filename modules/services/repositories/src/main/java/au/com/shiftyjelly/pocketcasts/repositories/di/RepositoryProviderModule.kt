package au.com.shiftyjelly.pocketcasts.repositories.di

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.crashlogging.di.ProvideApplicationScope
import au.com.shiftyjelly.pocketcasts.payment.PaymentClient
import au.com.shiftyjelly.pocketcasts.payment.PaymentDataSource
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.repositories.payment.AnalyticsPaymentListener
import au.com.shiftyjelly.pocketcasts.repositories.payment.LoggingPaymentListener
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncAccountManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.server.ListWebService
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import au.com.shiftyjelly.pocketcasts.utils.Util
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
class RepositoryProviderModule {

    @Provides
    @Singleton
    fun provideTokenHandler(syncAccountManager: SyncAccountManager): TokenHandler = syncAccountManager

    @Provides
    @Singleton
    @ApplicationScope
    fun coroutineScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    fun provideApplicationScope(
        @ApplicationScope appScope: CoroutineScope,
    ): ProvideApplicationScope = ProvideApplicationScope { appScope }

    @Provides
    @Singleton
    @ProcessLifecycle
    fun processLifecycle(): LifecycleOwner = ProcessLifecycleOwner.get()

    @Provides
    @IntoSet
    fun provideLoggingListener(): PaymentClient.Listener {
        return LoggingPaymentListener()
    }

    @Provides
    @IntoSet
    fun provideAnalyticsListener(tracker: AnalyticsTracker): PaymentClient.Listener {
        return AnalyticsPaymentListener(tracker)
    }

    @Provides
    @Singleton
    fun providePaymentDataSource(
        @ApplicationContext context: Context,
        listeners: Set<@JvmSuppressWildcards PaymentClient.Listener>,
    ): PaymentDataSource {
        return if (context.packageName == "au.com.shiftyjelly.pocketcasts") {
            PaymentDataSource.billing(context, listeners)
        } else {
            PaymentDataSource.fake()
        }
    }

    @Provides
    @Singleton
    internal fun provideDiscoverRepository(listWebService: ListWebService, syncManager: SyncManager, @ApplicationContext context: Context): ListRepository {
        val platform = if (Util.isAutomotive(context)) "automotive" else "android"
        return ListRepository(
            listWebService,
            syncManager,
            platform,
        )
    }
}
