package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.extensions.onEnter
import au.com.shiftyjelly.pocketcasts.compose.extensions.onTabMoveFocus
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

/***
 * A page for to set a title for a bookmark and save it.
 */
@Composable
fun BookmarkPage(isNewBookmark: Boolean, title: TextFieldValue, tintColor: Color, backgroundColor: Color, onTitleChange: (TextFieldValue) -> Unit, onSave: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            ThemedTopAppBar(
                backgroundColor = Color.Transparent,
                navigationButton = NavigationButton.Close,
                onNavigationClick = onClose,
                iconColor = tintColor,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = stringResource(if (isNewBookmark) R.string.add_bookmark_title else R.string.change_title),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Content(
            isNewBookmark = isNewBookmark,
            title = title,
            tintColor = tintColor,
            backgroundColor = backgroundColor,
            onTitleChange = onTitleChange,
            onSave = onSave
        )
    }
}

@Composable
private fun Content(isNewBookmark: Boolean, title: TextFieldValue, tintColor: Color, backgroundColor: Color, onTitleChange: (TextFieldValue) -> Unit, onSave: () -> Unit, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val buttonColor = if (tintColor == Color.White) MaterialTheme.colors.primary else tintColor
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        val titleRes = if (isNewBookmark) R.string.add_bookmark_title_hint else R.string.change_bookmark_title_hint
        TextP40(
            text = stringResource(titleRes),
            color = Color.White.copy(alpha = 0.5f),
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(100.dp, 240.dp)
        )

        Spacer(Modifier.weight(1f))

        val tintTextSelectionColors = TextSelectionColors(
            handleColor = buttonColor,
            backgroundColor = buttonColor.copy(alpha = 0.4f)
        )
        CompositionLocalProvider(LocalTextSelectionColors provides tintTextSelectionColors) {
            TextField(
                value = title,
                onValueChange = { onTitleChange(it) },
                textStyle = LocalTextStyle.current.copy(
                    // if the title is too long, reduce the font size
                    fontSize = if (title.text.length > 20) 18.sp else 26.sp,
                    fontWeight = FontWeight.Bold
                ),
                colors = TextFieldDefaults.textFieldColors(
                    textColor = Color.White,
                    backgroundColor = Color.Transparent,
                    cursorColor = buttonColor,
                    focusedIndicatorColor = Color(0x33FFFFFF),
                    unfocusedIndicatorColor = Color(0x33FFFFFF),

                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onEnter(onSave)
                    .onTabMoveFocus()
            )
        }

        Spacer(Modifier.weight(1f))
        val isTitleBlank = title.text.isBlank()
        RowButton(
            text = stringResource(if (isNewBookmark) R.string.save_bookmark else R.string.change_title),
            colors = ButtonDefaults.buttonColors(backgroundColor = buttonColor),
            enabled = !isTitleBlank,
            // if the tint color is too light use the background color for the text
            textColor = if (buttonColor.luminance() > 0.5) backgroundColor else Color.White,
            includePadding = false,
            onClick = onSave
        )
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Preview
@Composable
private fun BookmarkPagePreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
        BookmarkPage(
            isNewBookmark = true,
            title = TextFieldValue(""),
            tintColor = Color.White,
            backgroundColor = Color.Black,
            onTitleChange = {},
            onSave = {},
            onClose = {}
        )
    }
}
