package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.compose.theme
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
        WhatsNewMessageType.NewFeature -> IR.drawable.ic_whats_new_new_feature
        WhatsNewMessageType.Tip -> IR.drawable.ic_whats_new_tip
        WhatsNewMessageType.Announcement -> IR.drawable.ic_whats_new_announcement
        WhatsNewMessageType.KnownIssue -> IR.drawable.ic_warning
        WhatsNewMessageType.Research -> IR.drawable.ic_transcript_24
    }

internal val WhatsNewMessageType.gradient: Brush
    @Composable get() {
        val colors = MaterialTheme.theme.colors
        return when (this) {
            WhatsNewMessageType.NewFeature -> Brush.horizontalGradient(listOf(colors.gradient05A, colors.gradient05E))
            WhatsNewMessageType.Tip -> Brush.linearGradient(listOf(colors.gradient03A, colors.gradient03E))
            WhatsNewMessageType.Announcement -> Brush.linearGradient(listOf(colors.gradient02A, colors.gradient02E))
            WhatsNewMessageType.KnownIssue -> Brush.linearGradient(listOf(Color(0xFFFF9D3B), Color(0xFFEB6F4F)))
            WhatsNewMessageType.Research -> Brush.linearGradient(listOf(colors.gradient04A, colors.gradient04E))
        }
    }
