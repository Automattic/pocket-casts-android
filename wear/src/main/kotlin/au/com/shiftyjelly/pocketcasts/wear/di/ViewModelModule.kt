package au.com.shiftyjelly.pocketcasts.wear.di

import com.google.android.horologist.media.data.mapper.MediaItemExtrasMapper
import com.google.android.horologist.media.data.mapper.MediaItemExtrasMapperNoopImpl
import com.google.android.horologist.media.data.mapper.MediaItemMapper
import com.google.android.horologist.media.data.mapper.MediaMapper
import com.google.android.horologist.media.data.repository.PlayerRepositoryImpl
import com.google.android.horologist.media.repository.PlayerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.scopes.ActivityRetainedScoped

@Module
@InstallIn(ActivityRetainedComponent::class)
object ViewModelModule {
    @Provides
    @ActivityRetainedScoped
    fun playerRepositoryImpl(
        mediaMapper: MediaMapper,
        mediaItemMapper: MediaItemMapper,
        activityRetainedLifecycle: ActivityRetainedLifecycle,
        /*coroutineScope: CoroutineScope,*/
        /*mediaController: Deferred<MediaBrowser>,*/
    ): PlayerRepositoryImpl =
        PlayerRepositoryImpl(
            mediaMapper = mediaMapper,
            mediaItemMapper = mediaItemMapper
        ).also { playerRepository ->
            activityRetainedLifecycle.addOnClearedListener {
                playerRepository.close()
            }
            // TODO: Connect to player repository
            /*coroutineScope.launch(Dispatchers.Main) {
                val player = mediaController.await()
                playerRepository.connect(
                    player = player,
                    onClose = player::release
                )
            }*/
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
