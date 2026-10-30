package au.com.shiftyjelly.pocketcasts.search.component

import android.graphics.Rect
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.search.R
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun SearchToolbar(
    query: String,
    isFloating: Boolean,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.theme.colors
    val context = LocalContext.current
    val plate = remember(context) {
        val themedContext = ContextThemeWrapper(context, R.style.Widget_AppCompat_SearchView_PCAccent)
        requireNotNull(AppCompatResources.getDrawable(themedContext, androidx.appcompat.R.drawable.abc_textfield_search_material))
    }
    val platePadding = with(LocalDensity.current) {
        val rect = Rect().also(plate::getPadding)
        PaddingValues(rect.left.toDp(), rect.top.toDp(), rect.right.toDp(), rect.bottom.toDp())
    }
    var isFocused by remember { mutableStateOf(false) }
    val textStyle = TextStyle(
        color = colors.secondaryText01,
        fontSize = 16.sp,
        platformStyle = PlatformTextStyle(includeFontPadding = true),
    )

    Column(
        modifier = modifier
            .background(colors.secondaryUi01)
            .clickable(interactionSource = null, indication = null, onClick = onBack),
    ) {
        Spacer(Modifier.windowInsetsTopHeight(WindowInsets.systemBars))
        Box(
            modifier = Modifier.padding(start = 16.dp, top = if (isFloating) 17.dp else 0.dp),
        ) {
            Row(
                verticalAlignment = CenterVerticallyFloor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(start = 48.dp, end = 16.dp)
                    .drawBehind {
                        plate.state = if (isFocused) intArrayOf(android.R.attr.state_focused) else intArrayOf()
                        plate.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                        plate.draw(drawContext.canvas.nativeCanvas)
                    }
                    .padding(platePadding),
            ) {
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(colors.secondaryText01),
                    keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = CenterStartFloor,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = stringResource(LR.string.search_podcasts_or_add_url),
                                    style = textStyle.copy(color = colors.secondaryText02),
                                    maxLines = 1,
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged { isFocused = it.isFocused },
                )
                if (query.isNotEmpty()) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = null,
                                indication = ripple(bounded = false),
                                role = Role.Button,
                                onClick = onClear,
                            )
                            .padding(horizontal = 8.dp),
                    ) {
                        DrawableImage(
                            drawableId = androidx.appcompat.R.drawable.abc_ic_clear_material,
                            contentDescription = stringResource(androidx.appcompat.R.string.abc_searchview_description_clear),
                            tint = colors.primaryInteractive01,
                            modifier = Modifier
                                .width(24.dp)
                                .fillMaxHeight(),
                        )
                    }
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(start = 6.dp, top = 6.dp)
                    .size(44.dp)
                    .clickable(
                        interactionSource = null,
                        indication = ripple(bounded = false),
                        role = Role.Button,
                        onClick = onBack,
                    ),
            ) {
                DrawableImage(
                    drawableId = IR.drawable.ic_arrow_back_24dp,
                    contentDescription = stringResource(LR.string.back),
                    tint = colors.secondaryIcon01,
                )
            }
        }
    }
}

private val CenterStartFloor = Alignment { size, space, _ -> IntOffset(0, (space.height - size.height) / 2) }

private val CenterVerticallyFloor = Alignment.Vertical { size, space -> (space - size) / 2 }
