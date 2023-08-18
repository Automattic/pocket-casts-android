package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun MessageView(
    titleView: @Composable () -> Unit,
    @StringRes buttonTitleRes: Int,
    buttonAction: () -> Unit,
    style: MessageViewColors,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(16.dp)
                .background(
                    color = style.backgroundColor(),
                    shape = RoundedCornerShape(size = 4.dp)
                )
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 16.dp)
            ) {
                titleView()
                TextP40(
                    text = stringResource(LR.string.bookmarks_create_instructions),
                    color = style.textColor(),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
                TextButton(
                    buttonTitleRes = buttonTitleRes,
                    buttonAction = buttonAction,
                    modifier = Modifier.padding(top = 4.dp),
                    style = style
                )
            }
        }
    }
}

@Composable
private fun TextButton(
    @StringRes buttonTitleRes: Int,
    buttonAction: () -> Unit = {},
    modifier: Modifier,
    style: MessageViewColors
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable { buttonAction() }
    ) {
        TextH40(
            text = stringResource(buttonTitleRes),
            color = style.buttonTextColor(),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(8.dp)
        )
    }
}

sealed class MessageViewColors {
    @Composable
    abstract fun backgroundColor(): Color
    @Composable
    abstract fun textColor(): Color
    @Composable
    abstract fun buttonTextColor(): Color

    object Default : MessageViewColors() {
        @Composable
        override fun backgroundColor(): Color = MaterialTheme.theme.colors.primaryUi01Active

        @Composable
        override fun textColor(): Color = MaterialTheme.theme.colors.primaryText02

        @Composable
        override fun buttonTextColor(): Color = MaterialTheme.theme.colors.primaryInteractive01
    }

    object Player : MessageViewColors() {
        @Composable
        override fun backgroundColor(): Color = MaterialTheme.theme.colors.playerContrast06

        @Composable
        override fun textColor(): Color = MaterialTheme.theme.colors.playerContrast02

        @Composable
        override fun buttonTextColor(): Color = MaterialTheme.theme.colors.playerContrast01
    }
}
