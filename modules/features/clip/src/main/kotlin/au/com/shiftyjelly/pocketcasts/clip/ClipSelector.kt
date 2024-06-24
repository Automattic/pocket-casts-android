package au.com.shiftyjelly.pocketcasts.clip

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import com.airbnb.android.showkase.annotation.ShowkaseComposable

@Composable
fun ClipSelector(
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color.Magenta),
    ) {
        TextH30(
            text = "SELECTOR PLACEHOLDER",
            color = Color.White,
        )
    }
}

@ShowkaseComposable(name = "ClipSelector", group = "Clip")
@Preview(name = "ClipSelector")
@Composable
fun ClipSelectorPreview() = ClipSelector()
