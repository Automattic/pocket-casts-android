package au.com.shiftyjelly.pocketcasts

import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.utils.gravatar.GravatarService
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

class NoOpGravatarSdkService @AssistedInject constructor(@Assisted fragment: Fragment?, @Assisted onAvatarSelected: (() -> Unit)?) :
    GravatarService {

    override fun launchExternalQuickEditor(email: String) {
        error("Operation not supported")
    }

    override fun launchQuickEditor(isLightTheme: Boolean, email: String) {
        error("Operation not supported")
    }

    override suspend fun logout(email: String) {
        error("Operation not supported")
    }

    @AssistedFactory
    interface Factory : GravatarService.Factory {
        override fun create(fragment: Fragment?, onAvatarSelected: (() -> Unit)?): NoOpGravatarSdkService
    }
}
