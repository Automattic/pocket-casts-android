package au.com.shiftyjelly.pocketcasts.playlists.di

import au.com.shiftyjelly.pocketcasts.playlists.manual.AddToPlaylistFragment
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import au.com.shiftyjelly.pocketcasts.views.swipe.AddToPlaylistFragmentFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SharingModule {
    @Provides
    fun provideShareDialogFactory(): AddToPlaylistFragmentFactory = object : AddToPlaylistFragmentFactory {
        override fun create(episodeUuid: String): BaseDialogFragment {
            return AddToPlaylistFragment.newInstance(episodeUuid)
        }
    }
}
