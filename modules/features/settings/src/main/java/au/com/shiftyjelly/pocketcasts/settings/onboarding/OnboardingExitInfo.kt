package au.com.shiftyjelly.pocketcasts.settings.onboarding

sealed interface OnboardingExitInfo {
    data object Simple : OnboardingExitInfo

    data object ShowPlusPromotion : OnboardingExitInfo

    data object ShowReferralWelcome : OnboardingExitInfo

    data class ApplySuggestedFolders(
        val action: SuggestedFoldersAction,
    ) : OnboardingExitInfo
}
