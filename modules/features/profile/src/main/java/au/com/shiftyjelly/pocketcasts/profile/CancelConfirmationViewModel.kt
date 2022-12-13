package au.com.shiftyjelly.pocketcasts.profile

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatLongStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CancelConfirmationViewModel
@Inject constructor(
    private val settings: Settings,
) : ViewModel() {
    var expirationDate: String? = null
    private val plusSubscription: SubscriptionStatus.Plus?
        get() = settings.getCachedSubscription() as? SubscriptionStatus.Plus

    init {
        expirationDate = plusSubscription?.expiryDate?.toLocalizedFormatLongStyle()
    }
}
