package au.com.shiftyjelly.pocketcasts.compose.bottomsheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

private val outlinedBorder: BorderStroke
    @Composable
    get() = BorderStroke(2.dp, MaterialTheme.colors.primary)

class BottomSheetContentState(
    val content: Content,
) {
    data class Content(
        val titleText: String,
        val summaryText: String,
        val primaryButton: Button.Primary,
        val secondaryButton: Button.Secondary? = null,
    ) {
        sealed interface Button {
            val label: String
            val onClick: (() -> Unit)?

            data class Primary(
                override val label: String,
                override val onClick: () -> Unit,
            ) : Button

            data class Secondary(
                override val label: String,
                override val onClick: (() -> Unit)? = null,
            ) : Button
        }
    }
}

@Composable
fun BottomSheetContent(
    state: BottomSheetContentState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentWidth(unbounded = false)
            .wrapContentHeight(unbounded = true)
            .padding(24.dp)
    ) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val content = state.content

            Spacer(modifier = modifier.height(16.dp))

            TextH40(
                text = content.titleText,
                color = MaterialTheme.theme.colors.primaryText01,
                maxLines = 1
            )

            Spacer(modifier = modifier.height(16.dp))

            TextP50(
                text = content.summaryText,
                style = MaterialTheme.typography.body1,
                color = MaterialTheme.theme.colors.primaryText02,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = modifier.height(16.dp))

            Button(onClick = content.primaryButton.onClick) {
                Text(text = content.primaryButton.label)
            }

            Spacer(modifier = modifier.height(8.dp))

            if (content.secondaryButton != null) {
                OutlinedButton(
                    border = outlinedBorder,
                    onClick = {
                        onDismiss.invoke()
                        content.secondaryButton.onClick?.invoke()
                    }
                ) {
                    Text(text = content.secondaryButton.label)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BottomSheetContentPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        BottomSheetContent(
            state = BottomSheetContentState(
                content = BottomSheetContentState.Content(
                    titleText = "Heading",
                    summaryText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt.",
                    primaryButton = BottomSheetContentState.Content.Button.Primary(
                        label = "Confirm",
                        onClick = {}
                    ),
                    secondaryButton = BottomSheetContentState.Content.Button.Secondary(
                        label = "Not now",
                    ),
                )
            ),
            onDismiss = {}
        )
    }
}
