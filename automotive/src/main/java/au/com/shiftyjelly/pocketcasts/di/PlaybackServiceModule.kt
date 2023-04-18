package au.com.shiftyjelly.pocketcasts.di

import androidx.media3.session.MediaLibraryService
import au.com.shiftyjelly.pocketcasts.AutoPlaybackService.AutoMediaLibrarySessionCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(ServiceComponent::class)
object PlaybackServiceModule {
    @ServiceScoped
    @Provides
    fun librarySessionCallback(
        serviceCoroutineScope: CoroutineScope,
    ): MediaLibraryService.MediaLibrarySession.Callback =
        AutoMediaLibrarySessionCallback(serviceCoroutineScope)
}
