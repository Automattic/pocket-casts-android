package au.com.shiftyjelly.pocketcasts.account.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.account.AccountActivity.AccountUpdatedSource
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DoneViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper
) : ViewModel() {

    val title = MutableLiveData<String>()
    val detail = MutableLiveData<String>()
    val imageRef = MutableLiveData<Int>()

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

    companion object {
        private const val SOURCE_KEY = "source"
    }
}
