package au.com.shiftyjelly.pocketcasts.repositories.playlist

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoOpDefaultPlaylistsInitializer @Inject constructor() : DefaultPlaylistsInitializer {
    override suspend fun initialize(force: Boolean) = Unit
}
