package au.com.shiftyjelly.pocketcasts.di

import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadWearCallFactory
import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadWearRequestBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Call
import okhttp3.Request
import javax.inject.Singleton

/**
 * This module is used to provide placeholders for wear dependencies that are
 * not needed at runtime in the automotive app, but still must have @Provides methods.
 */
@Module
@InstallIn(SingletonComponent::class)
object AutomotiveWearModule {

    @Provides
    @DownloadWearRequestBuilder
    fun downloadRequestBuilder(): Request.Builder =
        throw RuntimeException("@DownloadWearRequestBuilder is not available in the phone app")

    @Provides
    @Singleton
    @DownloadWearCallFactory
    fun provideDownloadWearCallFactory(): Call.Factory =
        throw RuntimeException("@DownloadWearCallFactory is not available in the phone app")
}
