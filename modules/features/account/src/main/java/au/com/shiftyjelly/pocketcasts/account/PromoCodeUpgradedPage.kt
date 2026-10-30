package au.com.shiftyjelly.pocketcasts.account

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.ViewMaterialButton
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun PromoCodeUpgradedPage(
    codeDescription: String,
    @DrawableRes logoRes: Int,
    onDoneClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .padding(horizontal = 16.dp)
            .clipToBounds(),
    ) {
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(width = 48.dp, height = 4.dp)
                .background(MaterialTheme.theme.colors.playerContrast01, RoundedCornerShape(3.dp)),
        )
        DrawableImage(
            drawableId = logoRes,
            contentDescription = null,
            modifier = Modifier
                .padding(top = 8.dp)
                .size(width = 156.dp, height = 148.dp),
        )
        TextViewText(
            text = stringResource(LR.string.profile_promo_upgraded),
            fontSize = 22.sp,
            color = MaterialTheme.theme.colors.primaryText01,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth(),
        )
        TextViewText(
            text = "$codeDescription\n${stringResource(LR.string.profile_promo_upgraded_welcome)}",
            fontSize = 18.sp,
            color = MaterialTheme.theme.colors.primaryText02,
            lineSpacingMultiplier = 1.2f,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth(),
        )
        ViewMaterialButton(
            text = stringResource(LR.string.done),
            onClick = onDoneClick,
            backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
            textColor = MaterialTheme.theme.colors.primaryInteractive02,
            fontSize = 18.sp,
            letterSpacing = 0.02.em,
            cornerRadius = 12.dp,
            verticalPadding = 16.dp,
            modifier = Modifier
                .padding(top = 40.dp, bottom = 8.dp)
                .fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun PromoCodeUpgradedPagePreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        PromoCodeUpgradedPage(
            codeDescription = "Three months of Pocket Casts Plus free",
            logoRes = IR.drawable.plus_logo_vertical_white,
            onDoneClick = {},
        )
    }
}
