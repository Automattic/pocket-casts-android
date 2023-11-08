package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.extensions.nonScaledSp
import au.com.shiftyjelly.pocketcasts.endofyear.components.DayCirclesView
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryBlurredBackground
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryFontFamily
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.disableScale
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListeningTime
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val ListenedTimeFontSize = 100

@Composable
fun StoryListeningTimeView(
    story: StoryListeningTime,
    modifier: Modifier = Modifier,
) {
    Box {
        StoryBlurredBackground(
            Offset(
                -LocalView.current.width * 0.7f,
                LocalView.current.height * 0.3f
            ),
        )
        Column(
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 30.dp)
        ) {
            Spacer(modifier = modifier.height(40.dp))

            PrimaryText(story, modifier)

            Spacer(modifier = modifier.height(14.dp))

            SecondaryText(story, modifier)

            Spacer(modifier = modifier.weight(0.25f))

            ListenedTimeTexts(story)

            Spacer(modifier = Modifier.weight(0.16f))

            DayCirclesView(story.listeningTimeInSecs)

            Spacer(modifier = modifier.weight(1f))
        }
    }
}

@Composable
private fun PrimaryText(
    story: StoryListeningTime,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val language = Locale.current.language
    val timeText = StatsHelper.secondsToFriendlyString(story.listeningTimeInSecs, context.resources)
    val textResId = if (language == "en") {
        LR.string.end_of_year_listening_time_title_english_only
    } else {
        LR.string.end_of_year_listening_time_title
    }
    val text = stringResource(textResId, timeText)
    StoryPrimaryText(text = text, color = story.tintColor, modifier = modifier)
}

@Composable
private fun SecondaryText(
    story: StoryListeningTime,
    modifier: Modifier,
) {
    val text = stringResource(LR.string.end_of_year_listening_time_subtitle)
    StorySecondaryText(text = text, color = story.subtitleColor, modifier = modifier)
}

@Composable
private fun ListenedTimeTexts(story: StoryListeningTime) {
    val context = LocalContext.current
    val listeningTimeDisplayStrings = getListeningTimeDisplayStrings(context, story.listeningTimeInSecs)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = listeningTimeDisplayStrings.firstNumber,
            color = story.tintColor,
            fontSize = ListenedTimeFontSize.nonScaledSp,
            lineHeight = ListenedTimeFontSize.nonScaledSp,
            fontWeight = FontWeight.W300,
            fontFamily = StoryFontFamily,
            style = LocalTextStyle.current.merge(
                @Suppress("DEPRECATION")
                TextStyle(
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false,
                    ),
                    lineHeightStyle = LineHeightStyle(
                        alignment = LineHeightStyle.Alignment.Top,
                        trim = LineHeightStyle.Trim.Both
                    )
                )
            ),
            modifier = Modifier.paddingFromBaseline(top = 0.sp, bottom = 0.sp),
        )
        TextH50(
            text = listeningTimeDisplayStrings.subtitle,
            textAlign = TextAlign.Center,
            color = story.subtitleColor,
            fontFamily = StoryFontFamily,
            fontWeight = FontWeight.W600,
            disableScale = disableScale(),
        )
    }
}

private fun getListeningTimeDisplayStrings(
    context: Context,
    listeningTimeInSecs: Long,
): ListeningTimeDisplayStrings {
    val timeText = StatsHelper.secondsToFriendlyString(listeningTimeInSecs, context.resources)
    val timeTextStrings = timeText.split(" ")
    val firstNumber = timeTextStrings.firstOrNull() ?: ""
    val subtitle = if (timeTextStrings.size > 1) {
        timeTextStrings.drop(1).joinToString(" ")
    } else {
        ""
    }
    return ListeningTimeDisplayStrings(firstNumber, subtitle)
}

data class ListeningTimeDisplayStrings(
    val firstNumber: String,
    val subtitle: String,
)
