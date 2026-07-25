package au.com.shiftyjelly.pocketcasts

import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutomotiveSettingsActivityRecreateTest {

    @Test
    fun backAfterRecreateDoesNotLeaveOverlappingFragments() {
        ActivityScenario.launch(AutomotiveSettingsActivity::class.java).use { scenario ->
            // Settings -> About -> Acknowledgements
            scenario.onActivity { activity ->
                activity.addFragment(AutomotiveAboutFragment())
                activity.addFragment(AutomotiveLicensesFragment())
                activity.supportFragmentManager.executePendingTransactions()
            }

            // Simulate the configuration change (e.g. rotation to portrait)
            scenario.recreate()

            // Tap Back
            scenario.onActivity { activity ->
                activity.supportFragmentManager.popBackStack()
                activity.supportFragmentManager.executePendingTransactions()

                val frameMain = activity.findViewById<FrameLayout>(R.id.frameMain)
                assertEquals("frameMain should hold a single fragment after back", 1, frameMain.childCount)

                val visible = activity.supportFragmentManager.findFragmentById(R.id.frameMain)
                assertTrue("expected the About fragment, not an orphaned settings fragment", visible is AutomotiveAboutFragment)
            }
        }
    }
}
