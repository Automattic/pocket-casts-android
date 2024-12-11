package au.com.shiftyjelly.pocketcasts

import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.utils.gravatar.GravatarService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.FragmentComponent

@Module
@InstallIn(FragmentComponent::class)
object GravatarModule {
    @Provides
    fun provideGravatarService(
        fragment: Fragment,
        onAvatarSelected: () -> Unit,
    ): GravatarService = NoOpGravatarSdkService(fragment, onAvatarSelected)
}
