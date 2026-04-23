package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage

@Composable
internal fun AiMessageBubble(
    text: String,
    podcastUuid: String?,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
        modifier = modifier.fillMaxWidth(),
    ) {
        if (podcastUuid != null) {
            PodcastImage(
                uuid = podcastUuid,
                imageSize = 28.dp,
                cornerSize = 14.dp,
                elevation = null,
            )
        }
        Text(
            text = text,
            color = theme.aiBubbleText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(theme.aiBubble, AiBubbleShape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

@Composable
internal fun UserMessageBubble(
    text: String,
    theme: ChatTheme,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.End,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            color = theme.userBubbleText,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .background(theme.userBubble, UserBubbleShape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        )
    }
}

private val AiBubbleShape = RoundedCornerShape(
    topStart = 4.dp,
    topEnd = 16.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp,
)

private val UserBubbleShape = RoundedCornerShape(
    topStart = 16.dp,
    topEnd = 4.dp,
    bottomStart = 16.dp,
    bottomEnd = 16.dp,
)
