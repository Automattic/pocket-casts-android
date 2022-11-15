package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.account.BuildConfig
import au.com.shiftyjelly.pocketcasts.utils.extensions.isGooglePlayServicesAvailableSuccess
import com.google.android.gms.common.GoogleApiAvailability
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@HiltViewModel
class OnboardingLoginOrSignUpViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel() {

    val showContinueWithGoogleButton = BuildConfig.SINGLE_SIGN_ON_ENABLED && GoogleApiAvailability.getInstance().isGooglePlayServicesAvailableSuccess(context)
}
