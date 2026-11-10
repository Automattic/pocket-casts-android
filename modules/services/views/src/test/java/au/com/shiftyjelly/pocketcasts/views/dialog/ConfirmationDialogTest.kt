package au.com.shiftyjelly.pocketcasts.views.dialog

import android.os.Looper
import android.view.View
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltShimApplication::class)
class ConfirmationDialogTest {
    @get:Rule
    val composeRule = createEmptyComposeRule()

    private val controller = Robolectric.buildActivity(HiltShimActivity::class.java).setup()
    private val fragmentManager get() = controller.get().supportFragmentManager

    private var confirmCount = 0
    private val dismissedWithoutAction = mutableListOf<Boolean>()

    @Test
    fun `confirm button runs the confirm action exactly once`() {
        showDialog()

        composeRule.onNodeWithText("Delete everywhere").performClick()
        idle()

        assertEquals(1, confirmCount)
        assertEquals(listOf(false), dismissedWithoutAction)
        assertDialogRemoved()
    }

    @Test
    fun `cancel does not run the confirm action`() {
        val dialog = showDialog()

        dialog.requireDialog().cancel()
        idle()

        assertEquals(0, confirmCount)
        assertEquals(listOf(true), dismissedWithoutAction)
        assertDialogRemoved()
    }

    @Test
    fun `tap outside does not run the confirm action`() {
        val dialog = showDialog()

        dialog.requireDialog().findViewById<View>(com.google.android.material.R.id.touch_outside).performClick()
        idle()

        assertEquals(0, confirmCount)
        assertEquals(listOf(true), dismissedWithoutAction)
        assertDialogRemoved()
    }

    @Test
    fun `pausing the host dismisses without running the confirm action`() {
        showDialog()

        controller.pause()
        idle()

        assertEquals(0, confirmCount)
        assertEquals(listOf(true), dismissedWithoutAction)
        assertDialogRemoved()
    }

    @Test
    fun `secondary button does not run the confirm action`() {
        var secondaryCount = 0
        showDialog { setSecondaryButtonType(ConfirmationDialog.ButtonType.Normal("Keep")).setOnSecondary { secondaryCount++ } }

        composeRule.onNodeWithText("Keep").performClick()
        idle()

        assertEquals(0, confirmCount)
        assertEquals(1, secondaryCount)
        assertEquals(listOf(false), dismissedWithoutAction)
        assertDialogRemoved()
    }

    private fun showDialog(configure: ConfirmationDialog.() -> ConfirmationDialog = { this }): ConfirmationDialog {
        val dialog = ConfirmationDialog()
            .setTitle("Delete")
            .setButtonType(ConfirmationDialog.ButtonType.Danger("Delete everywhere"))
            .setOnConfirm { confirmCount++ }
            .setOnDismiss { dismissedWithoutAction += it }
            .configure()
        dialog.show(fragmentManager, TAG)
        idle()
        return dialog
    }

    private fun assertDialogRemoved() {
        assertNull(fragmentManager.findFragmentByTag(TAG))
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private companion object {
        const val TAG = "confirmation"
    }
}
