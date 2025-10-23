package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import android.content.Context
import android.os.Build
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTracker,
    private val credentialRequest: GetCredentialRequest,
    @ApplicationContext context: Context,
) : ViewModel() {
    private val credentialManager: CredentialManager by lazy { CredentialManager.create(context) }

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.WEAR_SIGNIN_SHOWN)
    }

    fun onGoogleLoginClicked(onFinish: () -> Unit) {
        analyticsTracker.track(AnalyticsEvent.WEAR_SIGNIN_GOOGLE_TAPPED)
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
        analyticsTracker.track(AnalyticsEvent.WEAR_SIGNIN_PHONE_TAPPED)
    }

    fun onEmailLoginClicked() {
        analyticsTracker.track(AnalyticsEvent.WEAR_SIGNIN_EMAIL_TAPPED)
    }
}
