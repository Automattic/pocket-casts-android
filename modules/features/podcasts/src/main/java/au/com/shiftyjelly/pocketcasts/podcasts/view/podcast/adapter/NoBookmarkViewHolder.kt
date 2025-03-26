package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast.adapter

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.recyclerview.widget.RecyclerView
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.EmptyState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class NoBookmarkViewHolder(
    private val composeView: ComposeView,
    private val theme: Theme,
    private val onHeadsetSettingsClicked: () -> Unit,
) : RecyclerView.ViewHolder(composeView) {
    fun bind() {
        composeView.setContent {
            AppTheme(theme.activeTheme) {
                EmptyState(
                    title = stringResource(LR.string.bookmarks_empty_state_title),
                    subtitle = stringResource(LR.string.bookmarks_paid_user_empty_state_message),
                    iconResourcerId = IR.drawable.ic_bookmark,
                    buttonText = stringResource(LR.string.bookmarks_headphone_settings),
                    onButtonClick = {
                        onHeadsetSettingsClicked()
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .padding(vertical = 8.dp),
                )
            }
        }
    }
}
