package au.com.shiftyjelly.pocketcasts.di

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaLibraryService
import au.com.shiftyjelly.pocketcasts.AutoPlaybackService.AutoMediaLibrarySessionCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineScope

@UnstableApi
@Module
@InstallIn(ServiceComponent::class)
object PlaybackServiceModule {
    @ServiceScoped
    @Provides
    fun librarySessionCallback(
        serviceCoroutineScope: CoroutineScope,
        @ApplicationContext context: Context,
    ): MediaLibraryService.MediaLibrarySession.Callback =
        AutoMediaLibrarySessionCallback(
            serviceScope = serviceCoroutineScope,
            context = context,
        )
}
