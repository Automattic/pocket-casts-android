package au.com.shiftyjelly.pocketcasts.playlists.di

import androidx.fragment.app.FragmentManager
import au.com.shiftyjelly.pocketcasts.playlists.manual.AddToPlaylistFragment
import au.com.shiftyjelly.pocketcasts.views.swipe.AddToPlaylistHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SharingModule {
    @Provides
    fun provideShareDialogFactory(): AddToPlaylistHandler = object : AddToPlaylistHandler {
        override fun handle(episodeUuid: String, fragmentManager: FragmentManager) {
            val fragment = AddToPlaylistFragment.newInstance(episodeUuid)
            if (fragmentManager.findFragmentByTag("add-to-playlist") == null) {
                fragment.show(fragmentManager, "add-to-playlist")
            }
        }
    }
}
