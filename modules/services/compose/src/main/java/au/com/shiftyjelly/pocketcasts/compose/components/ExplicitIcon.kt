package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ExplicitIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.theme.colors.primaryText02,
    size: Dp = with(LocalDensity.current) { 16.sp.toDp() },
) {
    Icon(
        painter = painterResource(IR.drawable.explicit),
        contentDescription = stringResource(LR.string.explicit),
        tint = tint,
        modifier = modifier.size(size),
    )
}
