package au.com.shiftyjelly.pocketcasts.preferences.model

enum class AppReviewReason(
    val analyticsValue: String,
) {
    ThirdEpisodeCompleted(
        analyticsValue = "third_episode_completed",
    ),
    EpisodeStarred(
        analyticsValue = "episode_starred",
    ),
    ShowRated(
        analyticsValue = "show_rated",
    ),
    FilterCreated(
        analyticsValue = "filter_created",
    ),
    PlusUpgraded(
        analyticsValue = "plus_upgraded",
    ),
    FolderCreated(
        analyticsValue = "folder_created",
    ),
    BookmarkCreated(
        analyticsValue = "bookmark_created",
    ),
    CustomThemeSet(
        analyticsValue = "custom_theme_set",
    ),
    ReferralShared(
        analyticsValue = "referral_shared",
    ),
    PlaybackShared(
        analyticsValue = "playback_shared",
    ),
    DevelopmentTrigger(
        analyticsValue = "development_trigger",
    ),
    ;

    companion object {
        fun fromValue(value: String) = entries.find { it.analyticsValue == value }
    }
}
