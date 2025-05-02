package au.com.shiftyjelly.pocketcasts.podcasts.view.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.podcasts.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PodrollHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_author),
            contentDescription = null,
            tint = MaterialTheme.theme.colors.primaryIcon03,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        TextH60(
            text = stringResource(LR.string.recommended_by_creator),
            color = MaterialTheme.theme.colors.primaryText02,
        )
    }
}

@Preview
@Composable
fun PodrollHeaderPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: Theme.ThemeType,
) {
    AppThemeWithBackground(theme) {
        PodrollHeader()
    }
}
