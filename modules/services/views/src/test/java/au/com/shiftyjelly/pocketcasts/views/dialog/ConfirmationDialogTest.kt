package au.com.shiftyjelly.pocketcasts.views.dialog

import android.os.Looper
import android.view.View
import au.com.shiftyjelly.pocketcasts.views.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = HiltShimApplication::class)
class ConfirmationDialogTest {
    private val controller = Robolectric.buildActivity(HiltShimActivity::class.java).setup()
    private val fragmentManager get() = controller.get().supportFragmentManager

    private var confirmCount = 0
    private val dismissedWithoutAction = mutableListOf<Boolean>()

    @Test
    fun `confirm button runs the confirm action exactly once`() {
        val dialog = showDialog()

        dialog.requireView().findViewById<View>(R.id.btnConfirm).performClick()
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
        val dialog = showDialog { setSecondaryButtonType(ConfirmationDialog.ButtonType.Normal("Keep")).setOnSecondary { secondaryCount++ } }

        dialog.requireView().findViewById<View>(R.id.btnSecondary).performClick()
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
