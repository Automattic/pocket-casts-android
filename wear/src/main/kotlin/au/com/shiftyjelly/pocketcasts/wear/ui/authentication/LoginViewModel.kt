package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import android.content.Context
import android.os.Build
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.WearSigninEmailTappedEvent
import com.automattic.eventhorizon.WearSigninGoogleTappedEvent
import com.automattic.eventhorizon.WearSigninPhoneTappedEvent
import com.automattic.eventhorizon.WearSigninShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val eventHorizon: EventHorizon,
    private val credentialRequest: GetCredentialRequest,
    @ApplicationContext context: Context,
) : ViewModel() {
    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    fun onShown() {
        eventHorizon.track(WearSigninShownEvent)
    }

    fun onGoogleLoginClicked(onFinish: () -> Unit) {
        eventHorizon.track(WearSigninGoogleTappedEvent)
        viewModelScope.launch {
            val isGoogleSignInAvailable = Settings.GOOGLE_SIGN_IN_SERVER_CLIENT_ID.isNotEmpty()

            if (isGoogleSignInAvailable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatching {
                    credentialManager.prepareGetCredential(
                        request = credentialRequest,
                    )
                }.getOrNull()
                onFinish()
            }
        }
    }

    fun onPhoneLoginClicked() {
        eventHorizon.track(WearSigninPhoneTappedEvent)
    }

    fun onEmailLoginClicked() {
        eventHorizon.track(WearSigninEmailTappedEvent)
    }
}
