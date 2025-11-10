package au.com.shiftyjelly.pocketcasts.appreview

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class AppReviewViewModel @Inject constructor(
    private val settings: Settings,
    private val clock: Clock,
) : ViewModel() {
    fun declineAppReview() {
        val newTimestamps = buildList {
            addAll(settings.appReviewLastDeclineTimestamps.value.takeLast(1))
            add(clock.instant())
        }
        settings.appReviewLastDeclineTimestamps.set(newTimestamps, updateModifiedAt = false)
    }
}
