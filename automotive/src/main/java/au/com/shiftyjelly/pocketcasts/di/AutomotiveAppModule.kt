package au.com.shiftyjelly.pocketcasts.di

import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadCallFactory
import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadOkHttpClient
import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadRequestBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AutomotiveAppModule {

    @Provides
    @Singleton
    @DownloadCallFactory
    fun downloadCallFactory(
        @DownloadOkHttpClient phoneCallFactory: OkHttpClient,
    ): Call.Factory = phoneCallFactory

    @Provides
    @DownloadRequestBuilder
    fun downloadRequestBuilder(): Request.Builder = Request.Builder()
}
