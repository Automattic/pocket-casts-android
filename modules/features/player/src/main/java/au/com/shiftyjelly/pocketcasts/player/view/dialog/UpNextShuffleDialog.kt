package au.com.shiftyjelly.pocketcasts.player.view.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.GradientRowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.extensions.plusBackgroundBrush
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun UpNextShuffleDialog(
    modifier: Modifier = Modifier,
    onTryPlusNowClick: () -> Unit,
    onNotNowClick: () -> Unit,

) {
    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier.fillMaxWidth(),
        ) {
            Image(
                painterResource(IR.drawable.swipe_affordance),
                contentDescription = stringResource(LR.string.swipe_affordance_content_description),
                modifier = modifier
                    .width(56.dp)
                    .padding(top = 8.dp, bottom = 35.dp),
            )

            Image(
                painterResource(IR.drawable.shuffle_plus_feature_icon),
                contentDescription = stringResource(LR.string.up_next_shuffle_button_content_description),
                modifier = modifier
                    .padding(top = 8.dp, bottom = 12.dp)
                    .size(96.dp)
            )

            TextH30(
                text = stringResource(LR.string.up_next_shuffle_your_episodes_with_plus),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.W600,
                modifier = modifier.padding(bottom = 12.dp, start = 21.dp, end = 21.dp),
            )

            TextH50(
                text = stringResource(LR.string.up_next_shuffle_unlock_feature_description),
                fontWeight = FontWeight.W500,
                color = MaterialTheme.theme.colors.primaryText02,
                textAlign = TextAlign.Center,
                modifier = modifier.padding(bottom = 12.dp, start = 21.dp, end = 21.dp),
            )

            GradientRowButton(
                primaryText = stringResource(LR.string.try_plus_now),
                textColor = Color.Black,
                gradientBackgroundColor = plusBackgroundBrush,
                modifier = modifier.padding(bottom = 12.dp).padding(horizontal = 20.dp),
                onClick = { onTryPlusNowClick.invoke() },
            )

            RowOutlinedButton(
                text = stringResource(LR.string.not_now),
                onClick = { onNotNowClick.invoke() },
                includePadding = false,
                modifier = modifier.padding(bottom = 12.dp).padding(horizontal = 20.dp),
                border = null,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryText01),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewUpNextShuffleDialog(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        UpNextShuffleDialog(onTryPlusNowClick = {}, onNotNowClick = {})
    }
}
