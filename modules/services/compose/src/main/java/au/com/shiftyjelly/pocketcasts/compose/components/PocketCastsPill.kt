package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PocketCastsPill(
    modifier: Modifier = Modifier,
    disableScale: Boolean = false,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
        .background(Color(0xFFF43E37), RoundedCornerShape(24.dp))
        .defaultMinSize(minHeight = 24.dp)
        .padding(start = 4.dp, end = 8.dp),
) {
    Image(
        painter = painterResource(id = IR.drawable.ic_logo_foreground),
        contentDescription = null,
        modifier = Modifier.size(18.dp),
    )
    Spacer(
        modifier = Modifier.width(8.dp),
    )
    TextH70(
        text = stringResource(id = LR.string.pocket_casts),
        color = Color.White,
        disableScale = disableScale,
    )
}

@Composable
fun PocketCastsLogo(
    modifier: Modifier = Modifier,
) = Image(
    painter = painterResource(id = IR.drawable.ic_logo_foreground),
    contentDescription = null,
    modifier = Modifier
        .background(Color(0xFFF43E37), CircleShape)
        .size(24.dp)
        .then(modifier),
)

@Preview
@Composable
private fun PocketCastsPillPreview() = Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
        .background(Color.White)
        .size(180.dp, 90.dp),
) {
    PocketCastsPill()
}

@Preview
@Composable
private fun PocketCastsLogoPreview() = Box(
    contentAlignment = Alignment.Center,
    modifier = Modifier
        .background(Color.White)
        .size(32.dp, 32.dp),
) {
    PocketCastsLogo()
}
