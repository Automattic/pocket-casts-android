package au.com.shiftyjelly.pocketcasts.account.onboarding.testutil

import androidx.test.platform.app.InstrumentationRegistry

internal object OnboardingTestCredentials {
    private const val EMAIL_ARG = "onboardingEmail"
    private const val PASSWORD_ARG = "onboardingPassword"

    val email: String by lazy { requireArgument(EMAIL_ARG) }
    val password: String by lazy { requireArgument(PASSWORD_ARG) }

    private fun requireArgument(key: String): String {
        val value = InstrumentationRegistry.getArguments().getString(key).orEmpty().trim()
        check(value.isNotEmpty()) {
            "Missing instrumentation argument '$key'. " +
                "Configure onboarding-test.local.properties or pass " +
                "ONBOARDING_TEST_EMAIL/ONBOARDING_TEST_PASSWORD in CI, or pass " +
                "-Pandroid.testInstrumentationRunnerArguments.$key=<value>"
        }
        return value
    }
}
