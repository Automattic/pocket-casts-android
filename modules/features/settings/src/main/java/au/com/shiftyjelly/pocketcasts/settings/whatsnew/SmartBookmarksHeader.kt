package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun SmartBookmarksHeader(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = IR.drawable.ic_plus_feature_bookmark),
        contentDescription = null,
        modifier = modifier
            .padding(bottom = 16.dp)
            .size(96.dp),
    )
}
