package au.com.shiftyjelly.pocketcasts.settings.onboarding

import com.automattic.eventhorizon.OnboardingSourceType

enum class OnboardingUpgradeSource(
    val analyticsValue: OnboardingSourceType,
) {
    ACCOUNT_DETAILS(
        analyticsValue = OnboardingSourceType.AccountDetails,
    ),
    APPEARANCE(
        analyticsValue = OnboardingSourceType.Appearance,
    ),
    BANNER_AD(
        analyticsValue = OnboardingSourceType.BannerAd,
    ),
    ICONS(
        analyticsValue = OnboardingSourceType.Icons,
    ),
    THEMES(
        analyticsValue = OnboardingSourceType.Themes,
    ),
    BOOKMARKS(
        analyticsValue = OnboardingSourceType.Bookmarks,
    ),
    BOOKMARKS_SHELF_ACTION(
        analyticsValue = OnboardingSourceType.BookmarksShelfAction,
    ),
    END_OF_YEAR(
        analyticsValue = OnboardingSourceType.EndOfYear,
    ),
    FILES(
        analyticsValue = OnboardingSourceType.Files,
    ),
    FOLDERS(
        analyticsValue = OnboardingSourceType.Folders,
    ),
    SUGGESTED_FOLDERS(
        analyticsValue = OnboardingSourceType.SuggestedFolders,
    ),
    FOLDERS_PODCAST_SCREEN(
        analyticsValue = OnboardingSourceType.FoldersPodcastScreen,
    ),
    HEADPHONE_CONTROLS_SETTINGS(
        analyticsValue = OnboardingSourceType.HeadphoneControlsSettings,
    ),
    LOGIN(
        analyticsValue = OnboardingSourceType.Login,
    ),
    LOGIN_PLUS_PROMOTION(
        analyticsValue = OnboardingSourceType.LoginPlusPromotion,
    ),
    OVERFLOW_MENU(
        analyticsValue = OnboardingSourceType.OverflowMenu,
    ),
    PLUS_DETAILS(
        analyticsValue = OnboardingSourceType.PlusDetails,
    ),
    PROFILE(
        analyticsValue = OnboardingSourceType.Profile,
    ),
    RECOMMENDATIONS(
        analyticsValue = OnboardingSourceType.Recommendations,
    ),
    SKIP_CHAPTERS(
        analyticsValue = OnboardingSourceType.SkipChapters,
    ),
    SETTINGS(
        analyticsValue = OnboardingSourceType.Settings,
    ),
    SLUMBER_STUDIOS(
        analyticsValue = OnboardingSourceType.SlumberStudios,
    ),
    UP_NEXT_SHUFFLE(
        analyticsValue = OnboardingSourceType.UpNextShuffle,
    ),
    GENERATED_TRANSCRIPTS(
        analyticsValue = OnboardingSourceType.GeneratedTranscripts,
    ),
    DEEP_LINK(
        analyticsValue = OnboardingSourceType.DeepLink,
    ),
    FINISHED_ONBOARDING(
        analyticsValue = OnboardingSourceType.AccountCreated,
    ),
    UNKNOWN(
        analyticsValue = OnboardingSourceType.Unknown,
    ),
}
