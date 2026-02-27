package au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import au.com.shiftyjelly.pocketcasts.views.R as VR
import androidx.test.espresso.action.ViewActions.click


class BottomNavigationPage {
    fun assertAccountVisible(): BottomNavigationPage {
        onView(withId(VR.id.navigation_profile)).check(matches(isDisplayed()))
        return this
    }

    fun tapAccount(): BottomNavigationPage {
        onView(withId(VR.id.navigation_profile)).perform(click())
        return this
    }
}
