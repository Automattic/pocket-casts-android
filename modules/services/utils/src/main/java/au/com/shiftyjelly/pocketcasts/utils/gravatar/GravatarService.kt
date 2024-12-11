package au.com.shiftyjelly.pocketcasts.utils.gravatar

import androidx.fragment.app.Fragment

interface GravatarService {

    fun launchQuickEditor(isLightTheme: Boolean, email: String)
    fun launchExternalQuickEditor(email: String)
    suspend fun logout(email: String)

    interface Factory {
        fun create(fragment: Fragment? = null, onAvatarSelected: (() -> Unit)? = null): GravatarService
    }
}
