package au.com.shiftyjelly.pocketcasts.wear.di

import android.content.Context
import android.net.ConnectivityManager
import au.com.shiftyjelly.pocketcasts.NoOpGravatarSdkService
import au.com.shiftyjelly.pocketcasts.utils.gravatar.GravatarService
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class WearAppModule {

    companion object {
        @Provides
        fun connectivityManager(
            @ApplicationContext application: Context,
        ): ConnectivityManager =
            application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    @Binds
    abstract fun gravatarService(factory: NoOpGravatarSdkService.Factory): GravatarService.Factory
}
