package au.com.shiftyjelly.pocketcasts.repositories.di

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import au.com.shiftyjelly.pocketcasts.crashlogging.di.ProvideApplicationScope
import au.com.shiftyjelly.pocketcasts.payment.Logger
import au.com.shiftyjelly.pocketcasts.payment.billing.BillingClientWrapper
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncAccountManager
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber

@Module
@InstallIn(SingletonComponent::class)
class RepositoryProviderModule {

    @Provides
    @Singleton
    fun provideTokenHandler(syncAccountManager: SyncAccountManager): TokenHandler = syncAccountManager

    @Provides
    @Singleton
    @ApplicationScope
    fun coroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    fun provideApplicationScope(
        @ApplicationScope appScope: CoroutineScope,
    ): ProvideApplicationScope = ProvideApplicationScope { appScope }

    @Provides
    @Singleton
    @ProcessLifecycle
    fun processLifecycle(): LifecycleOwner = ProcessLifecycleOwner.get()

    @Provides
    @Singleton
    fun provideBillingCilentWrapper(
        @ApplicationContext context: Context,
    ) = BillingClientWrapper(
        context = context,
        logger = object : Logger {
            override fun info(message: String) {
                Timber.tag(LogBuffer.TAG_SUBSCRIPTIONS).i(message)
            }

            override fun warning(message: String) {
                Timber.tag(LogBuffer.TAG_SUBSCRIPTIONS).w(message)
                LogBuffer.w(LogBuffer.TAG_SUBSCRIPTIONS, message)
            }

            override fun error(message: String, exception: Throwable) {
                Timber.tag(LogBuffer.TAG_SUBSCRIPTIONS).e(exception, message)
                LogBuffer.e(LogBuffer.TAG_SUBSCRIPTIONS, exception, message)
            }
        },
    )
}
