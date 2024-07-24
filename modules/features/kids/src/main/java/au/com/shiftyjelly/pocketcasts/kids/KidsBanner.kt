package au.com.shiftyjelly.pocketcasts.kids

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.images.R.drawable.ic_close
import au.com.shiftyjelly.pocketcasts.images.R.drawable.kids_profile_face
import au.com.shiftyjelly.pocketcasts.localization.R.string
import au.com.shiftyjelly.pocketcasts.localization.R.string.close
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun KidsProfileCard(
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.theme.colors.primaryUi01Active, shape = RoundedCornerShape(8.dp)),
    ) {
        Image(
            colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryUi05Selected),
            painter = painterResource(id = ic_close),
            contentDescription = stringResource(id = close),
            modifier = Modifier
                .padding(11.dp)
                .size(22.dp)
                .align(Alignment.TopEnd)
                .clickable { onDismiss() },
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
                ) {
                    TextH40(
                        text = stringResource(string.kids_profile),
                        fontWeight = FontWeight.W500,
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .background(MaterialTheme.theme.colors.primaryField01Active, shape = RoundedCornerShape(4.dp))
                            .width(40.dp)
                            .height(22.dp),
                    ) {
                        Text(
                            text = stringResource(string.soon_label),
                            color = MaterialTheme.theme.colors.primaryUi05Selected,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.W600,
                        )
                    }
                }
                TextH70(
                    text = stringResource(string.kids_profile_banner_description),
                    color = MaterialTheme.theme.colors.primaryUi05Selected,
                    modifier = Modifier
                        .padding(start = 24.dp, bottom = 8.dp)
                        .width(200.dp),
                    fontWeight = FontWeight.W600,
                )

                ClickableText(
                    text = AnnotatedString(text = stringResource(string.request_early_access)),
                    onClick = { },
                    modifier = Modifier.padding(start = 24.dp, bottom = 14.dp),
                    style = TextStyle(
                        color = MaterialTheme.theme.colors.primaryIcon01,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.W600,
                        textAlign = TextAlign.Start,
                    ),
                )
            }
        }

        Image(
            painter = painterResource(id = kids_profile_face),
            contentDescription = stringResource(id = string.kids_profile_face_image),
            modifier = Modifier
                .size(116.dp)
                .padding(top = 36.dp)
                .align(Alignment.BottomEnd),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewKidsProfileCard(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        KidsProfileCard(
            onDismiss = {},
        )
    }
}
