package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChatScreen(
    episodeUuid: String,
    onClickClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val theme = rememberChatTheme()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(theme.background),
    ) {
        IconButton(
            onClick = onClickClose,
            modifier = Modifier
                .padding(start = 8.dp, top = 8.dp)
                .offset(x = -4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(LR.string.close),
                tint = theme.iconButton,
            )
        }

        // TODO: Chat content
    }
}
