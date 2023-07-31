package au.com.shiftyjelly.pocketcasts.ui

import android.Manifest
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.GeneralClickAction
import androidx.test.espresso.action.GeneralLocation
import androidx.test.espresso.action.Press
import androidx.test.espresso.action.Tap
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withClassName
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.adevinta.android.barista.interaction.BaristaClickInteractions.clickOn
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.anyOf
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import timber.log.Timber
import java.io.File
import au.com.shiftyjelly.pocketcasts.discover.R as DR
import au.com.shiftyjelly.pocketcasts.podcasts.R as PR
import au.com.shiftyjelly.pocketcasts.views.R as VR

private const val RUN_SCREENSHOTS = false

@LargeTest
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule var activityRule: ActivityScenarioRule<MainActivity> = ActivityScenarioRule(MainActivity::class.java)
    @get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()
    @get:Rule var permissionRule = GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    lateinit var device: UiDevice

    private var screenShotCount = 1

    @Before
    fun setup() {
        device = UiDevice.getInstance(getInstrumentation())
    }

    @Test
    fun mainActivityTest() {
        if (!RUN_SCREENSHOTS) {
            return
        }

        for (theme in Theme.ThemeType.values()) {
            takeScreenshots()
        }
    }

    private fun takeScreenshots() {
        // open the podcasts tab
        doubleClickOn(VR.id.navigation_podcasts)
        sleepSecs(1)
        takeScreenshot()

        // open the podcast grid options bottom sheet
        clickOn(PR.id.more_options)
        sleepSecs(2)
        takeScreenshot()

        // click the top options dialog option
        composeOptionsDialog(index = 0).performClick()
        sleepSecs(2)
        takeScreenshot()

        // close the dialog
        pressBack()
        sleepSecs(2)

        // open the filters tab (double tap to make sure the top level page is opened)
        doubleClickOn(VR.id.navigation_filters)
        sleepSecs(1)
        takeScreenshot()

        // open New Releases filter
        clickOn("New Releases")
        sleepSecs(1)
        takeScreenshot()

        // open the discover tab
        doubleClickOn(VR.id.navigation_discover)
        sleepSecs(4)
        takeScreenshot()

        // click show all in the first list
        discoverFirstShowAll().perform(click())
        sleepSecs(4)
        takeScreenshot()

        device.pressBack()

        // open featured podcast page
        discoverFeaturedViewClick()
        sleepSecs(5)
        takeScreenshot()

        // subscribe to the podcast
        clickOn("Subscribe")
        sleepSecs(5)
        takeScreenshot()

        // open settings page
        clickOn(PR.id.settings)
        sleepSecs(1)
        takeScreenshot()

        // scroll to show Unsubscribe button
        scrollToPosition(id = androidx.preference.R.id.recycler_view, position = 8)

        // click unsubscribe from the podcast
        clickOn("Unsubscribe")
        sleepSecs(2)
        takeScreenshot()

        // confirm unsubscribe in the dialog
        clickOn("Unsubscribe")
        sleepSecs(2)

        // open the profile tab
        clickOn(VR.id.navigation_profile)
        sleepSecs(1)
        takeScreenshot()

        // open stats page
        clickOnContentDescription("Stats")
        sleepSecs(1)
        takeScreenshot()

        // go back to the profile page
        device.pressBack()

        // open downloads page
        clickOnContentDescription("Downloads")
        sleepSecs(1)
        takeScreenshot()

        // go back to the profile page
        device.pressBack()

        // open files page
        clickOnContentDescription("Files")
        sleepSecs(1)
        takeScreenshot()

        // go back to the profile page
        device.pressBack()

        // open settings page
        clickOnContentDescription("Settings")
        sleepSecs(1)
        takeScreenshot()

        // open the general settings page
        clickOn("General")
        sleepSecs(1)
        takeScreenshot()

        // open the podcasts tab
        clickOn(VR.id.navigation_podcasts)
        sleepSecs(2)
        longPressToolbar()
        sleepSecs(5)
    }

    private fun doubleClickOn(textId: Int) {
        clickOn(textId)
        clickOn(textId)
    }

    private fun scrollToPosition(id: Int, position: Int) {
        onView(withId(id)).perform(RecyclerViewActions.scrollToPosition<RecyclerView.ViewHolder>(position))
    }

    private fun clickOnContentDescription(text: String) {
        onView(anyOf(withContentDescription(text))).perform(click())
    }

    private fun clickOnUp() {
        onView(anyOf(withContentDescription("Back"))).perform(click())
    }

    private fun composeOptionsDialog(index: Int): SemanticsNodeInteraction {
        return composeTestRule.onNodeWithTag("option_$index")
    }

    private fun discoverFirstShowAll(): ViewInteraction {
        return onView(
            allOf(
                withId(DR.id.btnShowAll),
                childAtPosition(childAtPosition(withClassName(`is`("android.widget.LinearLayout")), 0), 1),
                isDisplayed()
            )
        )
    }

    private fun longPressToolbar() {
        try {
            onView(allOf(withId(PR.id.toolbar), isDisplayed())).perform(
                GeneralClickAction(
                    Tap.LONG,
                    GeneralLocation.CENTER,
                    Press.FINGER,
                    InputDevice.SOURCE_UNKNOWN,
                    MotionEvent.BUTTON_PRIMARY
                )
            )
        } catch (t: Throwable) {
            // ignore error
        }
    }

    private fun discoverFeaturedViewClick() {
        val recyclerView = onView(
            allOf(
                withId(DR.id.rowRecyclerView),
                withParent(withId(DR.id.carousel))
            )
        )
        recyclerView.perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click()))
    }

    private fun sleepSecs(secs: Int) {
        SystemClock.sleep(secs * 1000L)
    }

    @Suppress("DEPRECATION")
    private fun takeScreenshot() {
        val dir = File(getInstrumentation().targetContext.cacheDir, "app_screenshots")
        dir.mkdirs()
        val file = File(dir, "${screenShotCount.toString().padStart(2, '0')}.jpg")
        screenShotCount += 1
        device.takeScreenshot(file)
        Timber.i("Taken screenshot to ${file.absolutePath}")
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>,
        position: Int
    ): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent) && view == parent.getChildAt(position)
            }
        }
    }
}
