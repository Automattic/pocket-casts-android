package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.IconButton
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvMoreButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(
        onClick = onClick,
        colors = TvButtonDefaults.iconButtonColors(),
        modifier = Modifier.size(48.dp).then(modifier),
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_ellipsis_horizontal),
            contentDescription = stringResource(LR.string.more_options),
            modifier = Modifier.size(20.dp),
        )
    }
}

@Preview
@Composable
private fun TvMoreButtonPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvMoreButton(onClick = {})
        }
    }
}
