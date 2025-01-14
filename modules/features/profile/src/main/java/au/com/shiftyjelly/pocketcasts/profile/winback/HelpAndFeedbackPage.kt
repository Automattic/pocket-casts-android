package au.com.shiftyjelly.pocketcasts.profile.winback

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun HelpAndFeedbackPage(
    onGoBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF9BD3CB))
            .padding(16.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .background(Color(0xFF129A7D))
                .clickable(onClick = onGoBack)
                .padding(16.dp),
        ) {
            Text(
                text = "Back",
                color = Color.White,
            )
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .height(80.dp)
                .fillMaxWidth(),
        ) {
            Text(
                text = "Help & Feedback",
                color = Color.Black,
            )
        }
    }
}
