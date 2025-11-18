package au.com.shiftyjelly.pocketcasts.utils.accessibility

import android.content.Context
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener
import au.com.shiftyjelly.pocketcasts.utils.Util
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.view.accessibility.AccessibilityManager as AndroidAccessibilityManager

interface AccessibilityManager {
    val isTalkBackOnFlow: StateFlow<Boolean>
    fun startListening()
    fun stopListening()
}

class AccessibilityManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : AccessibilityManager {
    private val accessibilityManager = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AndroidAccessibilityManager
    private val _isTalkBackOnFlow = MutableStateFlow(false)
    override val isTalkBackOnFlow: StateFlow<Boolean> = _isTalkBackOnFlow.asStateFlow()

    private val accessibilityStateChangeListener = AccessibilityStateChangeListener {
        _isTalkBackOnFlow.value = it
    }

    override fun startListening() {
        _isTalkBackOnFlow.value = Util.isTalkbackOn(context)
        accessibilityManager?.addAccessibilityStateChangeListener(accessibilityStateChangeListener)
    }

    override fun stopListening() {
        accessibilityManager?.removeAccessibilityStateChangeListener(accessibilityStateChangeListener)
    }
}
