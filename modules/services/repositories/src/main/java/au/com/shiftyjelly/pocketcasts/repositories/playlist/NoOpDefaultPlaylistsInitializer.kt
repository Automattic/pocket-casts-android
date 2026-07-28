package au.com.shiftyjelly.pocketcasts.repositories.playlist

import javax.inject.Inject

class NoOpDefaultPlaylistsInitializer @Inject constructor() : DefaultPlaylistsInitializer {
    override suspend fun initialize(force: Boolean) = Unit
}
