package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.account.AccountActivity.AccountUpdatedSource
import au.com.shiftyjelly.pocketcasts.account.R.drawable.ic_email_address_changed
import au.com.shiftyjelly.pocketcasts.account.R.drawable.ic_password_changed
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.localization.R
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class DoneViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
) : ViewModel() {

    private val mutableState = MutableStateFlow<State>(State.Empty)
    val state: StateFlow<State> = mutableState

    val title = MutableLiveData<String>()
    val detail = MutableLiveData<String>()
    val imageRef = MutableLiveData<Int>()

//    fun setChangedEmailState(detail: String = "") {
//        mutableState.value = State.SuccessFullChangedEmail(detail = detail)
//    }

    fun setChangedPasswordState(detail: String = "") {
        mutableState.value = State.SuccessFullChangedPassword(detail = detail)
    }

    fun updateTitle(value: String) {
        title.value = value
    }

    fun updateDetail(value: String) {
        detail.value = value
    }

    fun updateImage(resid: Int) {
        imageRef.value = resid
    }

    fun trackShown(source: AccountUpdatedSource) {
        analyticsTracker.track(AnalyticsEvent.ACCOUNT_UPDATED_SHOWN, mapOf(SOURCE_KEY to source.analyticsValue))
    }

    fun trackDismissed() {
        analyticsTracker.track(AnalyticsEvent.ACCOUNT_UPDATED_DISMISSED)
    }

    sealed class State {
        data object Empty : State()
        data class SuccessFullChangedEmail(
            var detail: String = "",
            val tittleResourceId: Int = R.string.profile_email_address_changed,
            val imageResourceId: Int = ic_email_address_changed,
            val analyticsValue: AccountUpdatedSource = AccountUpdatedSource.CHANGE_EMAIL,
        ) : State()
        data class SuccessFullChangedPassword(
            var detail: String = "",
            val tittleResourceId: Int = R.string.profile_password_changed,
            val imageResourceId: Int = ic_password_changed,
            val analyticsValue: AccountUpdatedSource = AccountUpdatedSource.CHANGE_PASSWORD,
        ) : State()
    }

    companion object {
        private const val SOURCE_KEY = "source"
    }
}
