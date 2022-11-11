package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun OutlinedRowButton(
    text: String,
    modifier: Modifier = Modifier,
    includePadding: Boolean = true,
    enabled: Boolean = true,
    border: BorderStroke = BorderStroke(1.dp, MaterialTheme.colors.onSurface),
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    textColor: Color = MaterialTheme.theme.colors.primaryInteractive01,
    @DrawableRes leadingIcon: Int? = null,
    onClick: () -> Unit
) {
    RowButton(
        text = text,
        modifier = modifier,
        includePadding = includePadding,
        enabled = enabled,
        border = border,
        colors = colors,
        textColor = textColor,
        leadingIcon = leadingIcon,
        onClick = onClick
    )
}

@Composable
fun RowButton(
    text: String,
    modifier: Modifier = Modifier,
    includePadding: Boolean = true,
    enabled: Boolean = true,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    textColor: Color = MaterialTheme.theme.colors.primaryInteractive02,
    @DrawableRes leadingIcon: Int? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .then(if (includePadding) Modifier.padding(16.dp) else Modifier)
            .fillMaxWidth()
    ) {
        Button(
            onClick = { onClick() },
            shape = RoundedCornerShape(12.dp),
            border = border,
            modifier = Modifier.fillMaxWidth(),
            colors = colors,
            enabled = enabled
        ) {
            Box(Modifier.fillMaxWidth()) {
                if (leadingIcon != null) {
                    Image(
                        painter = painterResource(leadingIcon),
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(4.dp)
                    )
                }
                Text(
                    text = text,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .padding(6.dp)
                        .align(Alignment.Center),
                    textAlign = TextAlign.Center,
                    color = textColor
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RowButtonLightPreview() {
    AppTheme(Theme.ThemeType.LIGHT) {
        RowButton(
            text = "Accept",
            onClick = {}
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
fun RowButtonDarkPreview() {
    AppTheme(Theme.ThemeType.DARK) {
        RowButton(
            text = "Accept",
            onClick = {}
        )
    }
}
