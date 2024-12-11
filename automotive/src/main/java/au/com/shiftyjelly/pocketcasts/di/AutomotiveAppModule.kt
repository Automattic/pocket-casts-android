package au.com.shiftyjelly.pocketcasts.di

import au.com.shiftyjelly.pocketcasts.NoOpGravatarSdkService
import au.com.shiftyjelly.pocketcasts.servers.di.Downloads
import au.com.shiftyjelly.pocketcasts.utils.gravatar.GravatarService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

@Module
@InstallIn(SingletonComponent::class)
abstract class AutomotiveAppModule {

    companion object {
        @Provides
        @Downloads
        fun downloadRequestBuilder(): Request.Builder = Request.Builder()
    }

    @Binds
    @Downloads
    abstract fun downloadsCallFactory(@Downloads client: OkHttpClient): Call.Factory

    @Binds
    abstract fun gravatarService(factory: NoOpGravatarSdkService.Factory): GravatarService.Factory
}
