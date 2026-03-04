package au.com.shiftyjelly.pocketcasts.account.onboarding.testutil

import android.accounts.AccountManager
import androidx.preference.PreferenceManager
import androidx.test.platform.app.InstrumentationRegistry
import au.com.shiftyjelly.pocketcasts.preferences.AccountConstants
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

class OnboardingStateResetRule : TestRule {

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                resetState()
                try {
                    base.evaluate()
                } finally {
                    resetState()
                }
            }
        }
    }

    private fun resetState() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val accountManager = AccountManager.get(context)
        accountManager.getAccountsByType(AccountConstants.ACCOUNT_TYPE).forEach { account ->
            accountManager.removeAccountExplicitly(account)
        }

        PreferenceManager.getDefaultSharedPreferences(context)
            .edit()
            .remove(COMPLETED_ONBOARDING_KEY)
            .commit()
    }

    companion object {
        private const val COMPLETED_ONBOARDING_KEY = "CompletedOnboardingKey"
    }
}
