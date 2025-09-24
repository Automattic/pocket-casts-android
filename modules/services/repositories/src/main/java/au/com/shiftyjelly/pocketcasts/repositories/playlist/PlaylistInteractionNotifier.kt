package au.com.shiftyjelly.pocketcasts.repositories.playlist

import au.com.shiftyjelly.pocketcasts.models.db.dao.PlaylistDao
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

@Singleton
class PlaylistInteractionNotifier @Inject constructor(
    @ApplicationScope private val scope: CoroutineScope,
    private val playlistDao: PlaylistDao,
    private val notificationManager: NotificationManager,
) {
    private val isMonitoring = AtomicBoolean()

    fun monitorPlaylistsInteraction() {
        if (!isMonitoring.getAndSet(true)) {
            scope.launch {
                if (notificationManager.hasUserInteractedWithFeature(OnboardingNotificationType.Filters)) {
                    return@launch
                }
                awaitNewPlaylist()
                notificationManager.updateUserFeatureInteraction(OnboardingNotificationType.Filters)
            }
        }
    }

    suspend fun awaitNewPlaylist() {
        playlistDao.allPlaylistsFlow()
            .takeWhile(::hasOnlyDefaultPlaylists)
            .collect()
    }

    private fun hasOnlyDefaultPlaylists(playlists: List<PlaylistEntity>) = playlists.all { playlist ->
        playlist.uuid in Playlist.PREDEFINED_UUIDS
    }
}
