package au.com.shiftyjelly.pocketcasts.player.view

import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.withTranslation
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.buttons.ViewMaterialButton
import au.com.shiftyjelly.pocketcasts.compose.components.NumberStepper
import au.com.shiftyjelly.pocketcasts.compose.images.DrawableImage
import au.com.shiftyjelly.pocketcasts.compose.images.drawableBackground
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewAlignment
import au.com.shiftyjelly.pocketcasts.compose.text.TextViewText
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.airbnb.lottie.LottieDrawable
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.model.KeyPath
import kotlin.math.floor
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.views.R as VR

internal sealed interface SleepState {
    data class Setup(
        val customTimeText: String,
        val endOfEpisodesText: String,
        val endOfChaptersText: String?,
    ) : SleepState

    data class RunningTime(
        val timeLeftText: String,
        val hasChapters: Boolean,
    ) : SleepState

    data class RunningEndOfEpisodeOrChapter(
        val sleepingInText: String,
    ) : SleepState
}

internal data class SleepColors(
    val contrast01: Color,
    val contrast05: Color,
    val highlight: Color,
)

internal interface SleepListener {
    fun onMinutesClick(mins: Int)
    fun onCustomClick()
    fun onCustomMinusClick()
    fun onCustomPlusClick()
    fun onEndOfEpisodeClick()
    fun onEndOfEpisodeMinusClick()
    fun onEndOfEpisodePlusClick()
    fun onEndOfChapterClick()
    fun onEndOfChapterMinusClick()
    fun onEndOfChapterPlusClick()
    fun onSettingsClick()
    fun onAddMinutesClick(mins: Int)
    fun onExtendEndOfEpisodeClick()
    fun onExtendEndOfChapterClick()
    fun onCancelClick()
}

@Composable
internal fun SleepPage(
    state: SleepState,
    colors: SleepColors,
    listener: SleepListener,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp)
                    .size(width = 48.dp, height = 4.dp)
                    .drawableBackground(VR.drawable.background_dragger_player)
                    .align(Alignment.TopCenter),
            )
            when (state) {
                is SleepState.Setup -> SleepSetup(state, colors, listener)
                is SleepState.RunningTime -> SleepRunningTime(state, colors, listener)
                is SleepState.RunningEndOfEpisodeOrChapter -> SleepRunningEndOfEpisodeOrChapter(state, colors, listener)
            }
        }
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun SleepSetup(state: SleepState.Setup, colors: SleepColors, listener: SleepListener) {
    Column {
        Layout(
            content = {
                TextViewText(
                    text = stringResource(LR.string.player_sleep_timer_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.contrast01,
                )
                Box(
                    modifier = Modifier
                        .clickable(interactionSource = null, indication = ripple(bounded = false), role = Role.Button, onClick = listener::onSettingsClick)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    DrawableImage(
                        drawableId = IR.drawable.sleep_timer_cog,
                        contentDescription = stringResource(LR.string.player_sleep_timer_settings),
                        tint = colors.contrast01,
                    )
                }
            },
            modifier = Modifier.padding(start = 32.dp, top = 24.dp, end = 24.dp),
        ) { measurables, constraints ->
            val loose = constraints.copy(minWidth = 0, minHeight = 0)
            val title = measurables[0].measure(loose)
            val cog = measurables[1].measure(loose.copy(maxHeight = Constraints.Infinity))
            layout(constraints.maxWidth, title.height) {
                title.place(0, 0)
                cog.place(constraints.maxWidth - cog.width, (title.height - cog.height) / 2)
            }
        }
        Spacer(Modifier.height(24.dp))
        SleepOption(LR.string.player_sleep_15_minutes, colors, onClick = { listener.onMinutesClick(15) })
        SleepSeparator(colors)
        SleepOption(LR.string.player_sleep_30_minutes, colors, onClick = { listener.onMinutesClick(30) })
        SleepSeparator(colors)
        SleepOption(LR.string.hours_singular, colors, onClick = { listener.onMinutesClick(60) })
        SleepSeparator(colors)
        SleepStepperOption(
            text = state.customTimeText,
            colors = colors,
            onClick = listener::onCustomClick,
            onMinusClick = listener::onCustomMinusClick,
            onPlusClick = listener::onCustomPlusClick,
            minusContentDescription = LR.string.player_sleep_custom_minus,
            plusContentDescription = LR.string.player_sleep_custom_plus,
        )
        SleepSeparator(colors)
        if (state.endOfChaptersText != null) {
            SleepStepperOption(
                text = state.endOfChaptersText,
                colors = colors,
                onClick = listener::onEndOfChapterClick,
                onMinusClick = listener::onEndOfChapterMinusClick,
                onPlusClick = listener::onEndOfChapterPlusClick,
                minusContentDescription = LR.string.player_sleep_chapter_minus,
                plusContentDescription = LR.string.player_sleep_chapter_plus,
            )
            SleepSeparator(colors)
        }
        SleepStepperOption(
            text = state.endOfEpisodesText,
            colors = colors,
            onClick = listener::onEndOfEpisodeClick,
            onMinusClick = listener::onEndOfEpisodeMinusClick,
            onPlusClick = listener::onEndOfEpisodePlusClick,
            minusContentDescription = LR.string.player_sleep_episode_minus,
            plusContentDescription = LR.string.player_sleep_episode_plus,
        )
    }
}

@Composable
private fun SleepOption(@StringRes textRes: Int, colors: SleepColors, onClick: () -> Unit) {
    SleepRow(text = stringResource(textRes), colors = colors, onClick = onClick)
}

@Composable
private fun SleepStepperOption(
    text: String,
    colors: SleepColors,
    onClick: () -> Unit,
    onMinusClick: () -> Unit,
    onPlusClick: () -> Unit,
    @StringRes minusContentDescription: Int,
    @StringRes plusContentDescription: Int,
) {
    SleepRow(text = text, colors = colors, onClick = onClick) {
        AppTheme(Theme.ThemeType.DARK) {
            NumberStepper(
                onMinusClick = onMinusClick,
                onPlusClick = onPlusClick,
                minusContentDescription = minusContentDescription,
                plusContentDescription = plusContentDescription,
                modifier = Modifier.padding(end = 24.dp),
            )
        }
    }
}

@Composable
private fun SleepRow(
    text: String,
    colors: SleepColors,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .clickable(onClick = onClick),
    ) {
        Box(
            contentAlignment = TextViewAlignment.CenterStart,
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 32.dp),
        ) {
            TextViewText(text = text, fontSize = 16.sp, color = colors.contrast01)
        }
        trailing()
    }
}

@Composable
private fun SleepSeparator(colors: SleepColors) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind { drawRect(colors.contrast05) },
    )
}

@Composable
private fun SleepAnimation(colors: SleepColors) {
    val composition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.sleep_button))
    val progress by animateLottieCompositionAsState(composition, iterations = LottieConstants.IterateForever)
    val context = LocalContext.current
    val animationsEnabled = remember(context) { Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f }
    val tint = colors.highlight.toArgb()
    val drawable = remember(composition, tint) {
        LottieDrawable().apply {
            composition?.let(::setComposition)
            addValueCallback(KeyPath("**"), LottieProperty.COLOR) { tint }
        }
    }
    Box(
        modifier = Modifier
            .padding(top = 32.dp)
            .size(72.dp)
            .drawBehind {
                val width = drawable.intrinsicWidth
                val height = drawable.intrinsicHeight
                if (width <= 0 || height <= 0) return@drawBehind
                drawable.progress = if (animationsEnabled) progress else composition?.let { it.getProgressForFrame(floor(it.endFrame)) } ?: 0f
                drawable.setBounds(0, 0, width, height)
                val scale = maxOf(size.width / width, size.height / height)
                val canvas = drawContext.canvas.nativeCanvas
                canvas.withTranslation((size.width - width * scale) * 0.5f, (size.height - height * scale) * 0.5f) {
                    scale(scale, scale)
                    drawable.draw(this)
                }
            },
    )
}

@Composable
private fun SleepRunningTime(state: SleepState.RunningTime, colors: SleepColors, listener: SleepListener) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SleepAnimation(colors)
        TextViewText(
            text = state.timeLeftText,
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            lineSpacingExtra = 11.sp,
            color = colors.contrast01,
            modifier = Modifier.padding(top = 8.dp),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(start = 32.dp, top = 32.dp, end = 32.dp),
        ) {
            SleepButton(stringResource(LR.string.player_sleep_add_1_minute), colors, onClick = { listener.onAddMinutesClick(1) }, modifier = Modifier.weight(1f))
            SleepButton(stringResource(LR.string.player_sleep_add_5_minutes), colors, onClick = { listener.onAddMinutesClick(5) }, modifier = Modifier.weight(1f))
        }
        if (state.hasChapters) {
            SleepWideButton(LR.string.player_sleep_timer_end_of_chapter, colors, listener::onExtendEndOfChapterClick)
        }
        SleepWideButton(LR.string.player_sleep_timer_end_of_episode, colors, listener::onExtendEndOfEpisodeClick)
        SleepWideButton(LR.string.player_sleep_cancel_timer, colors, listener::onCancelClick)
    }
}

@Composable
private fun SleepRunningEndOfEpisodeOrChapter(state: SleepState.RunningEndOfEpisodeOrChapter, colors: SleepColors, listener: SleepListener) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SleepAnimation(colors)
        TextViewText(
            text = state.sleepingInText,
            fontSize = 16.sp,
            color = colors.contrast01,
            modifier = Modifier.padding(top = 24.dp),
        )
        Spacer(Modifier.height(40.dp - 8.dp))
        SleepWideButton(LR.string.player_sleep_cancel_timer, colors, listener::onCancelClick)
    }
}

@Composable
private fun SleepWideButton(@StringRes textRes: Int, colors: SleepColors, onClick: () -> Unit) {
    SleepButton(
        text = stringResource(textRes),
        colors = colors,
        modifier = Modifier
            .padding(start = 32.dp, top = 8.dp, end = 32.dp)
            .fillMaxWidth(),
        onClick = onClick,
    )
}

@Composable
private fun SleepButton(text: String, colors: SleepColors, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ViewMaterialButton(
        text = text,
        onClick = onClick,
        backgroundColor = Color.Transparent,
        textColor = colors.highlight,
        strokeColor = colors.highlight,
        fontSize = 16.sp,
        elevation = 0.dp,
        modifier = modifier.height(56.dp),
    )
}
