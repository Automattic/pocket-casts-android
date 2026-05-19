package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.icons.IconChat
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChatBanner(
    modifier: Modifier = Modifier,
    colors: ChatBannerColors = ChatBannerColors.default(),
    dimensions: ChatBannerDimensions = ChatBannerDimensions.default(),
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(dimensions.itemSpacing),
        modifier = Modifier
            .shadow(elevation = dimensions.elevation, shape = BackgroundShape)
            .background(colors.background, BackgroundShape)
            .then(modifier)
            .padding(vertical = dimensions.verticalPadding, horizontal = dimensions.horizontalPadding),
    ) {
        Icon(
            imageVector = IconChat,
            contentDescription = null,
            tint = colors.leadingIcon,
            modifier = Modifier.size(dimensions.iconSize),
        )
        Text(
            text = stringResource(LR.string.episode_chat),
            color = MaterialTheme.theme.colors.primaryText01,
            fontSize = dimensions.textSize,
            lineHeight = dimensions.textLineHeight,
        )
        Spacer(
            modifier = Modifier.weight(1f),
        )
        Image(
            painter = painterResource(IR.drawable.ic_chevron_right),
            colorFilter = ColorFilter.tint(colors.trailingIcon),
            contentDescription = null,
            modifier = Modifier.size(dimensions.iconSize),
        )
    }
}

data class ChatBannerColors(
    val background: Color,
    val leadingIcon: Color,
    val trailingIcon: Color,
) {
    companion object {
        @Composable
        fun default() = ChatBannerColors(
            background = MaterialTheme.theme.colors.primaryUi04,
            leadingIcon = MaterialTheme.theme.colors.primaryIcon02,
            trailingIcon = MaterialTheme.theme.colors.primaryIcon02,
        )
    }
}

data class ChatBannerDimensions(
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val elevation: Dp,
    val textSize: TextUnit,
    val textLineHeight: TextUnit,
    val iconSize: Dp,
    val itemSpacing: Dp,
) {
    companion object {
        fun default() = ChatBannerDimensions(
            horizontalPadding = 12.dp,
            verticalPadding = 12.dp,
            elevation = 2.dp,
            textSize = 16.sp,
            textLineHeight = 24.sp,
            iconSize = 20.dp,
            itemSpacing = 12.dp,
        )

        fun compact() = ChatBannerDimensions(
            horizontalPadding = 16.dp,
            verticalPadding = 10.dp,
            elevation = 0.dp,
            textSize = 14.sp,
            textLineHeight = 20.sp,
            iconSize = 18.dp,
            itemSpacing = 14.dp,
        )
    }
}

private val BackgroundShape = RoundedCornerShape(8.dp)

@Preview
@Composable
private fun ChatBannerPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) theme: ThemeType,
) {
    AppThemeWithBackground(theme) {
        ChatBanner(
            modifier = Modifier.padding(16.dp),
        )
    }
}
