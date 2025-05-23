package au.com.shiftyjelly.pocketcasts.account.onboarding

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.IntentCompat
import au.com.shiftyjelly.pocketcasts.settings.onboarding.SuggestedFoldersAction
import kotlinx.parcelize.Parcelize

class OnboardingActivityContract : ActivityResultContract<Intent, OnboardingActivityContract.OnboardingFinish?>() {

    override fun createIntent(context: Context, input: Intent): Intent = input

    override fun parseResult(resultCode: Int, intent: Intent?): OnboardingFinish? {
        return intent?.let { IntentCompat.getParcelableExtra(it, FINISH_KEY, OnboardingFinish::class.java) }
    }

    sealed interface OnboardingFinish : Parcelable {
        @Parcelize
        data object Done : OnboardingFinish

        @Parcelize
        data object DoneGoToDiscover : OnboardingFinish

        @Parcelize
        data object DoneShowPlusPromotion : OnboardingFinish

        @Parcelize
        data object DoneShowWelcomeInReferralFlow : OnboardingFinish

        @Parcelize
        data class DoneApplySuggestedFolders(
            val action: SuggestedFoldersAction,
        ) : OnboardingFinish
    }

    companion object {
        const val FINISH_KEY = "onboarding_finish"
    }
}
