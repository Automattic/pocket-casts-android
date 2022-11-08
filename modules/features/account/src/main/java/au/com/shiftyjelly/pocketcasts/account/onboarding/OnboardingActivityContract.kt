package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContract

class OnboardingActivityContract : ActivityResultContract<Intent, OnboardingActivityContract.OnboardingFinish?>() {

    override fun createIntent(context: Context, input: Intent): Intent = input

    override fun parseResult(resultCode: Int, intent: Intent?): OnboardingFinish? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getSerializableExtra(FINISH_KEY, OnboardingFinish::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent?.getSerializableExtra(FINISH_KEY) as? OnboardingFinish
        }

    enum class OnboardingFinish {
        CompletedOnboarding,
        AbortedOnboarding
    }

    companion object {
        const val FINISH_KEY = "onboarding_finish"
    }
}
