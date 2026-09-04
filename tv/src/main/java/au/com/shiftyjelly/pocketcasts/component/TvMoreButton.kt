package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvMoreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    buttonSize: Dp = 36.dp,
    iconSize: Dp = 15.dp,
    colors: ButtonColors = TvButtonDefaults.iconButtonColors(),
) {
    IconButton(
        onClick = onClick,
        colors = colors,
        modifier = Modifier.size(buttonSize).then(modifier),
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_ellipsis_horizontal),
            contentDescription = stringResource(LR.string.more_options),
            modifier = Modifier.size(iconSize),
        )
    }
}

@Preview
@Composable
private fun TvMoreButtonPreview() {
    TvTheme {
        TvMoreButton(onClick = {})
    }
}
