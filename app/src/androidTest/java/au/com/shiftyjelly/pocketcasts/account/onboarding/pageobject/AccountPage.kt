package au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule

class AccountPage(
    private val rule: ComposeTestRule
) {
    fun checkAccountBottomIsVisible(): AccountPage {
        rule.onNode(hasText("Account")).assertIsDisplayed()
        return this
    }
    fun checkEmailIsVisible(expectedEmail: String): AccountPage {
        rule.onNode(hasText(expectedEmail)).assertIsDisplayed()
        return this
    }

}
