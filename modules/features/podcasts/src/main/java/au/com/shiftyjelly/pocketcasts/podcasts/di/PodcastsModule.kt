package au.com.shiftyjelly.pocketcasts.podcasts.di

import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButton
import au.com.shiftyjelly.pocketcasts.views.helper.PlayButtonListener
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent

@Module
@InstallIn(ActivityComponent::class)
abstract class PodcastsModule {

    @Binds
    abstract fun providesPlayListener(playButtonListener: PlayButtonListener): PlayButton.OnClickListener
}
