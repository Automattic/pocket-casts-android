package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ButtonElevation
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme

@Composable
fun BaseRowButton(
    text: String,
    modifier: Modifier = Modifier,
    includePadding: Boolean = true,
    enabled: Boolean = true,
    border: BorderStroke? = null,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    textColor: Color = MaterialTheme.theme.colors.primaryInteractive02,
    elevation: ButtonElevation? = ButtonDefaults.elevation(),
    fontFamily: FontFamily? = null,
    fontWeight: FontWeight? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
    cornerRadius: Dp = 12.dp,
    textVerticalPadding: Dp = 6.dp,
    @DrawableRes textIcon: Int? = null,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .then(if (includePadding) Modifier.padding(16.dp) else Modifier)
            .fillMaxWidth(),
    ) {
        Button(
            onClick = { onClick() },
            shape = RoundedCornerShape(cornerRadius),
            elevation = elevation,
            border = border,
            modifier = Modifier.fillMaxWidth().let { if (contentDescription != null) it.semantics { this.contentDescription = contentDescription } else it },
            colors = colors,
            enabled = enabled,
        ) {
            Box(Modifier.fillMaxWidth()) {
                if (leadingIcon != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(4.dp),
                    ) {
                        leadingIcon()
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (textIcon != null) {
                        Image(
                            painter = painterResource(textIcon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(textColor),
                            modifier = Modifier,
                        )
                    }
                    TextP40(
                        text = text,
                        fontFamily = fontFamily,
                        fontWeight = fontWeight,
                        modifier = Modifier
                            .padding(vertical = textVerticalPadding, horizontal = 6.dp),
                        textAlign = TextAlign.Center,
                        color = textColor,
                    )
                }
            }
        }
    }
}
