package au.com.shiftyjelly.pocketcasts.search

import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent.KEYCODE_DEL
import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.KeyEvent.KEYCODE_NUMPAD_ENTER
import android.view.KeyEvent.KEYCODE_SPACE
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.localization.R as LR

internal sealed interface TvSearchKey {
    data class Character(val char: Char) : TvSearchKey
    data object Space : TvSearchKey
    data object Delete : TvSearchKey
    data object TogglePage : TvSearchKey
}

private enum class TvSearchKeyboardPage { Letters, Symbols }

private val LettersKeys: List<TvSearchKey> = buildList {
    add(TvSearchKey.TogglePage)
    add(TvSearchKey.Space)
    ('a'..'z').forEach { add(TvSearchKey.Character(it)) }
    add(TvSearchKey.Delete)
}

private val SymbolsKeys: List<TvSearchKey> = buildList {
    add(TvSearchKey.TogglePage)
    add(TvSearchKey.Space)
    ('0'..'9').forEach { add(TvSearchKey.Character(it)) }
    ".,'-_&@".forEach { add(TvSearchKey.Character(it)) }
    add(TvSearchKey.Delete)
}

private val InitialSelectedIndex = LettersKeys.indexOfFirst { it is TvSearchKey.Character }

@Stable
internal class TvSearchKeyboardState {
    private var page by mutableStateOf(TvSearchKeyboardPage.Letters)

    var selectedIndex by mutableIntStateOf(InitialSelectedIndex)
        private set

    var isFocused by mutableStateOf(false)
        private set

    private var consumedLastLeftRight = false

    val keys: List<TvSearchKey> get() = if (page == TvSearchKeyboardPage.Letters) LettersKeys else SymbolsKeys
    val isSymbolsPage: Boolean get() = page == TvSearchKeyboardPage.Symbols
    val selectedKey: TvSearchKey get() = keys[selectedIndex]

    fun onFocusChanged(focused: Boolean) {
        isFocused = focused
    }

    fun togglePage() {
        page = if (page == TvSearchKeyboardPage.Letters) TvSearchKeyboardPage.Symbols else TvSearchKeyboardPage.Letters
        if (selectedIndex > keys.lastIndex) {
            selectedIndex = keys.lastIndex
        }
    }

    fun isSelected(index: Int): Boolean = isFocused && selectedIndex == index

    fun handleDpadDirection(keyCode: Int, isKeyDown: Boolean): Boolean {
        if (keyCode != KEYCODE_DPAD_LEFT && keyCode != KEYCODE_DPAD_RIGHT) return false
        return if (isKeyDown) {
            consumedLastLeftRight = when (keyCode) {
                KEYCODE_DPAD_RIGHT -> (selectedIndex < keys.lastIndex).also { if (it) selectedIndex++ }
                KEYCODE_DPAD_LEFT -> (selectedIndex > 0).also { if (it) selectedIndex-- }
                else -> false
            }
            consumedLastLeftRight
        } else {
            consumedLastLeftRight.also { consumedLastLeftRight = false }
        }
    }
}

@Composable
internal fun rememberTvSearchKeyboardState(): TvSearchKeyboardState = remember { TvSearchKeyboardState() }

@Composable
internal fun TvSearchKeyboard(
    onCharacter: (Char) -> Unit,
    onSpace: () -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
    autoFocus: Boolean = true,
    state: TvSearchKeyboardState = rememberTvSearchKeyboardState(),
) {
    val focusRequester = remember { FocusRequester() }
    if (autoFocus) {
        LaunchedEffect(Unit) {
            withFrameNanos {}
            runCatching { focusRequester.requestFocus() }
        }
    }

    fun activate(key: TvSearchKey) {
        when (key) {
            is TvSearchKey.Character -> onCharacter(key.char)
            TvSearchKey.Space -> onSpace()
            TvSearchKey.Delete -> onDelete()
            TvSearchKey.TogglePage -> state.togglePage()
        }
    }

    val keyboardDescription = stringResource(LR.string.tv_search_keyboard)
    val selectedKeyLabel = when (val key = state.selectedKey) {
        is TvSearchKey.Character -> key.char.toString()

        TvSearchKey.Space -> stringResource(LR.string.tv_search_key_space)

        TvSearchKey.Delete -> stringResource(LR.string.delete)

        TvSearchKey.TogglePage -> stringResource(
            if (state.isSymbolsPage) LR.string.tv_search_key_letters else LR.string.tv_search_key_symbols,
        )
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .focusRequester(focusRequester)
                .onFocusChanged { state.onFocusChanged(it.isFocused) }
                .semantics {
                    contentDescription = keyboardDescription
                    stateDescription = selectedKeyLabel
                    liveRegion = LiveRegionMode.Polite
                }
                .onPreviewKeyEvent { event ->
                    val keyCode = event.key.nativeKeyCode
                    val isKeyDown = event.type == KeyEventType.KeyDown
                    when (keyCode) {
                        KEYCODE_DPAD_LEFT, KEYCODE_DPAD_RIGHT -> state.handleDpadDirection(keyCode, isKeyDown)

                        KEYCODE_DPAD_CENTER -> {
                            if (isKeyDown) activate(state.selectedKey)
                            true
                        }

                        KEYCODE_ENTER, KEYCODE_NUMPAD_ENTER -> {
                            if (isKeyDown) {
                                if (event.nativeKeyEvent.device?.keyboardType == InputDevice.KEYBOARD_TYPE_ALPHABETIC) {
                                    onSubmit()
                                } else {
                                    activate(state.selectedKey)
                                }
                            }
                            true
                        }

                        KEYCODE_DEL -> {
                            if (isKeyDown) onDelete()
                            true
                        }

                        KEYCODE_SPACE -> {
                            if (isKeyDown) onSpace()
                            true
                        }

                        else -> {
                            val nativeEvent = event.nativeKeyEvent
                            if (nativeEvent.isCtrlPressed || nativeEvent.isMetaPressed) {
                                return@onPreviewKeyEvent false
                            }
                            val unicodeChar = nativeEvent.getUnicodeChar(nativeEvent.metaState)
                            val isPrintable = unicodeChar != 0 &&
                                (unicodeChar and KeyCharacterMap.COMBINING_ACCENT) == 0 &&
                                !Character.isISOControl(unicodeChar)
                            if (isPrintable) {
                                if (isKeyDown) onCharacter(unicodeChar.toChar())
                                true
                            } else {
                                false
                            }
                        }
                    }
                }
                .focusable(),
        ) {
            state.keys.forEachIndexed { index, key ->
                TvSearchKeyCap(
                    key = key,
                    selected = state.isSelected(index),
                    isSymbolsPage = state.isSymbolsPage,
                )
            }
        }
    }
}

@Composable
private fun TvSearchKeyCap(
    key: TvSearchKey,
    selected: Boolean,
    isSymbolsPage: Boolean,
) {
    val hasPersistentBackground = key is TvSearchKey.Space || key is TvSearchKey.TogglePage
    val background = when {
        selected -> MaterialTheme.tvColors.backgroundActive
        hasPersistentBackground -> MaterialTheme.tvColors.backgroundActive20
        else -> Color.Transparent
    }
    val contentColor = if (selected) MaterialTheme.tvColors.textPrimaryActive else MaterialTheme.tvColors.textSecondary
    val shape = RoundedCornerShape(8.dp)
    val sizeModifier = when (key) {
        is TvSearchKey.Character -> Modifier.width(24.dp).height(48.dp).clip(shape).background(background)

        TvSearchKey.Delete -> Modifier.width(42.dp).height(48.dp).clip(shape).background(background)

        TvSearchKey.Space, TvSearchKey.TogglePage ->
            Modifier.clip(shape).background(background).padding(horizontal = 12.dp, vertical = 6.dp)
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .scale(if (selected) 1.25f else 1f)
            .then(sizeModifier),
    ) {
        when (key) {
            is TvSearchKey.Character -> Text(
                text = key.char.toString(),
                style = MaterialTheme.tvTypography.subtitle1,
                color = contentColor,
            )

            TvSearchKey.Space -> Text(
                text = stringResource(LR.string.tv_search_key_space),
                style = MaterialTheme.tvTypography.caption1,
                color = contentColor,
            )

            TvSearchKey.Delete -> Text(
                text = "⌫",
                style = MaterialTheme.tvTypography.title3,
                color = contentColor,
            )

            TvSearchKey.TogglePage -> Text(
                text = stringResource(
                    if (isSymbolsPage) LR.string.tv_search_key_letters else LR.string.tv_search_key_symbols,
                ),
                style = MaterialTheme.tvTypography.caption1,
                color = contentColor,
            )
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSearchKeyboardPreview() {
    TvTheme {
        var query by remember { mutableStateOf("") }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.tvColors.backgroundSunken)
                .padding(48.dp),
        ) {
            Text(
                text = query.ifEmpty { "Type with the remote or a keyboard…" },
                style = MaterialTheme.tvTypography.title2,
                color = MaterialTheme.tvColors.textPrimary,
            )
            Spacer(modifier = Modifier.height(24.dp))
            TvSearchKeyboard(
                onCharacter = { query += it },
                onSpace = { query += ' ' },
                onDelete = { query = query.dropLast(1) },
                onSubmit = {},
            )
        }
    }
}
