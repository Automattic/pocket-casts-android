package au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class AccountPage(
    private val rule: ComposeTestRule,
    context: Context
) {
    private val accountText = context.getString(LR.string.account)

    fun checkAccountBottomIsVisible(): AccountPage {
        rule.onNode(hasText(accountText)).assertIsDisplayed()
        return this
    }
    fun checkEmailIsVisible(expectedEmail: String): AccountPage {
        rule.onNode(hasText(expectedEmail)).assertIsDisplayed()
        return this
    }

}
