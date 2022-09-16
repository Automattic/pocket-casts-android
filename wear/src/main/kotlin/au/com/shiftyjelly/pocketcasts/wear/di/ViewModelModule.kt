package au.com.shiftyjelly.pocketcasts.wear.di

import android.content.ComponentName
import android.content.Context
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import au.com.shiftyjelly.pocketcasts.wear.data.service.playback.PlaybackService
import com.google.android.horologist.media.data.mapper.MediaItemExtrasMapper
import com.google.android.horologist.media.data.mapper.MediaItemExtrasMapperNoopImpl
import com.google.android.horologist.media.data.mapper.MediaItemMapper
import com.google.android.horologist.media.data.mapper.MediaMapper
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.repository.PlayerRepository
import com.google.android.horologist.media3.flows.buildSuspend
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@Module
@InstallIn(ActivityRetainedComponent::class)
object ViewModelModule {
    @ActivityRetainedScoped
    @Provides
    fun providesCoroutineScope(
        activityRetainedLifecycle: ActivityRetainedLifecycle
    ): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.Default).also {
            activityRetainedLifecycle.addOnClearedListener {
                it.cancel()
            }
        }
    }

    @Provides
    @ActivityRetainedScoped
    fun mediaController(
        @ApplicationContext application: Context,
        activityRetainedLifecycle: ActivityRetainedLifecycle,
        coroutineScope: CoroutineScope
    ): Deferred<MediaBrowser> =
        coroutineScope.async {
            MediaBrowser.Builder(
                application,
                SessionToken(application, ComponentName(application, PlaybackService::class.java))
            ).buildSuspend()
        }.also {
            activityRetainedLifecycle.addOnClearedListener {
                it.cancel()
                if (it.isCompleted && !it.isCancelled) {
                    it.getCompleted().release()
                }
            }
        }

    @Provides
    @ActivityRetainedScoped
    fun playerRepositoryImpl(
        mediaMapper: MediaMapper,
        mediaItemMapper: MediaItemMapper,
        activityRetainedLifecycle: ActivityRetainedLifecycle,
        coroutineScope: CoroutineScope,
        mediaController: Deferred<MediaBrowser>,
    ): PlayerRepositoryImpl =
        PlayerRepositoryImpl(
            mediaMapper = mediaMapper,
            mediaItemMapper = mediaItemMapper
        ).also { playerRepository ->
            activityRetainedLifecycle.addOnClearedListener {
                playerRepository.close()
            }
            coroutineScope.launch(Dispatchers.Main) {
                val player = mediaController.await()
                playerRepository.connect(
                    player = player,
                    onClose = player::release
                )
            }
        }

    @Provides
    @ActivityRetainedScoped
    fun playerRepository(
        playerRepositoryImpl: PlayerRepositoryImpl,
    ): PlayerRepository = playerRepositoryImpl

    @Provides
    @ActivityRetainedScoped
    fun mediaItemMapper(mediaItemExtrasMapper: MediaItemExtrasMapper): MediaItemMapper =
        MediaItemMapper(mediaItemExtrasMapper)

    @Provides
    fun mediaItemExtrasMapper(): MediaItemExtrasMapper = MediaItemExtrasMapperNoopImpl
}
