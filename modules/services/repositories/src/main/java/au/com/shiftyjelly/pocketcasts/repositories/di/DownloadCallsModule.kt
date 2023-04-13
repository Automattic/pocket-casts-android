package au.com.shiftyjelly.pocketcasts.repositories.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.utils.Util
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Call
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Provider
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DownloadCallsModule {

    @Provides
    @Singleton
    @DownloadPhoneOkHttpClient
    fun provideDownloadPhoneCallFactory(): OkHttpClient {
        val dispatcher = Dispatcher().apply {
            maxRequestsPerHost = 5
        }
        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @DownloadPhoneRequestBuilder
    fun downloadRequestBuilder(): Request.Builder = Request.Builder()

    @Provides
    @Singleton
    @DownloadCallFactory
    fun provideDownloadCallFactory(
        @ApplicationContext context: Context,
        @DownloadPhoneOkHttpClient phoneCallFactory: Provider<OkHttpClient>,
        @DownloadWearCallFactory wearCallFactory: Provider<Call.Factory>,
    ): Call.Factory =
        if (Util.isWearOs(context)) {
            Timber.e("TEST123, providing wear call factory")
            wearCallFactory
        } else {
            Timber.e("TEST123, providing phone call factory")
            phoneCallFactory
        }.get()

    @Provides
    @DownloadRequestBuilder
    fun provideDownloadRequestBuilder(
        @ApplicationContext context: Context,
        @DownloadWearRequestBuilder wearRequestBuilder: Provider<Request.Builder>,
        @DownloadPhoneRequestBuilder phoneRequestBuilder: Provider<Request.Builder>,
    ): Request.Builder =
        if (Util.isWearOs(context)) {
            Timber.e("TEST123, providing wear request builder")
            wearRequestBuilder
        } else {
            Timber.e("TEST123, providing phone request builder")
            phoneRequestBuilder
        }.get()
}

/* Download Call.Factory annotations */

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadCallFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadWearCallFactory

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadPhoneOkHttpClient

/* Download Request.Builder annotations */

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadRequestBuilder

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadWearRequestBuilder

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadPhoneRequestBuilder
