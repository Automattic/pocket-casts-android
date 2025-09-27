package au.com.shiftyjelly.pocketcasts.playlists.di

import au.com.shiftyjelly.pocketcasts.playlists.manual.AddToPlaylistFragment
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.fragments.BaseDialogFragment
import au.com.shiftyjelly.pocketcasts.views.swipe.AddToPlaylistFragmentFactory
import au.com.shiftyjelly.pocketcasts.views.swipe.AddToPlaylistFragmentFactory.Source
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object SharingModule {
    @Provides
    fun provideShareDialogFactory(): AddToPlaylistFragmentFactory = object : AddToPlaylistFragmentFactory {
        override fun create(
            source: Source,
            episodeUuid: String,
            customTheme: Theme.ThemeType?,
        ): BaseDialogFragment {
            return AddToPlaylistFragment.newInstance(source, episodeUuid, customTheme)
        }
    }
}
