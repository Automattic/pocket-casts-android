package au.com.shiftyjelly.pocketcasts.repositories.di

import android.app.Service
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.media3.session.MediaLibraryService
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackService.CustomMediaLibrarySessionCallback
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ServiceComponent
import dagger.hilt.android.scopes.ServiceScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

@Module
@InstallIn(ServiceComponent::class)
object PlaybackServiceModule {
    @ServiceScoped
    @Provides
    fun serviceCoroutineScope(
        service: Service,
    ): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default).also {
            (service as LifecycleOwner).lifecycle.addObserver(object : DefaultLifecycleObserver {
                override fun onStop(owner: LifecycleOwner) {
                    it.cancel()
                }
            })
        }
    }

    @ServiceScoped
    @Provides
    fun librarySessionCallback(
        serviceCoroutineScope: CoroutineScope,
    ): MediaLibraryService.MediaLibrarySession.Callback =
        CustomMediaLibrarySessionCallback(serviceCoroutineScope)
}
