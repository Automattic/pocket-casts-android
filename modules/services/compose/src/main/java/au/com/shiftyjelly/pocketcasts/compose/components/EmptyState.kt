package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun EmptyState(
    title: String,
    subtitle: String,
    iconResourcerId: Int,
    modifier: Modifier = Modifier,
    buttonText: String? = null,
    onButtonClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(id = iconResourcerId),
            contentDescription = null,
            modifier = Modifier
                .padding(bottom = 16.dp)
                .size(32.dp),
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryIcon02),
        )

        TextH30(
            text = title,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W500,
            modifier = Modifier
                .padding(bottom = 16.dp),
        )

        TextP40(
            text = subtitle,
            textAlign = TextAlign.Center,
            color = MaterialTheme.theme.colors.primaryText02,
            fontSize = 15.sp,
            fontWeight = FontWeight.W400,
            modifier = Modifier
                .padding(bottom = 16.dp),
        )

        buttonText?.let {
            RowButton(
                text = it,
                onClick = { onButtonClick() },
                includePadding = false,
                textColor = MaterialTheme.theme.colors.primaryInteractive02,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.theme.colors.primaryInteractive01,
                ),
                modifier = Modifier.widthIn(max = 330.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStatePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        EmptyState(
            title = "Time to add some podcasts",
            subtitle = "Discover and subscribe to your favorite podcasts.",
            iconResourcerId = IR.drawable.ic_podcasts,
            buttonText = "Discover",
            onButtonClick = { },
        )
    }
}
