package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessageType
import com.automattic.eventhorizon.WhatsNewActionType
import com.automattic.eventhorizon.WhatsNewMessageType as AnalyticsMessageType

internal val WhatsNewMessageType.analyticsValue
    get() = when (this) {
        WhatsNewMessageType.NewFeature -> AnalyticsMessageType.NewFeature
        WhatsNewMessageType.Tip -> AnalyticsMessageType.Tip
        WhatsNewMessageType.Announcement -> AnalyticsMessageType.Announcement
        WhatsNewMessageType.KnownIssue -> AnalyticsMessageType.KnownIssue
        WhatsNewMessageType.Research -> AnalyticsMessageType.Research
    }

internal val WhatsNewActionEvent.analyticsValue
    get() = when (this) {
        WhatsNewActionEvent.OpenPodcasts -> WhatsNewActionType.OpenPodcasts
        WhatsNewActionEvent.OpenDiscover -> WhatsNewActionType.OpenDiscover
        WhatsNewActionEvent.OpenUpNext -> WhatsNewActionType.OpenUpNext
        WhatsNewActionEvent.OpenPlaylists -> WhatsNewActionType.OpenPlaylists
        WhatsNewActionEvent.OpenProfile -> WhatsNewActionType.OpenProfile
        WhatsNewActionEvent.OpenSettings -> WhatsNewActionType.OpenSettings
        WhatsNewActionEvent.OpenUpsell -> WhatsNewActionType.OpenUpsell
    }
