package au.com.shiftyjelly.pocketcasts.podcasts.view.compose.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.theme

@Composable
fun LargePageTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        fontSize = 31.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.theme.colors.primaryText01,
        modifier = modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
    )
}
