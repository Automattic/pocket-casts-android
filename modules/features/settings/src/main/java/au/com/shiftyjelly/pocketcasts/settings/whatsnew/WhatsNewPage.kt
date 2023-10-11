package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import android.content.res.Configuration
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun WhatsNewPage(
    title: String,
    message: String,
    confirmButtonTitle: String,
    closeButtonTitle: String? = null,
    header: @Composable () -> Unit,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
) {

    var closing by remember { mutableStateOf(false) }
    val targetAlpha = if (closing) 0f else 0.66f
    val scrimAlpha: Float by animateFloatAsState(
        targetValue = targetAlpha,
        finishedListener = { onClose() }
    )

    val performClose = {
        closing = true
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .background(Color.Black.copy(alpha = scrimAlpha))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = performClose,
            )
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp)
            .fillMaxSize()
    ) {
        Column(Modifier.background(MaterialTheme.theme.colors.primaryUi01)) {

            // Hide the header graphic if the phone is in landscape mode so there is room for the text
            if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_PORTRAIT) {
                header()
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(all = 16.dp),
            ) {

                TextH20(
                    text = title,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.theme.colors.primaryText01,
                )

                Spacer(modifier = Modifier.height(16.dp))

                TextP40(
                    text = message,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.theme.colors.primaryText02,
                )

                Spacer(modifier = Modifier.height(16.dp))

                RowButton(
                    text = confirmButtonTitle,
                    onClick = onConfirm,
                    includePadding = false,
                    modifier = Modifier
                        .fillMaxWidth()
                )

                closeButtonTitle?.let {
                    Spacer(modifier = Modifier.height(8.dp))

                    RowTextButton(
                        text = closeButtonTitle,
                        fontSize = 15.sp,
                        onClick = performClose,
                        includePadding = false,
                    )
                }
            }
        }
    }
}

@Composable
@Preview
private fun WhatsNewPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        WhatsNewPage(
            title = stringResource(R.string.whats_new_autoplay_title),
            message = stringResource(R.string.whats_new_autoplay_body),
            confirmButtonTitle = stringResource(R.string.whats_new_autoplay_enable_button),
            closeButtonTitle = stringResource(R.string.whats_new_autoplay_maybe_later_button),
            header = { AutoPlayHeader() },
            onConfirm = {},
            onClose = {}
        )
    }
}
