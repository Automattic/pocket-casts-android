package au.com.shiftyjelly.pocketcasts

import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import au.com.shiftyjelly.pocketcasts.gravatar.BuildConfig.GRAVATAR_APP_ID
import au.com.shiftyjelly.pocketcasts.utils.Gravatar
import au.com.shiftyjelly.pocketcasts.utils.gravatar.GravatarService
import com.gravatar.quickeditor.GravatarQuickEditor
import com.gravatar.quickeditor.ui.GetQuickEditorResult
import com.gravatar.quickeditor.ui.GravatarQuickEditorActivity
import com.gravatar.quickeditor.ui.GravatarQuickEditorResult
import com.gravatar.quickeditor.ui.editor.AuthenticationMethod
import com.gravatar.quickeditor.ui.editor.AvatarPickerContentLayout
import com.gravatar.quickeditor.ui.editor.GravatarQuickEditorParams
import com.gravatar.quickeditor.ui.editor.GravatarUiMode
import com.gravatar.quickeditor.ui.oauth.OAuthParams
import com.gravatar.types.Email
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GravatarSdkService @AssistedInject constructor(@Assisted fragment: Fragment?, @Assisted onAvatarSelected: (() -> Unit)?) :
    GravatarService {

    private val gravatarExternalQuickEditorLauncher =
        fragment?.registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { _ ->
            onAvatarSelected?.invoke()
        }

    private val gravatarNativeQuickEditorLauncher = fragment?.registerForActivityResult(GetQuickEditorResult()) { quickEditorResult ->
        when (quickEditorResult) {
            GravatarQuickEditorResult.AVATAR_SELECTED -> onAvatarSelected?.invoke()
            GravatarQuickEditorResult.DISMISSED,
            null,
            -> {
                /* Do nothing */
            }
        }
    }

    override fun launchExternalQuickEditor(email: String) {
        gravatarExternalQuickEditorLauncher?.launch(Intent(Intent.ACTION_VIEW, Uri.parse(Gravatar.getGravatarChangeAvatarUrl(email))))
            ?: error("gravatarExternalQuickEditorLauncher is not initialized")
    }

    override fun launchQuickEditor(isLightTheme: Boolean, email: String) {
        gravatarNativeQuickEditorLauncher?.launch(
            GravatarQuickEditorActivity.GravatarEditorActivityArguments(
                gravatarQuickEditorParams = GravatarQuickEditorParams {
                    this.email = Email(email)
                    avatarPickerContentLayout = AvatarPickerContentLayout.Horizontal
                    uiMode = if (isLightTheme) GravatarUiMode.LIGHT else GravatarUiMode.DARK
                },
                authenticationMethod = AuthenticationMethod.OAuth(
                    OAuthParams {
                        clientId = GRAVATAR_APP_ID
                        redirectUri = Gravatar.GRAVATAR_QE_REDIRECT_URL
                    },
                ),
            ),
        ) ?: error("gravatarNativeQuickEditorLauncher is not initialized")
    }

    override suspend fun logout(email: String) = withContext(Dispatchers.IO) {
        GravatarQuickEditor.logout(
            email = Email(
                email,
            ),
        )
    }

    @AssistedFactory
    interface Factory : GravatarService.Factory {
        override fun create(fragment: Fragment?, onAvatarSelected: (() -> Unit)?): GravatarSdkService
    }
}
