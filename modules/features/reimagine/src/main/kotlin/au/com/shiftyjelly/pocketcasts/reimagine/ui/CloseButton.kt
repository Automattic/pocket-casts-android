package au.com.shiftyjelly.pocketcasts.reimagine.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun CloseButton(
    shareColors: ShareColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Image(
    painter = painterResource(IR.drawable.ic_close_sheet),
    contentDescription = stringResource(LR.string.close),
    colorFilter = ColorFilter.tint(shareColors.onContainerSecondary),
    modifier = modifier
        .clickable(onClick = onClick)
        .clip(CircleShape)
        .background(shareColors.container),
)
