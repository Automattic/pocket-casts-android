package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.LocalPodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.compose.PodcastColorsParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.onEnter
import au.com.shiftyjelly.pocketcasts.compose.extensions.onTabMoveFocus
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/***
 * A page for to set a title for a bookmark and save it.
 */
@Composable
fun BookmarkPage(
    isNewBookmark: Boolean,
    title: TextFieldValue,
    playerColors: PlayerColors,
    onTitleChange: (TextFieldValue) -> Unit,
    onSave: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onClose,
                ) {
                    Icon(
                        painter = painterResource(IR.drawable.ic_close),
                        contentDescription = stringResource(LR.string.close),
                        tint = playerColors.contrast01,
                    )
                }
                Text(
                    text = stringResource(if (isNewBookmark) R.string.add_bookmark_title else R.string.change_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = playerColors.contrast01,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(
                    modifier = Modifier.size(48.dp),
                )
            }
        }
        Content(
            isNewBookmark = isNewBookmark,
            title = title,
            colors = playerColors,
            onTitleChange = onTitleChange,
            onSave = onSave,
        )
    }
}

@Composable
private fun Content(
    isNewBookmark: Boolean,
    title: TextFieldValue,
    colors: PlayerColors,
    onTitleChange: (TextFieldValue) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        val titleRes = if (isNewBookmark) R.string.add_bookmark_title_hint else R.string.change_bookmark_title_hint
        TextP40(
            text = stringResource(titleRes),
            color = colors.contrast02,
            textAlign = TextAlign.Center,
        )

        Spacer(
            modifier = Modifier.weight(1f),
        )

        val tintTextSelectionColors = TextSelectionColors(
            handleColor = colors.highlight01,
            backgroundColor = colors.highlight01.copy(alpha = 0.4f),
        )
        CompositionLocalProvider(LocalTextSelectionColors provides tintTextSelectionColors) {
            TextField(
                value = title,
                onValueChange = { onTitleChange(it) },
                textStyle = LocalTextStyle.current.copy(
                    // if the title is too long, reduce the font size
                    fontSize = if (title.text.length > 20) 18.sp else 26.sp,
                    fontWeight = FontWeight.Bold,
                ),
                colors = TextFieldDefaults.textFieldColors(
                    textColor = colors.contrast01,
                    backgroundColor = Color.Transparent,
                    cursorColor = colors.highlight01,
                    focusedIndicatorColor = colors.highlight01,
                    unfocusedIndicatorColor = colors.highlight01,

                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onEnter(onSave)
                    .onTabMoveFocus(),
            )
        }

        Spacer(
            modifier = Modifier.weight(1f),
        )

        val isTitleBlank = title.text.isBlank()
        val textColor = remember(colors) {
            val backgroundForContrast = colors.background01.copy(alpha = 1f)
            val highlightForContrast = colors.highlight01.copy(alpha = 1f)
            when {
                ColorUtils.calculateContrast(backgroundForContrast, highlightForContrast) > 4.5 -> colors.background01
                ColorUtils.calculateContrast(Color.White, highlightForContrast) > 4.5 -> Color.White
                else -> Color.Black
            }
        }

        RowButton(
            text = stringResource(if (isNewBookmark) R.string.save_bookmark else R.string.change_title),
            colors = ButtonDefaults.buttonColors(backgroundColor = colors.highlight01),
            enabled = !isTitleBlank,
            textColor = textColor,
            includePadding = false,
            onClick = onSave,
        )
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Preview
@Composable
private fun BookmarkPagePreview(
    @PreviewParameter(PodcastColorsParameterProvider::class) podcastColors: PodcastColors,
) {
    AppTheme(Theme.ThemeType.ROSE) {
        CompositionLocalProvider(
            LocalPodcastColors provides podcastColors,
        ) {
            val colors = MaterialTheme.theme.rememberPlayerColorsOrDefault()
            BookmarkPage(
                isNewBookmark = true,
                title = TextFieldValue(""),
                playerColors = colors,
                onTitleChange = {},
                onSave = {},
                onClose = {},
                modifier = Modifier.background(colors.background01),
            )
        }
    }
}
