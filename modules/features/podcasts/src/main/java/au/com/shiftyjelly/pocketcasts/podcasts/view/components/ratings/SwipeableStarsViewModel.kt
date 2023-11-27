package au.com.shiftyjelly.pocketcasts.podcasts.view.components.ratings

import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.utils.Util
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SwipeableStarsViewModel @Inject constructor(
    @ApplicationContext context: Context,
) : ViewModel() {

    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager?
    private val _accessibilityEnabledState = MutableStateFlow(Util.isTalkbackOn(context))
    val accessibilityActiveState = _accessibilityEnabledState.asStateFlow()

    private val accessibilityStateChangeListener = AccessibilityStateChangeListener {
        _accessibilityEnabledState.value = it
    }

    init {
        accessibilityManager?.addAccessibilityStateChangeListener(accessibilityStateChangeListener)
    }

    override fun onCleared() {
        accessibilityManager?.removeAccessibilityStateChangeListener(accessibilityStateChangeListener)
        super.onCleared()
    }
}
