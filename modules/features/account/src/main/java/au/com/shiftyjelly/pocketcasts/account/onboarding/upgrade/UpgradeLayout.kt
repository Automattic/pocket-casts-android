package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

sealed class UpgradeLayout {
    data object Original : UpgradeLayout()
    data object Features : UpgradeLayout()
    data object Reviews : UpgradeLayout()
}
