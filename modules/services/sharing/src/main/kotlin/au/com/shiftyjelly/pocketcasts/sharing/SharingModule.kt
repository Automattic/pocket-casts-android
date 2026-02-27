package au.com.shiftyjelly.pocketcasts.sharing

import android.content.Context
import com.automattic.eventhorizon.EventHorizon
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
object SharingModule {
    @Provides
    fun provideSharingClient(
        @ApplicationContext context: Context,
        mediaService: MediaService,
        listeners: Set<@JvmSuppressWildcards SharingClient.Listener>,
    ): SharingClient = SharingClient(
        context,
        mediaService,
        listeners,
    )

    @Provides
    @IntoSet
    fun provideAnalyticsListener(
        eventHorizon: EventHorizon,
    ): SharingClient.Listener = SharingAnalytics(eventHorizon)

    @Provides
    @IntoSet
    fun provideLoggingListener(): SharingClient.Listener = SharingLogger()
}
