package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessageType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal val WhatsNewMessageType.labelId
    get() = when (this) {
        WhatsNewMessageType.NewFeature -> LR.string.whats_new_category_new_feature
        WhatsNewMessageType.Tip -> LR.string.whats_new_category_tip
        WhatsNewMessageType.Announcement -> LR.string.whats_new_category_announcement
        WhatsNewMessageType.KnownIssue -> LR.string.whats_new_category_known_issue
        WhatsNewMessageType.Research -> LR.string.whats_new_category_research
    }

internal val WhatsNewMessageType.iconId
    get() = when (this) {
        WhatsNewMessageType.NewFeature -> IR.drawable.ic_filters_list
        WhatsNewMessageType.Tip -> IR.drawable.ic_sort
        WhatsNewMessageType.Announcement -> IR.drawable.ic_heart
        WhatsNewMessageType.KnownIssue -> IR.drawable.ic_warning
        WhatsNewMessageType.Research -> IR.drawable.ic_transcript_24
    }

internal val WhatsNewMessageType.gradient
    get() = when (this) {
        WhatsNewMessageType.NewFeature -> listOf(Color(0xFFF43769), Color(0xFFFB5246))
        WhatsNewMessageType.Tip -> listOf(Color(0xFF03A9F4), Color(0xFF50D0F1))
        WhatsNewMessageType.Announcement -> listOf(Color(0xFFC9522E), Color(0xFFB82E3C))
        WhatsNewMessageType.KnownIssue -> listOf(Color(0xFFFF9D3B), Color(0xFFEB6F4F))
        WhatsNewMessageType.Research -> listOf(Color(0xFF6B59C7), Color(0xFFBC4E7B))
    }
